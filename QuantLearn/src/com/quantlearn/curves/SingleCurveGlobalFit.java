package com.quantlearn.curves;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.collect.ImmutableList;
import com.quantlearn.caching.RefDateUtils;
import com.quantlearn.enums.CurveInstrumentType;
import com.quantlearn.interpolation.BaseInterpolator;
import com.quantlearn.interpolation.InterpolateOnWhat;
import com.quantlearn.schedule.BusDate;
import com.quantlearn.utils.FormulaUtil;

import net.finmath.optimizer.LevenbergMarquardt;

public class SingleCurveGlobalFit implements Curve {
	private BusDate refDate;
	TreeMap<Double,Double> allDateDFMap = new TreeMap<Double,Double>();
	TreeMap<Double,Double> edfswapDateDFMap = new TreeMap<Double,Double>();
		
	InterpolateOnWhat w = null;
	BaseInterpolator interp = null;
	
	@SuppressWarnings("unchecked")
	public SingleCurveGlobalFit(BusDate today, ImmutableList<CurveInstrument> instruments, InterpolateOnWhat w, BaseInterpolator interp) throws Exception {
		this.w = w;
		this.interp = interp;
		refDate = RefDateUtils.getRefDate();
		List<CurveInstrument> edfswaps = instruments.stream()
				.filter(i -> i.getCurveInstrumentType() == CurveInstrumentType.SWAP
				|| i.getCurveInstrumentType() == CurveInstrumentType.EDF)
				.map(instrument -> ((CurveInstrument)instrument))
				.collect(Collectors.toList());
			
		allDateDFMap.put(today.getExcelSerial(), w.fromDfToInterp(1.0));
		
		instruments.forEach(i -> allDateDFMap.put(i.getMaturityDate().getExcelSerial(), w.fromDfToInterp(i.getDiscountFactor())));
		edfswaps.forEach(i -> edfswapDateDFMap.put(i.getMaturityDate().getExcelSerial(), w.fromDfToInterp(i.getDiscountFactor())));
						
		LevenbergMarquardt optimizer = new LevenbergMarquardt() {
			@Override
			public void setValues(double[] x, double[] fi) {
				for (int i = 0; i < x.length; i++) {
					Double key = edfswapDateDFMap.keySet().toArray(new Double[edfswapDateDFMap.size()])[i];
		            allDateDFMap.put(key, x[i]);
		        }
				interp.reinitialize(ArrayUtils.toPrimitive(allDateDFMap.keySet().toArray(new Double[allDateDFMap.size()])), 
						ArrayUtils.toPrimitive(allDateDFMap.values().toArray(new Double[allDateDFMap.size()])));
				for (int i = 0; i < x.length; i++) {      
		             fi[i] = (calcParRate(edfswaps.get(i)) - edfswaps.get(i).getRateValue()) * 10000; 
		        }				
			}
		};
		optimizer.setInitialParameters(ArrayUtils.toPrimitive(edfswapDateDFMap.values().toArray(new Double[edfswapDateDFMap.size()])));
		double[] weights = new double[edfswapDateDFMap.values().size()];
		double[] targets = new double[edfswapDateDFMap.values().size()];
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
		double interpDF = interp.solve(excelSerial);
        return w.fromInterpToDf(interpDF);
	}
	public double getDiscFactorForSerial(double d) {
		double interpDF = interp.solve(d);
        return w.fromInterpToDf(interpDF);
	}
	
	public double calcParRate(CurveInstrument instr) {
		double output = 0;
		if (instr instanceof VanillaSwap) {
			VanillaSwap swap = (VanillaSwap)instr;
			double[] yfFixedLeg = swap.getFixedSchedule().getYFArray(swap.getFixedLeg().getDayCount()); 
			double[] dfDates = swap.getFixedSchedule().getPayDatesArray();
			double[] dfFixLeg = this.interp.curve(dfDates); 
	        for (int i = 0; i < yfFixedLeg.length; i++) {
	           dfFixLeg[i] = w.fromInterpToDf(dfFixLeg[i]);
	        }
	        output = FormulaUtil.parRate(yfFixedLeg, dfFixLeg); 
		} else if (instr instanceof EuroDollarFuture) {
			EuroDollarFuture edf = (EuroDollarFuture)instr;
			double beginDF = w.fromInterpToDf(interp.solve(edf.IMMDate.getExcelSerial()));
			double endDF = w.fromInterpToDf(interp.solve(edf.endDate.getExcelSerial()));
			output = endDF/beginDF;
		}
		return output;
	}
}

