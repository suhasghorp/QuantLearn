package com.quantlearn.ircurves.tests;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;

import com.quantlearn.caching.CacheBuilderHelper;
import com.quantlearn.caching.CacheManager;
import com.quantlearn.caching.RefDateUtils;
import com.quantlearn.enums.BusinessDayAdjustment;
import com.quantlearn.enums.BuySell;
import com.quantlearn.enums.CurveInstrumentType;
import com.quantlearn.enums.DC;
import com.quantlearn.enums.FixFloat;
import com.quantlearn.enums.Pay;
import com.quantlearn.enums.Rule;
import com.quantlearn.interpolation.OnLogDF;
import com.quantlearn.ircurves.IRCurve;
import com.quantlearn.ircurves.IRCurveInstrument;
import com.quantlearn.ircurves.Deposit;
import com.quantlearn.ircurves.DepositON;
import com.quantlearn.ircurves.DepositTN;
import com.quantlearn.ircurves.EuroDollarFuture;
import com.quantlearn.ircurves.SingleCurveGlobalFit;
import com.quantlearn.ircurves.SingleCurveISDA;
import com.quantlearn.ircurves.SwapLeg;
import com.quantlearn.ircurves.VanillaSwap;
import com.quantlearn.schedule.BusDate;
import com.quantlearn.schedule.Schedule;
import com.quantlearn.utils.FormulaUtil;
import com.quantlearn.utils.VanillaSwapUtils;
import com.quantlearn.interpolation.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class TestVanillaSwapWithISDACurve {
	public static String newline = System.getProperty("line.separator");	
	public IRCurve curve = null;
	public ImmutableList<IRCurveInstrument> instruments = null;
	public static BusDate today = new BusDate(2016,7,13);
	public List<IRCurveInstrument> cinstruments = new ArrayList<>();
	public List<IRCurveInstrument> oisswaps = new ArrayList<>();
	
	public static void main(String[] args) {
		try {			
			TestVanillaSwapWithISDACurve t = new TestVanillaSwapWithISDACurve();
			t.test();			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void test() throws Exception {
		CacheBuilderHelper.cacheCalendarName("NONE");
		CacheBuilderHelper.buildHolidayCache();
		//CacheBuilderHelper.buildHolidayCache("UK"); //for LIBOR resets
		
		BusDate refDate = today.shiftPeriod("2D", BusinessDayAdjustment.ModifiedFollowing, "ADD");		
		CacheBuilderHelper.cacheRefDate(refDate);			
		
		curve = buildSingleCurve(today);
		BusDate settlementDate = new BusDate(2014,11,17);
		BusDate maturityDate = new BusDate(2024,11,17);
		double fixedRate = 0.02475;	
		VanillaSwap swap = buildSwap(settlementDate, maturityDate, fixedRate);
		double NPV = calcNPV(curve, swap);
		System.out.println("Swap NPV = " + NPV);
		double parallelShiftedUpNPV = parallelShiftedUpNPV(swap);
		System.out.println("Shited UP NPV = " + parallelShiftedUpNPV);
		double parallelShiftedDownNPV = parallelShiftedDownNPV(swap);
		System.out.println("Shited DOWN NPV = " + parallelShiftedDownNPV);
		double BPV = (Math.abs(parallelShiftedUpNPV) - Math.abs(parallelShiftedDownNPV))/2;
		System.out.println("BPV = " + BPV);
		double modDuration = 100 * Math.abs(BPV)/NPV;
		double totalBPV = calcSensitivities(swap, NPV);
		System.out.println("Total BPV = "+totalBPV);		
	}
	
		
	public void recoverInputRatesFromCurve() {		
        System.out.println("Are we able to reproduce the inputs ?");
        instruments.forEach(i -> recoverInputRate(i));	
	}
	
	public double recoverInputRate(IRCurveInstrument instr) {
		BusDate refDate = new BusDate();
		double calcRate = 0;
		if (instr.getCurveInstrumentType() == CurveInstrumentType.DEPO) {
			
			Deposit depo = (Deposit)instr;
			double yf = refDate.getYearFraction(depo.maturityDate, depo.dayCount);
            double df = curve.getDiscFactorForDate(depo.maturityDate);
            calcRate = ((1 / df) - 1) / yf;
            System.out.format("%s  Input Rate: %s  Recalc Rate: %s Diff: %s" + newline, 
            		depo.Tenor.getPeriodStringFormat(), 
            		depo.rateValue, 
            		calcRate, 
            		(calcRate - depo.rateValue));
            
		} else if (instr.getCurveInstrumentType() == CurveInstrumentType.SWAP) {			
			VanillaSwap swap = (VanillaSwap)instr;
			double[] yfFixedLeg = swap.getFixedSchedule().getYFArray(swap.getFixedLeg().getDayCount()); 
            BusDate[] dfDates = swap.getFixedSchedule().payDates.toArray(new BusDate[swap.getFixedSchedule().payDates.size()]);
            double[] dfFixLeg = new double[dfDates.length];
            for (int i = 0; i < dfDates.length; i++) {
                dfFixLeg[i] = curve.getDiscFactorForDate(dfDates[i]);
            }
            calcRate = FormulaUtil.parRate(yfFixedLeg, dfFixLeg);
            System.out.format("%s  Input Rate: %s  Recalc Rate: %s Diff: %s" + newline, 
            		"swap", 
            		swap.getFixedRate(),
            		calcRate, 
            		(calcRate - swap.getFixedRate()));	
		}
		return calcRate;
	}
	
	public IRCurve buildSingleCurve2(BusDate today) throws Exception {	
		//calypso trade ID 537414
		BusDate refDate = RefDateUtils.getRefDate();
		
		List<Double> dates = new ArrayList<>();
		List<Double> DF = new ArrayList<>();
		
		Deposit depo1M = new Deposit(refDate, 0.742E-2, "1M");
		cinstruments.add(depo1M);
		dates.add(depo1M.getMaturityDate().getExcelSerial());
		DF.add(depo1M.getDiscountFactor());
		
		Deposit depo3M = new Deposit(refDate, 0.952e-2, "3M");
		cinstruments.add(depo3M);
		dates.add(depo3M.getMaturityDate().getExcelSerial());
		DF.add(depo3M.getDiscountFactor());
		
		Deposit depo6M = new Deposit(refDate, 1.201e-2, "6M");
		cinstruments.add(depo6M);
		dates.add(depo6M.getMaturityDate().getExcelSerial());DF.add(depo6M.getDiscountFactor());
		
		//swaps
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(1.287e-2, "1Y", "1Y", "6M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(1.465e-2, "2Y", "1Y", "6M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(1.652e-2, "3Y", "1Y", "6M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(1.819e-2, "4Y", "1Y", "6M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(1.922e-2, "5Y", "1Y", "6M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(2.102e-2, "6Y", "1Y", "6M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(2.425e-2, "7Y", "1Y", "6M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(2.225e-2, "8Y", "1Y", "6M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(2.556e-2, "9Y", "1Y", "6M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(2.794e-2, "10Y", "1Y", "6M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(2.968e-2, "15Y", "1Y", "6M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(2.9895e-2, "20Y", "1Y", "6M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(2.879e-2, "25Y", "1Y", "6M"));
		
		
				
		Collections.sort(cinstruments);
		instruments = ImmutableList.copyOf(cinstruments);
		//return new SingleCurveGlobalFit(today, instruments,new OnLogDF(),new Linear() );
		//return new SingleCurveGlobalFit(today, instruments,new OnLogDF(),new CubicSpline() );
		return new SingleCurveISDA(today, instruments,new OnLogDF(),new Linear() );
	}
	
	public IRCurve buildSingleCurve(BusDate today) throws Exception {	
		//calypso trade ID 537414
		BusDate refDate = RefDateUtils.getRefDate();
		
		List<Double> dates = new ArrayList<>();
		List<Double> DF = new ArrayList<>();
		
		Deposit depo1M = new Deposit(refDate, 0.004814, "1M");
		cinstruments.add(depo1M);
		dates.add(depo1M.getMaturityDate().getExcelSerial());
		DF.add(depo1M.getDiscountFactor());
		
		Deposit depo2M = new Deposit(refDate, 0.005633, "2M");
		cinstruments.add(depo2M);
		dates.add(depo2M.getMaturityDate().getExcelSerial());
		DF.add(depo2M.getDiscountFactor());
		
		Deposit depo3M = new Deposit(refDate, 0.006801, "3M");
		cinstruments.add(depo3M);
		dates.add(depo3M.getMaturityDate().getExcelSerial());
		DF.add(depo3M.getDiscountFactor());
		
		Deposit depo6M = new Deposit(refDate, 0.009801, "6M");
		cinstruments.add(depo6M);
		dates.add(depo6M.getMaturityDate().getExcelSerial());DF.add(depo6M.getDiscountFactor());
		
		Deposit depo1Y = new Deposit(refDate, 0.012934, "1Y");
		cinstruments.add(depo1Y);
		dates.add(depo1Y.getMaturityDate().getExcelSerial());DF.add(depo1Y.getDiscountFactor());
				
		//swaps
		/*cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.009805, "3Y", "1Y", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.0104825, "4Y", "1Y", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.0112475, "5Y", "1Y", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.01194, "6Y", "1Y", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.0126875, "7Y", "1Y", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.013325, "8Y", "1Y", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.0139225, "9Y", "1Y", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.014495, "10Y", "1Y", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.0154575, "12Y", "1Y", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.0165175, "15Y", "1Y", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.0176425, "20Y", "1Y", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.01819, "25Y", "1Y", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.0185125, "30Y", "1Y", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.01869, "40Y", "1Y", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.01856, "50Y", "1Y", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.018475, "60Y", "1Y", "3M"));*/
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.008205, "2Y", "6M", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.00894, "3Y", "6M", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.009665, "4Y", "6M", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.010295, "5Y", "6M", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.01102, "6Y", "6M", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.011605, "7Y", "6M", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.012275, "8Y", "6M", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.012855, "9Y", "6M", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.01331, "10Y", "6M", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.014335, "12Y", "6M", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.015345, "15Y", "6M", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.016455, "20Y", "6M", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.016955, "25Y", "6M", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.01727, "30Y", "6M", "3M"));
				
		Collections.sort(cinstruments);
		instruments = ImmutableList.copyOf(cinstruments);
		//return new SingleCurveGlobalFit(today, instruments,new OnLogDF(),new Linear() );
		//return new SingleCurveGlobalFit(today, instruments,new OnLogDF(),new CubicSpline() );
		return new SingleCurveISDA(today, instruments,new OnLogDF(),new Linear() );
	}
	
	public VanillaSwap buildSwap(BusDate settlementDate, BusDate maturityDate, double fixedRate) {
			
		SwapLeg fixedLeg = SwapLeg.builder()
				.swapScheduleGeneratorRule(Rule.Backward)
				.payFreq("6M")
				.busDayRollsAdj(BusinessDayAdjustment.ModifiedFollowing)
				.lagPayment("0D")
				.busDayPayAdj(BusinessDayAdjustment.ModifiedFollowing)
				.dayCount(DC._30_360)
				.fixedFloating(FixFloat.Fixed)
				.build();
		SwapLeg floatingLeg = SwapLeg.builder()
				.swapScheduleGeneratorRule(Rule.Backward)
				.payFreq("3M")
				.busDayRollsAdj(BusinessDayAdjustment.ModifiedFollowing)
				.lagPayment("0D")
				.busDayPayAdj(BusinessDayAdjustment.ModifiedFollowing)
				.dayCount(DC._Act_360)
				.fixedFloating(FixFloat.Floating)
				.build();
		Schedule fixedSchedule = Schedule.builder()
				.startDate(settlementDate)
				.endDate(maturityDate)
				.stringTenor("6M")
				.generatorRule(Rule.Backward)
				.busDayAdjRolls(BusinessDayAdjustment.ModifiedFollowing)
				.busDayAdjPay(BusinessDayAdjustment.ModifiedFollowing)
				.payLag("0D")
				.resetLag("0D")
				.build();
		fixedSchedule.buildSchedule();
		System.out.println("========== Fixed Schedue =============");
		fixedSchedule.printSchedule();
		Schedule floatingSchedule = Schedule.builder()
				.startDate(settlementDate)
				.endDate(maturityDate)
				.stringTenor("3M")
				.generatorRule(Rule.Backward)
				.busDayAdjRolls(BusinessDayAdjustment.ModifiedFollowing)
				.busDayAdjPay(BusinessDayAdjustment.ModifiedFollowing)
				.payLag("0D")
				.resetLag("2D")
				.build();	
		floatingSchedule.buildSchedule();
		System.out.println("========== Floating Schedue =============");
		floatingSchedule.printSchedule();
		VanillaSwap swap = VanillaSwap.builder()
							.settleDate(settlementDate)
							.maturityDate(maturityDate)
							.fixedRate(fixedRate)
							.tenorString("10Y")
							.buySell(BuySell.BUY)
							.pay(Pay.FIXED)
							.notional(4000000)
							.fixedLeg(fixedLeg)
							.floatingLeg(floatingLeg)
							.fixedSchedule(fixedSchedule)
							.floatingSchedule(floatingSchedule)
							.build();
		return swap;
	}
	
	public double calcNPV(IRCurve curve, VanillaSwap swap) {
		double fixedLegPV = getFixedLegPV(curve, swap);
		double floatingLegPV = getFloatingLegPV(curve, swap);
		double NPV = 0;
		if(swap.getPay() == Pay.FIXED) {
			NPV = floatingLegPV - fixedLegPV;
		} else if (swap.getPay() == Pay.FLOATING) {
			NPV = fixedLegPV - floatingLegPV;
		}
		return NPV;
	}
	
	public double getFixedLegPV(IRCurve curve, VanillaSwap swap) {
		double calcFixedLegPV = 0.00;
		BusDate refDate = RefDateUtils.getRefDate();
		Schedule fixedSchedule = swap.getFixedSchedule();
		double notional = swap.getNotional();
		List<Double> yfList = fixedSchedule.getYF(DC._Act_360);
        double[] yfArray = ArrayUtils.toPrimitive(yfList.toArray(new Double[yfList.size()]));
        for (int j = 0; j < yfArray.length; j++) {
        	if (fixedSchedule.payDates.get(j).getDate().isAfter(refDate.getDate())) {
        		double temp = curve.getDiscFactorForDate(fixedSchedule.payDates.get(j));
        		calcFixedLegPV += notional * curve.getDiscFactorForDate(fixedSchedule.payDates.get(j)) * yfArray[j] * swap.getFixedRate();
        	}
        }
        return calcFixedLegPV;
	}
	
	public double getFloatingLegPV(IRCurve curve, VanillaSwap swap) {
		double calcFloatingLegPV = 0.00;
		BusDate refDate = RefDateUtils.getRefDate();
		Schedule floatingSchedule = swap.getFloatingSchedule();
		double notional = swap.getNotional();
		ImmutableMap<LocalDate,Double> fixings = ImmutableMap.of(LocalDate.of(2016, 5, 13), 0.006276);
        List<Double> yfList = floatingSchedule.getYF(DC._Act_360);
        double[] yfArray = ArrayUtils.toPrimitive(yfList.toArray(new Double[yfList.size()]));
        
        for (int j = 0; j < yfArray.length; j++) {
        	if (floatingSchedule.fromDates.get(j).getDate().isBefore(refDate.getDate()) &&
        			floatingSchedule.toDates.get(j).getDate().isAfter(refDate.getDate())) {
        		LocalDate fixingDate = floatingSchedule.fromDates.get(j).applyResetLag("2D").getDate();
        		double fixingRate = fixings.get(fixingDate);
        		calcFloatingLegPV += notional * fixingRate * yfArray[j];
        		
        	} else if (floatingSchedule.fromDates.get(j).getDate().isAfter(refDate.getDate()) &&
        			floatingSchedule.toDates.get(j).getDate().isAfter(refDate.getDate())) {
	            double startDiscfactor = curve.getDiscFactorForDate(floatingSchedule.fromDates.get(j));
	            double endDiscFactor = curve.getDiscFactorForDate(floatingSchedule.toDates.get(j));
	            double fwdRate = (startDiscfactor / endDiscFactor - 1) / yfArray[j];
	
	            calcFloatingLegPV += notional * fwdRate * yfArray[j] * curve.getDiscFactorForDate(floatingSchedule.payDates.get(j));
        	}
        }
        return calcFloatingLegPV;
	}
	
	public double getParSwapRate(SingleCurveGlobalFit curve, VanillaSwap swap) {
		double parRate = 0.00;
		BusDate refDate = RefDateUtils.getRefDate();
		Schedule fixedSchedule = swap.getFixedSchedule();
		List<Double> yfList = fixedSchedule.getYF(DC._Act_360);
        double[] yfArray = ArrayUtils.toPrimitive(yfList.toArray(new Double[yfList.size()]));
        double[] df = new double[yfList.size()];
        for (int j = 0; j < yfArray.length; j++) {
        	if (fixedSchedule.payDates.get(j).getDate().isAfter(refDate.getDate())) {
        		df[j] = curve.getDiscFactorForDate(fixedSchedule.payDates.get(j));
        	}
        }
        parRate = FormulaUtil.parRate(yfArray, df);
        return parRate;		
	}
	public SingleCurveGlobalFit buildShiftedCurve(IRCurveInstrument c) throws Exception  {
		List<IRCurveInstrument> cloned = new ArrayList<>(cinstruments);
		Collections.sort(cloned);
		int index = cloned.indexOf(c);
		IRCurveInstrument i = cloned.get(index);
		IRCurveInstrument shiftedI = i.shiftUp1BP();
		cloned.set(index,shiftedI);
		return new SingleCurveGlobalFit(today, ImmutableList.copyOf(cloned),new OnLogDF(),new CubicSpline());
	}
	public SingleCurveGlobalFit buildParallelShiftedCurve(IRCurveInstrument c) throws Exception  {
		List<IRCurveInstrument> cloned = new ArrayList<>(cinstruments);
		Collections.sort(cloned);
		int index = cloned.indexOf(c);
		IRCurveInstrument i = cloned.get(index);
		IRCurveInstrument shiftedI = i.shiftUp1BP();
		cloned.set(index,shiftedI);
		instruments = ImmutableList.copyOf(cloned);
		return new SingleCurveGlobalFit(today, instruments,new OnLogDF(),new CubicSpline());
	}
	public double shiftedNPV(IRCurveInstrument c, VanillaSwap swap) {
		double NPV = 0;	
		try {
			SingleCurveGlobalFit curve = buildShiftedCurve(c);
			NPV = calcNPV(curve,swap);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return NPV;
	}
	
	
	public double calcSensitivities(VanillaSwap swap, double NPV) throws Exception {		
		double totalBPV = 0;
		/*for (CurveInstrument i : cinstruments) {
			fractionalBPV = Math.abs(shiftedNPV(i, swap)) - Math.abs(NPV);
			System.out.println("Fractional BPV @ " + i.getTenorString() +" = " + fractionalBPV);
			totalBPV += fractionalBPV;
		}*/
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		Set<Callable<Double>> callables = new HashSet<Callable<Double>>();
		for (IRCurveInstrument i : cinstruments) {
			callables.add(new FractionalBPV (i,swap,NPV));
		}
		List<Future<Double>> futures = executorService.invokeAll(callables);
		for(Future<Double> future : futures){
		    totalBPV += future.get();
		}
		executorService.shutdown();			
		return totalBPV;
	}
	public double parallelShiftedUpNPV(VanillaSwap swap) throws Exception {
		List<IRCurveInstrument> cloned = new ArrayList<>(cinstruments);
		Collections.sort(cloned);
		List<IRCurveInstrument> shifted = cloned.stream().map(c -> c.shiftUp1BP()).collect(Collectors.toList());
		instruments = ImmutableList.copyOf(shifted);
		SingleCurveGlobalFit c = new SingleCurveGlobalFit(today, instruments,new OnLogDF(),new CubicSpline());
		return calcNPV(c,swap);		
	}
	public double parallelShiftedDownNPV(VanillaSwap swap) throws Exception {
		List<IRCurveInstrument> cloned = new ArrayList<>(cinstruments);
		Collections.sort(cloned);
		List<IRCurveInstrument> shifted = cloned.stream().map(c -> c.shiftDown1BP()).collect(Collectors.toList());
		instruments = ImmutableList.copyOf(shifted);
		SingleCurveGlobalFit c = new SingleCurveGlobalFit(today, instruments,new OnLogDF(),new CubicSpline());
		return calcNPV(c,swap);		
	}
	
	
	
	public class LinearInterp {
		public List<Double> dates, DF;
		public LinearInterp(List<Double> dates, List<Double> DF) {
			this.dates = dates;
			this.DF = DF;
		}
		public void reinitialize(List<Double> dates, List<Double> DF) {
			this.dates = dates;
			this.DF = DF;
		}
		public UnivariateFunction getInerpFunction() {
			UnivariateInterpolator interpolator = new LinearInterpolator();
			double[] X = ArrayUtils.toPrimitive(dates.toArray(new Double[dates.size()]));
			double[] Y = ArrayUtils.toPrimitive(DF.toArray(new Double[DF.size()]));
			return interpolator.interpolate(X,Y);
		}
	}
	
	public class FractionalBPV implements Callable<Double> {
		IRCurveInstrument c;
		VanillaSwap swap;
		double NPV;
		public FractionalBPV(IRCurveInstrument c,VanillaSwap swap,double NPV) {
			this.c = c;
			this.swap = swap;
			this.NPV = NPV;
		}
		@Override
		public Double call() throws Exception {
			double fractionalBPV = Math.abs(shiftedNPV(c, swap)) - Math.abs(NPV);
			System.out.println("Fractional BPV @ " + c.getTenorString() +" = " + fractionalBPV);
			return (Double)fractionalBPV;
		}
		
	}
	
}