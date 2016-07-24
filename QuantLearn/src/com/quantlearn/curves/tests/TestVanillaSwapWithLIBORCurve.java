package com.quantlearn.curves.tests;

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
import com.quantlearn.curves.Curve;
import com.quantlearn.curves.CurveInstrument;
import com.quantlearn.curves.Deposit;
import com.quantlearn.curves.DepositON;
import com.quantlearn.curves.DepositTN;
import com.quantlearn.curves.EuroDollarFuture;
import com.quantlearn.curves.SingleCurveGlobalFit;
import com.quantlearn.curves.SwapLeg;
import com.quantlearn.curves.VanillaSwap;
import com.quantlearn.enums.BusinessDayAdjustment;
import com.quantlearn.enums.BuySell;
import com.quantlearn.enums.CurveInstrumentType;
import com.quantlearn.enums.DC;
import com.quantlearn.enums.FixFloat;
import com.quantlearn.enums.Pay;
import com.quantlearn.enums.Rule;
import com.quantlearn.interpolation.OnLogDF;
import com.quantlearn.schedule.BusDate;
import com.quantlearn.schedule.Schedule;
import com.quantlearn.utils.FormulaUtil;
import com.quantlearn.utils.VanillaSwapUtils;
import com.quantlearn.interpolation.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class TestVanillaSwapWithLIBORCurve {
	public static String newline = System.getProperty("line.separator");	
	public SingleCurveGlobalFit curve = null;
	public ImmutableList<CurveInstrument> instruments = null;
	public static BusDate today = new BusDate(2016,7,13);
	public List<CurveInstrument> cinstruments = new ArrayList<>();
	public List<CurveInstrument> oisswaps = new ArrayList<>();
	
	public static void main(String[] args) {
		try {
			CacheBuilderHelper.buildHolidayCache("UK+NYSE");
			TestVanillaSwapWithLIBORCurve s = new TestVanillaSwapWithLIBORCurve();
			BusDate refDate = today.shiftPeriod("2D", BusinessDayAdjustment.ModifiedFollowing, "ADD");
			CacheBuilderHelper.cacheRefDate(refDate);	
			CacheBuilderHelper.buildHolidayCache("UK+NYSE");
			CacheBuilderHelper.buildHolidayCache("UK"); //for LIBOR resets
			
			
			SingleCurveGlobalFit curve = s.buildSingleCurve(today);
			BusDate settlementDate = new BusDate(2014,11,17);
			BusDate maturityDate = new BusDate(2024,11,17);
			double fixedRate = 0.02475;	
			VanillaSwap swap = s.buildSwap(settlementDate, maturityDate, fixedRate);
			double NPV = s.calcNPV(curve, swap);
			System.out.println("Swap NPV = " + NPV);
			double parallelShiftedUpNPV = s.parallelShiftedUpNPV(swap);
			System.out.println("Shited UP NPV = " + parallelShiftedUpNPV);
			double parallelShiftedDownNPV = s.parallelShiftedDownNPV(swap);
			System.out.println("Shited DOWN NPV = " + parallelShiftedDownNPV);
			double BPV = (Math.abs(parallelShiftedUpNPV) - Math.abs(parallelShiftedDownNPV))/2;
			System.out.println("BPV = " + BPV);
			double modDuration = 100 * Math.abs(BPV)/NPV;
			double totalBPV = s.calcSensitivities(swap, NPV);
			System.out.println("Total BPV = "+totalBPV);		
				
			
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
		
	public void recoverInputRatesFromCurve() {		
        System.out.println("Are we able to reproduce the inputs ?");
        instruments.forEach(i -> recoverInputRate(i));	
	}
	
	public double recoverInputRate(CurveInstrument instr) {
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
	
	public SingleCurveGlobalFit buildSingleCurve(BusDate today) throws Exception {	
		//calypso trade ID 537414
		BusDate refDate = RefDateUtils.getRefDate();
		
		List<Double> dates = new ArrayList<>();
		List<Double> DF = new ArrayList<>();
		
		DepositON depoON = new DepositON(today, 0.004127, "1D");
		double depoONDiscFactor = depoON.getDiscountFactor();
		BusDate depoONMaturityDate = depoON.getMaturityDate();
		cinstruments.add(depoON);
		dates.add(depoONMaturityDate.getExcelSerial());DF.add(depoONDiscFactor);
		
		DepositTN depoTN = new DepositTN(depoONMaturityDate, 0.004127, "2D", depoONDiscFactor);
		double depoTNDiscFactor = depoTN.getDiscountFactor();
		cinstruments.add(depoTN);
		dates.add(depoTN.getMaturityDate().getExcelSerial());DF.add(depoTNDiscFactor);
		
		Deposit depo1W = new Deposit(refDate, 0.004378, "1W");
		cinstruments.add(depo1W);
		dates.add(depo1W.getMaturityDate().getExcelSerial());DF.add(depo1W.getDiscountFactor());
		
		Deposit depo3M = new Deposit(refDate, 0.0068785, "3M");
		cinstruments.add(depo3M);
		dates.add(depo3M.getMaturityDate().getExcelSerial());DF.add(depo3M.getDiscountFactor());
		
		LinearInterp interp = new LinearInterp(dates,DF);		
		
		//SEP16
		EuroDollarFuture sep16 = new EuroDollarFuture(99.25, 9, 2016, interp.getInerpFunction());
		cinstruments.add(sep16);
		dates.add(sep16.getEndSerial()); DF.add(sep16.getDiscountFactor());
				
		//DEC16
		EuroDollarFuture dec16 = new EuroDollarFuture(99.175, 12, 2016);
		cinstruments.add(dec16);
				
		//MAR17
		EuroDollarFuture mar17 = new EuroDollarFuture(99.13, 3, 2017);
		cinstruments.add(mar17);
				
		//JUN17
		EuroDollarFuture jun17 = new EuroDollarFuture(99.085, 6, 2017);
		cinstruments.add(jun17);
				
		//SEP17
		EuroDollarFuture sep17 = new EuroDollarFuture(99.04, 9, 2017);
		cinstruments.add(sep17);
				
		//DEC17
		EuroDollarFuture dec17 = new EuroDollarFuture(98.985, 12, 2017);
		cinstruments.add(dec17);
				
		//MAR18
		EuroDollarFuture mar18 = new EuroDollarFuture(98.95, 3, 2018);
		cinstruments.add(mar18);
				
		//JUN18
		EuroDollarFuture jun18 = new EuroDollarFuture(98.91, 6, 2018);
		cinstruments.add(jun18);
				
		//swaps
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.009805, "3Y", "1Y", "3M"));
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
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.018475, "60Y", "1Y", "3M"));
		
		Collections.sort(cinstruments);
		instruments = ImmutableList.copyOf(cinstruments);
		//return new SingleCurveGlobalFit(today, instruments,new OnLogDF(),new Linear() );
		return new SingleCurveGlobalFit(today, instruments,new OnLogDF(),new CubicSpline() );
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
	
	public double calcNPV(Curve curve, VanillaSwap swap) {
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
	
	public double getFixedLegPV(Curve curve, VanillaSwap swap) {
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
	
	public double getFloatingLegPV(Curve curve, VanillaSwap swap) {
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
	public SingleCurveGlobalFit buildShiftedCurve(CurveInstrument c) throws Exception  {
		List<CurveInstrument> cloned = new ArrayList<>(cinstruments);
		Collections.sort(cloned);
		int index = cloned.indexOf(c);
		CurveInstrument i = cloned.get(index);
		CurveInstrument shiftedI = i.shiftUp1BP();
		cloned.set(index,shiftedI);
		return new SingleCurveGlobalFit(today, ImmutableList.copyOf(cloned),new OnLogDF(),new CubicSpline());
	}
	public SingleCurveGlobalFit buildParallelShiftedCurve(CurveInstrument c) throws Exception  {
		List<CurveInstrument> cloned = new ArrayList<>(cinstruments);
		Collections.sort(cloned);
		int index = cloned.indexOf(c);
		CurveInstrument i = cloned.get(index);
		CurveInstrument shiftedI = i.shiftUp1BP();
		cloned.set(index,shiftedI);
		instruments = ImmutableList.copyOf(cloned);
		return new SingleCurveGlobalFit(today, instruments,new OnLogDF(),new CubicSpline());
	}
	public double shiftedNPV(CurveInstrument c, VanillaSwap swap) {
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
		for (CurveInstrument i : cinstruments) {
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
		List<CurveInstrument> cloned = new ArrayList<>(cinstruments);
		Collections.sort(cloned);
		List<CurveInstrument> shifted = cloned.stream().map(c -> c.shiftUp1BP()).collect(Collectors.toList());
		instruments = ImmutableList.copyOf(shifted);
		SingleCurveGlobalFit c = new SingleCurveGlobalFit(today, instruments,new OnLogDF(),new CubicSpline());
		return calcNPV(c,swap);		
	}
	public double parallelShiftedDownNPV(VanillaSwap swap) throws Exception {
		List<CurveInstrument> cloned = new ArrayList<>(cinstruments);
		Collections.sort(cloned);
		List<CurveInstrument> shifted = cloned.stream().map(c -> c.shiftDown1BP()).collect(Collectors.toList());
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
		CurveInstrument c;
		VanillaSwap swap;
		double NPV;
		public FractionalBPV(CurveInstrument c,VanillaSwap swap,double NPV) {
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
