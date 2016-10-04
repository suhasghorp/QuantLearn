package com.quantlearn.ircurves;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.quantlearn.caching.RefDateUtils;
import com.quantlearn.enums.BusinessDayAdjustment;
import com.quantlearn.enums.CurveInstrumentType;
import com.quantlearn.enums.DC;
import com.quantlearn.enums.TenorType;
import com.quantlearn.interpolation.BaseInterpolator;
import com.quantlearn.interpolation.InterpolateOnWhat;
import com.quantlearn.interpolation.Linear;
import com.quantlearn.schedule.BusDate;
import com.quantlearn.schedule.Period;
import com.quantlearn.utils.FormulaUtil;
import com.quantlearn.utils.VanillaSwapUtils;

import net.finmath.optimizer.LevenbergMarquardt;

public class SingleCurveISDA implements IRCurve {
	private BusDate refDate;
	TreeMap<Double,Double> allDateDFMap = new TreeMap<Double,Double>();
	TreeMap<Double,Double> edfswapDateDFMap = new TreeMap<Double,Double>();
	double[] payDatesSerial;	
	InterpolateOnWhat w = null;
	BaseInterpolator interp = null;
	
	@SuppressWarnings("unchecked")
	public SingleCurveISDA(BusDate today, ImmutableList<IRCurveInstrument> instruments, InterpolateOnWhat w, BaseInterpolator interp) throws Exception {
		this.w = w;
		this.interp = interp;
		refDate = RefDateUtils.getRefDate();
		
		List<IRCurveInstrument> givenSwaps = instruments.stream()
				.filter(i -> i.getCurveInstrumentType() == CurveInstrumentType.SWAP)
				.map(instrument -> ((IRCurveInstrument)instrument))
				.collect(Collectors.toList());
		
		Double[] d = ArrayUtils.toObject((((VanillaSwap)Iterables.getLast(givenSwaps)).getFloatingSchedule().getFromDatesArray()));
		List<Double> fromDatesSerial = Arrays.asList(d);
		DC dc = ((VanillaSwap)Iterables.getLast(givenSwaps)).getFloatingLeg().getDayCount();
		List<Double> yfFloatingLeg = (((VanillaSwap)Iterables.getLast(givenSwaps)).getFloatingSchedule().getYF(dc));
		payDatesSerial = (((VanillaSwap)Iterables.getLast(givenSwaps)).getFloatingSchedule().getPayDatesArray());
		//List<Double> payDatesSerial = Arrays.asList(d);
		int N = payDatesSerial.length;
				
		String underlyingTenor = ((VanillaSwap)givenSwaps.get(0)).getFloatingLeg().getUnderlyingRateTenor();
	
		Map<Double,Double> dateDF = new TreeMap<>();
		dateDF.put(refDate.getExcelSerial(), 1.0);
		for (IRCurveInstrument i : instruments) {
			if (i.getCurveInstrumentType() == CurveInstrumentType.DEPO) {
				if (!i.getTenorString().equals(underlyingTenor)) {
					double yf = refDate.getYearFraction(i.getMaturityDate(), ((Deposit)i).dayCount);
					double df = FormulaUtil.DFsimple(yf, i.getRateValue());
					dateDF.put(i.getMaturityDate().getExcelSerial(), df);
				}
			}
		}
		
		double fixingGuess = instruments.stream()
				.filter(i -> i.getCurveInstrumentType() == CurveInstrumentType.DEPO)
				.filter(c -> c.getTenorString().equals(underlyingTenor))
				.mapToDouble(x -> x.getRateValue()).average().getAsDouble();
		
		double[] fwdGuesses = new double[N];
		fwdGuesses[0] = fixingGuess;
		double[] xx = new double[N-1];
		Arrays.fill(xx, fixingGuess);
		
		LevenbergMarquardt optimizer = new LevenbergMarquardt() {
			@Override
			public void setValues(double[] x, double[] fi) {
				System.arraycopy(x, 0, fwdGuesses, 1, N-1);
				double[] dfFloatLegLongerSwap = FormulaUtil.DFSimpleList(yfFloatingLeg, fwdGuesses);
				
				for (int i = 0; i < fi.length - 1; i++) {      
		             fi[i] = (calcParRate(givenSwaps.get(i),dfFloatLegLongerSwap) - givenSwaps.get(i).getRateValue()) * 10000; 
				}				
			}
		};
		optimizer.setInitialParameters(xx);
		double[] weights = new double[givenSwaps.size()+1];
		double[] targets = new double[givenSwaps.size()+1];
		Arrays.fill(weights, 1.0);
		Arrays.fill(targets, 0.0);
		optimizer.setWeights(weights);
		optimizer.setMaxIteration(100000);
		optimizer.setTargetValues(targets);
		
		optimizer.run();
		double[] bestParameters = optimizer.getBestFitParameters();
		double[] optimizedFwd = new double[bestParameters.length+1];
		optimizedFwd[0] = fixingGuess;
		System.arraycopy(bestParameters, 0, optimizedFwd, 1, bestParameters.length);
		double[] df = FormulaUtil.DFSimpleList(yfFloatingLeg, optimizedFwd);
		List<Double> adaptedDF = new ArrayList<>();
		for (int i = 0; i < payDatesSerial.length; i++) {
			dateDF.put(payDatesSerial[i], df[i]);
			
		}
		for (Map.Entry<Double, Double> entry : dateDF.entrySet()) {
			adaptedDF.add(w.fromDfToInterp(entry.getValue()));
		}
		interp.reinitialize(ArrayUtils.toPrimitive(dateDF.keySet().toArray(new Double[dateDF.size()])), 
				ArrayUtils.toPrimitive(adaptedDF.toArray(new Double[dateDF.size()])));		
	}
	
	public double calcParRate(IRCurveInstrument instr, double[] df) {
		double output = 0;
		if (instr instanceof VanillaSwap) {
			VanillaSwap swap = (VanillaSwap)instr;
			double[] yfFixedLeg = swap.getFixedSchedule().getYFArray(DC._30_360); 
			double[] dfDates = swap.getFixedSchedule().getPayDatesArray();
			double[] dfFixLeg = new double[dfDates.length];
			
			this.interp.reinitialize(payDatesSerial, df);
			
	        for (int i = 0; i < dfDates.length; i++) {
	           dfFixLeg[i] = this.interp.solve(dfDates[i]);
	        }
	        output = FormulaUtil.parRate(yfFixedLeg, dfFixLeg); 
		}
		return output;
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
}
