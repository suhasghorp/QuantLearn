package com.quantlearn.curves;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.collect.ImmutableList;
import com.quantlearn.caching.RefDateUtils;
import com.quantlearn.enums.CurveInstrumentType;
import com.quantlearn.interpolation.BaseInterpolator;
import com.quantlearn.interpolation.InterpolateOnWhat;
import com.quantlearn.schedule.BusDate;
import com.quantlearn.utils.FormulaUtil;

import net.finmath.optimizer.LevenbergMarquardt;

public class MultiCurveGlobalFit implements Curve {
	private BusDate refDate;
	BaseInterpolator interp = null;
	SingleCurveGlobalFit discountingCurve;
	
	@SuppressWarnings("unchecked")
	public MultiCurveGlobalFit(ImmutableList<CurveInstrument> instruments, SingleCurveGlobalFit discountingCurve, BaseInterpolator interp) throws Exception {
		this.discountingCurve = discountingCurve;
		this.interp = interp;
		
		refDate = RefDateUtils.getRefDate();
		Map<String,Double> liborFixings = new HashMap<>();
		liborFixings.put("3M", 0.0068010);
		liborFixings.put("6M", 0.0098010);
		
		List<CurveInstrument> edfswaps = instruments.stream()
				.filter(i -> i.getCurveInstrumentType() == CurveInstrumentType.SWAP
				|| i.getCurveInstrumentType() == CurveInstrumentType.EDF)
				.map(instrument -> ((CurveInstrument)instrument))
				.collect(Collectors.toList());
		
		List<CurveInstrument> edf = instruments.stream()
				.filter(i -> i.getCurveInstrumentType() == CurveInstrumentType.EDF)
				.map(instrument -> ((CurveInstrument)instrument))
				.collect(Collectors.toList());
		
		List<CurveInstrument> swaps = instruments.stream()
				.filter(i -> i.getCurveInstrumentType() == CurveInstrumentType.SWAP)
				.map(instrument -> ((CurveInstrument)instrument))
				.collect(Collectors.toList());
		
		String underlyingRateTenor = null;
		final double fixing ;
		if (((VanillaSwap)swaps.get(0)).getFloatingLeg().getUnderlyingRateTenor().equals("3M")) {
			underlyingRateTenor = "3M";
			fixing = liborFixings.get("3M");
		} else if (((VanillaSwap)swaps.get(0)).getFloatingLeg().getUnderlyingRateTenor().equals("6M")) {
			underlyingRateTenor = "6M";
			fixing = liborFixings.get("6M");
		} else throw new Exception("Underlying tenor of the market swap not understood.");
		
		List<Double> edfDates = new ArrayList<>();
		for (CurveInstrument c : edf) {
			EuroDollarFuture e = (EuroDollarFuture)c;
			edfDates.add(e.IMMDate.getExcelSerial());
		}
		edfDates.add(0, refDate.getExcelSerial());
		List<Double> swapDates = swaps.stream().map(e -> e.getLastFromDate())
												.collect(Collectors.toList());
		double[] edfDatesArray = ArrayUtils.toPrimitive(edfDates.toArray(new Double[edfDates.size()]));
		double[] swapDatesArray = ArrayUtils.toPrimitive(swapDates.toArray(new Double[swapDates.size()]));
		
		double[] fromDatesArray = ArrayUtils.addAll(edfDatesArray, swapDatesArray);
		
		List<Double> edfRates = edf.stream().map(e -> ((EuroDollarFuture)e).rateValue).collect(Collectors.toList());
		double[] edfInitialParameters = ArrayUtils.toPrimitive(edfRates.toArray(new Double[edfRates.size()]));
		double[] swapInitialParameters = DoubleStream.generate(() -> 0.0109).limit(swaps.size()+1).toArray();
		double[] initialParameters = ArrayUtils.addAll(edfInitialParameters, swapInitialParameters);
		
		LevenbergMarquardt optimizer = new LevenbergMarquardt() {
			@Override
			public void setValues(double[] x, double[] fi) {
				interp.reinitialize(fromDatesArray, x);
				fi[fi.length-1] = (fixing - x[0]) * 10000;
				for (int i = 0; i < edfswaps.size(); i++){
					if (edfswaps.get(i) instanceof EuroDollarFuture) {
						fi[i] = 0;
					} else {
						fi[i] = (calcParRate(edfswaps.get(i)) - edfswaps.get(i).getRateValue()) * 10000;
					}
		        } 				
				System.out.println("here");
			}
		};
		//optimizer.setInitialParameters(DoubleStream.generate(() -> fixing).limit(edfswaps.size()+1).toArray());
		optimizer.setInitialParameters(initialParameters);
		double[] weights = new double[edfswaps.size()+1];
		double[] targets = new double[edfswaps.size()+1];
		Arrays.fill(weights, 1.0);
		Arrays.fill(targets, 0.0);
		optimizer.setWeights(weights);
		optimizer.setMaxIteration(100000);
		optimizer.setTargetValues(targets);
		
		optimizer.run();
		double[] bestParameters = optimizer.getBestFitParameters();
		System.out.println("The solver for problem 1 required " + optimizer.getIterations() + " iterations. Accuracy is " + optimizer.getRootMeanSquaredError() + ". The best fit parameters are:");
		for (int i = 0; i < bestParameters.length; i++) 
			System.out.println("\tparameter[" + i + "]: " + bestParameters[i]);	
		
	}
	
	@Override
	public double getDiscFactorForDate(BusDate d) {
		double excelSerial = d.getExcelSerial();
		double interpDF = discountingCurve.getDiscFactorForSerial(excelSerial);
        return interpDF;
	}
	
	public double getFwdRate(BusDate d) { //assume a 3M forward, if 6M, need to build a new multi curve
		double excelSerial = d.getExcelSerial();
		double interpFwd = interp.solve(excelSerial);
        return interpFwd;
	}
	
	public double calcParRate(CurveInstrument instr) {
		
		double output = 0;
		try {
			if (instr instanceof VanillaSwap) {
				VanillaSwap swap = (VanillaSwap)instr;
				
				//Fixed Leg
				double[] yfFixedLeg = swap.getFixedSchedule().getYFArray(swap.getFixedLeg().getDayCount()); 
				double[] dfFixedDates = swap.getFixedSchedule().getPayDatesArray();
				double[] dfFixLeg = new double[yfFixedLeg.length];
				for (int i = 0; i < yfFixedLeg.length; i++) {
		           dfFixLeg[i] = discountingCurve.getDiscFactorForSerial(dfFixedDates[i]);
		        }
				
				//Floating Leg
				double[] yfFloatingLeg = swap.getFloatingSchedule().getYFArray(swap.getFloatingLeg().getDayCount()); 
				double[] dfFloatingPayDates = swap.getFloatingSchedule().getPayDatesArray();
				double[] dfFloatingLeg = new double[yfFloatingLeg.length];
				for (int i = 0; i < yfFloatingLeg.length; i++) {
		           dfFloatingLeg[i] = discountingCurve.getDiscFactorForSerial(dfFloatingPayDates[i]);
		        }
				double[] dfFloatingFromDates = swap.getFloatingSchedule().getFromDatesArray();
				double[] fwdFloatingLeg = new double[dfFloatingFromDates.length];
				for (int i = 0; i < dfFloatingFromDates.length; i++) {
					fwdFloatingLeg[i] = interp.solve(dfFloatingFromDates[i]);
				}			
		        output = FormulaUtil.multiCurveParRate(yfFloatingLeg, dfFloatingLeg, fwdFloatingLeg, yfFixedLeg, dfFixLeg); 
			} else if (instr instanceof EuroDollarFuture) {
				EuroDollarFuture edf = (EuroDollarFuture)instr;
				return edf.rateValue;
				//output = edf.getDiscountFactor();
				//double beginDF = interp.solve(edf.IMMDate.getExcelSerial());
				//double endDF = interp.solve(edf.endDate.getExcelSerial());
				//output = endDF/beginDF;
				
				//double term = (int)edf.IMMDate.D_EF(edf.endDate);
				//double beginDF = 1 / (1 + (interp.solve(edf.IMMDate.getExcelSerial())) * term / 360);
				//double endDF = 1 / (1 + (interp.solve(edf.endDate.getExcelSerial())) * term / 360);
				//output = endDF/beginDF;
			}
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		return output;
	}
}