package com.quantlearn.creditcurve;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.AllowedSolution;
import org.apache.commons.math3.analysis.solvers.BracketingNthOrderBrentSolver;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;
import org.apache.commons.math3.optim.univariate.UnivariateOptimizer;
import org.apache.commons.math3.optim.univariate.UnivariatePointValuePair;

import com.quantlearn.caching.RefDateUtils;
import com.quantlearn.enums.BusinessDayAdjustment;
import com.quantlearn.ircurves.IRCurve;
import com.quantlearn.ircurves.SingleCurveISDA;
import com.quantlearn.schedule.BusDate;

public class SingleNameBootStrapCurve {
	List<Double> tenors; 
	List<Double> spreads;
	List<Double> hazRates = new ArrayList<>();
	List<Double> survProbs = new ArrayList<>();
	int numPremiumPerYear;
	int numDefaultPerYear; 
	boolean accruedPremium; 
	double recoveryRate; 
	IRCurve isdaCurve;
	int currentCreditCurveLength;
	public SingleNameBootStrapCurve(List<Double> tenors, List<Double> spreads,  int numPremiumPerYear,
			int numDefaultPerYear, boolean accruedPremium, double recoveryRate, IRCurve isdaCurve) {
		this.tenors = tenors;
		this.spreads = spreads;
		this.numPremiumPerYear = numPremiumPerYear;
		this.numDefaultPerYear = numDefaultPerYear;
		this.accruedPremium = accruedPremium;
		this.recoveryRate = recoveryRate;
		this.isdaCurve = isdaCurve;
		this.currentCreditCurveLength = hazRates.size();
	}
	
	public List<Double> bootstrapCDSSpreads(){
		List<Double> retData = new ArrayList<>();
		for (int i = 0; i < tenors.size(); i++) {
			List<Double> ntenors = this.tenors.subList(0, i);
			List<Double> ncreditCurveSP = this.survProbs.subList(0, i);
			UnivariateFunction f = new objFunctionHazardRate(ntenors, ncreditCurveSP, this.isdaCurve,  
					this.tenors.get(i), this.numPremiumPerYear, this.accruedPremium, this.spreads.get(i), 
					this.numDefaultPerYear, this.recoveryRate);
		    
			BracketingNthOrderBrentSolver solver = new BracketingNthOrderBrentSolver();
			double h = solver.solve(200, f, 0, 30, AllowedSolution.LEFT_SIDE);
            
		    double survProb = 0;
		    hazRates.add(i, h);
		    this.currentCreditCurveLength = hazRates.size();
			if (i == 0) {
				survProb = Math.exp(-h * this.tenors.get(i));
			} else {
				survProb = survProbs.get(i - 1) * Math.exp(-h * (this.tenors.get(i) - this.tenors.get(i-1)));
			}
			survProbs.add(i, survProb);	
			System.out.println("=======================");
		}
		hazRates.addAll(survProbs);
		return hazRates;
	}
	
	double calculatePremiumLeg(List<Double> creditcurveTenor, List<Double> creditcurveSP, IRCurve isdaCurve,  
			double cdsMaturity, int numberPremiumPerYear, boolean accruedPremiumFlag, double spread, double h) {
			
			int max_time_index = currentCreditCurveLength - 1;

			// if creditcurve not empty (i.e ncreditcurve > 0) and 
			// if cdsMaturity <= creditcurveTenor[max_time_index]
			// i.e. if hazard rate already know for cdsMaturity 
			// ==> simple calculation, no bootstrap required
			if (currentCreditCurveLength > 0 && cdsMaturity <= creditcurveTenor.get(max_time_index)) {
				//Rprintf("calculatePremiumLeg ==> no boostrap,ncreditcurve=: %d\n",ncreditcurve);
				double annuity = 0;
				double accruedPremium = 0;
				int N = (int)(cdsMaturity * (double)numberPremiumPerYear);

				int n;
				for (n = 1; n <= N; n++) {
					double tn = n / numberPremiumPerYear;
					double tnm1 = (n - 1) / numberPremiumPerYear;
					double delta_t = 1.0 / numberPremiumPerYear;
					BusDate calcDate = RefDateUtils.getRefDate().shiftPeriod(tn+"Y", BusinessDayAdjustment.Unadjusted, "ADD");
					annuity += delta_t * isdaCurve.getDiscFactorForDate(calcDate) * getSurvivalProbability(creditcurveTenor, creditcurveSP, currentCreditCurveLength, tn);

					if (accruedPremiumFlag) {
						accruedPremium += 0.5 * delta_t * isdaCurve.getDiscFactorForDate(calcDate) *
							(getSurvivalProbability(creditcurveTenor, creditcurveSP, currentCreditCurveLength, tnm1) - getSurvivalProbability(creditcurveTenor, creditcurveSP, currentCreditCurveLength, tn));
					}
				}
				return(spread * (annuity + accruedPremium));
			}
			// if cdsMaturity > creditcurveTenor[max_time_index]
			// i.e. hazard rate not know for cdsMaturity 
			// ==> we will use the CDS spread to imply the non-cumulative hazard rate (h) (bootstrapping method)
			else {
				
				double annuity = 0;
				double accruedPremium = 0;
				int N = (int)(cdsMaturity * (double)numberPremiumPerYear);
				int M = (currentCreditCurveLength > 0 ? (int)(creditcurveTenor.get(max_time_index) * numberPremiumPerYear) : 0);

				for (int n = 1; n <= N; n++) {
					if (n <= M) {
						double tn = n / numberPremiumPerYear;
						double tnm1 = (n - 1) / numberPremiumPerYear;
						double delta_t = 1.0 / numberPremiumPerYear;
						BusDate calcDate = RefDateUtils.getRefDate().shiftPeriod(tn+"Y", BusinessDayAdjustment.Unadjusted, "ADD");
						annuity += delta_t * isdaCurve.getDiscFactorForDate(calcDate) * getSurvivalProbability(creditcurveTenor, creditcurveSP, currentCreditCurveLength, tn);

						if (accruedPremiumFlag) {
							accruedPremium += 0.5 * delta_t * isdaCurve.getDiscFactorForDate(calcDate) *
								(getSurvivalProbability(creditcurveTenor, creditcurveSP, currentCreditCurveLength, tnm1) - getSurvivalProbability(creditcurveTenor, creditcurveSP, currentCreditCurveLength, tn));
						}
					}
					else {
						double tn = n / numberPremiumPerYear;
						double tnm1 = (n - 1) / numberPremiumPerYear;
						double tM = M / numberPremiumPerYear;
						double delta_t = 1.0 / numberPremiumPerYear;

						double survivalProbabilityn = getSurvivalProbability(creditcurveTenor, creditcurveSP, currentCreditCurveLength, tM) * Math.exp(-h*(tn - tM));
						double survivalProbabilitynm1;
						if (tnm1 <= tM) survivalProbabilitynm1 = getSurvivalProbability(creditcurveTenor, creditcurveSP, currentCreditCurveLength, tnm1);
						else survivalProbabilitynm1 = getSurvivalProbability(creditcurveTenor, creditcurveSP, currentCreditCurveLength, tM) * Math.exp(-h*(tnm1 - tM));
						BusDate calcDate = RefDateUtils.getRefDate().shiftPeriod(tn+"Y", BusinessDayAdjustment.Unadjusted, "ADD");
						annuity += delta_t * isdaCurve.getDiscFactorForDate(calcDate) * survivalProbabilityn;

						if (accruedPremiumFlag) {
							accruedPremium += 0.5 * delta_t * isdaCurve.getDiscFactorForDate(calcDate) *
								(survivalProbabilitynm1 - survivalProbabilityn);
						}
					}
				}

				System.out.println("calculatePremiumLeg h=" + h + ", leg = " + (spread * (annuity + accruedPremium)));
				return(spread * (annuity + accruedPremium));
			}
		}
	
	double calculateDefaultLeg(List<Double> creditcurveTenor, List<Double> creditcurveSP, IRCurve isdaCurve, 
			double cdsMaturity, int numberDefaultIntervalPerYear, double recoveryRate, double h) {
			
			int max_time_index = currentCreditCurveLength - 1;

			
			// if creditcurve not empty (i.e ncreditcurve > 0) and 
			// if cdsMaturity <= creditcurveTenor[max_time_index]
			// i.e. if hazard rate already know for cdsMaturity 
			// ==> simple calculation, no bootstrap required
			if (currentCreditCurveLength > 0 && cdsMaturity <= creditcurveTenor.get(max_time_index)) {
				//Rprintf("calculateDefaultLeg (%f) ==> no boostrap\n",cdsMaturity,ncreditcurve);
				double annuity = 0;
				int N = (int)(cdsMaturity * (double)numberDefaultIntervalPerYear);

				for (int n = 1; n <= N; n++) {
					double tn = n / numberDefaultIntervalPerYear;
					double tnm1 = (n - 1) / numberDefaultIntervalPerYear;
					BusDate calcDate = RefDateUtils.getRefDate().shiftPeriod(tn+"Y", BusinessDayAdjustment.Unadjusted, "ADD");
					annuity += isdaCurve.getDiscFactorForDate(calcDate)*(getSurvivalProbability(creditcurveTenor, creditcurveSP, currentCreditCurveLength, tnm1) - getSurvivalProbability(creditcurveTenor, creditcurveSP, currentCreditCurveLength, tn));
				}

				return((1.0 - recoveryRate)*annuity);
			}
			// if cdsMaturity > creditcurveTenor[max_time_index]
			// i.e. hazard rate not know for cdsMaturity 
			// ==> we will use the CDS spread to imply the non-cumulative hazard rate (h) (bootstrapping method)
			else {
				//Rprintf("calculateDefaultLeg (%f/%f) ==> boostrap\n",cdsMaturity,creditcurveTenor[max_time_index]);
				double annuity = 0;
				int N = (int)(cdsMaturity * (double)numberDefaultIntervalPerYear);
				int M = (currentCreditCurveLength > 0 ? (int)(creditcurveTenor.get(max_time_index) * (double)numberDefaultIntervalPerYear) : 0);

				for (int n = 1; n <= N; n++) {
					if (n <= M) {
						double tn = n / numberDefaultIntervalPerYear;
						double tnm1 = (n - 1) / numberDefaultIntervalPerYear;
						BusDate calcDate = RefDateUtils.getRefDate().shiftPeriod(tn+"Y", BusinessDayAdjustment.Unadjusted, "ADD");
						annuity += isdaCurve.getDiscFactorForDate(calcDate) * (getSurvivalProbability(creditcurveTenor, creditcurveSP, currentCreditCurveLength, tnm1) - getSurvivalProbability(creditcurveTenor, creditcurveSP, currentCreditCurveLength, tn));

						//Rprintf("SP[%d]={%f}, SP[%d]={%f}\n",n,getSurvivalProbability(creditcurve,ncreditcurve,tn),n-1,getSurvivalProbability(creditcurve,ncreditcurve,tnm1));	
						//Rprintf("n=%d,M=%d,N=%d ==> annuity = %f\n",n,M,N,annuity);
					}
					else {
						double tM = M / numberDefaultIntervalPerYear;
						double tn = n / numberDefaultIntervalPerYear;
						double tnm1 = (n - 1) / numberDefaultIntervalPerYear;

						double survivalProbabilityn = getSurvivalProbability(creditcurveTenor, creditcurveSP, currentCreditCurveLength, tM) * Math.exp(-h*(tn - tM));
						double survivalProbabilitynm1;
						if (tnm1 <= tM) survivalProbabilitynm1 = getSurvivalProbability(creditcurveTenor, creditcurveSP, currentCreditCurveLength, tnm1);
						else survivalProbabilitynm1 = getSurvivalProbability(creditcurveTenor, creditcurveSP, currentCreditCurveLength, tM) * Math.exp(-h*(tnm1 - tM));
						BusDate calcDate = RefDateUtils.getRefDate().shiftPeriod(tn+"Y", BusinessDayAdjustment.Unadjusted, "ADD");
						annuity += isdaCurve.getDiscFactorForDate(calcDate) * (survivalProbabilitynm1 - survivalProbabilityn);

						//Rprintf("SP[%d]={%f}, SP[%d]={%f}\n",n,survivalProbabilityn,n-1,survivalProbabilitynm1);	
						//Rprintf("n=%d,M=%d,N=%d ==> annuity = %f\n",n,M,N,annuity);				
					}
				}
				System.out.println("calculateDefaultLeg h=" + h + ", leg = " + ((1.0 - recoveryRate)*annuity));
				return((1.0 - recoveryRate)*annuity);
			}
		}
	
	double getSurvivalProbability(List<Double> creditcurveTenor, List<Double> creditcurveSP, 
			int ncreditcurve, double t) {
		
		double result = -1.0;
		
		int min_time_index = 0;
		int max_time_index = currentCreditCurveLength - 1;
		int i;

		if (t < 0) {
			result = -1.0; // undefined case
		}
		else if (t == 0) {
			result = 1.0;
		}
		else if (t > 0 && t < creditcurveTenor.get(min_time_index)) {
			double h = (-1 / creditcurveTenor.get(min_time_index)) * Math.log(creditcurveSP.get(min_time_index));
			result = Math.exp(-t * h);
		}
		else if (t == creditcurveTenor.get(max_time_index)) {
			result = creditcurveSP.get(max_time_index);
		}
		else if (t > creditcurveTenor.get(max_time_index)) {
			double h = 0;
			if (max_time_index == 0) {
				h = -1 / (creditcurveTenor.get(max_time_index) - 0) * Math.log(creditcurveSP.get(max_time_index) / 1.0);
			}
			else {
				h = -1 / (creditcurveTenor.get(max_time_index) - creditcurveTenor.get(max_time_index - 1)) * Math.log(creditcurveSP.get(max_time_index) / creditcurveSP.get(max_time_index - 1));
			}

			result = creditcurveSP.get(max_time_index) * Math.exp(-(t - creditcurveTenor.get(max_time_index)) * h);
		}
		else {
			for (i = 0; i < currentCreditCurveLength - 1; i++) {
				if (t >= creditcurveTenor.get(i) && t < creditcurveTenor.get(i + 1)) {
					double h = -1 / (creditcurveTenor.get(i + 1) - creditcurveTenor.get(i)) * Math.log(creditcurveSP.get(i+1) / creditcurveSP.get(i));

					result = creditcurveSP.get(i) * Math.exp(-(t - creditcurveTenor.get(i)) * h);
				}
			}
		}

		return(result);
	}
	
	class objFunctionHazardRate implements UnivariateFunction {
		List<Double> creditcurveTenor; 
		List<Double> creditcurveSP; 
		IRCurve isdaCurve;
		double cdsMaturity;
		int numberPremiumPerYear; 
		boolean accruedPremiumFlag;
		double spread;
		int numberDefaultIntervalPerYear; 
		double recoveryRate;
		
		public objFunctionHazardRate(List<Double> creditcurveTenor, List<Double> creditcurveSP, IRCurve isdaCurve,  
				double cdsMaturity, int numberPremiumPerYear, boolean accruedPremiumFlag, double spread, int numberDefaultIntervalPerYear, double recoveryRate) {
			this.creditcurveTenor = creditcurveTenor;
			this.creditcurveSP = creditcurveSP;
			this.isdaCurve = isdaCurve;
			this.cdsMaturity = cdsMaturity;
			this.numberPremiumPerYear = numPremiumPerYear;
			this.accruedPremiumFlag = accruedPremiumFlag;
			this.spread = spread;
			this.numberDefaultIntervalPerYear = numDefaultPerYear;
			this.recoveryRate = recoveryRate;
		}
		

		@Override
		public double value(double h) {
			double result = calculatePremiumLeg(creditcurveTenor, creditcurveSP, isdaCurve,  
			cdsMaturity, numberPremiumPerYear, accruedPremiumFlag, spread, h) -
					calculateDefaultLeg(creditcurveTenor, creditcurveSP, isdaCurve, 
							cdsMaturity, numberDefaultIntervalPerYear, recoveryRate, h);
			return result;
		}
		
	}
	
	private static class LocalException extends RuntimeException {
	    private final double x;
	     public LocalException(double x) {
	         this.x = x;
	     }
	     public double getX() {
	         return x;
	     }
	 }
}
