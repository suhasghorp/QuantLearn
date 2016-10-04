package com.quantlearn.creditcurve.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.quantlearn.caching.CacheBuilderHelper;
import com.quantlearn.caching.RefDateUtils;
import com.quantlearn.creditcurve.SingleNameBootStrapCurve;
import com.quantlearn.enums.BusinessDayAdjustment;
import com.quantlearn.interpolation.Linear;
import com.quantlearn.interpolation.OnLogDF;
import com.quantlearn.ircurves.Deposit;
import com.quantlearn.ircurves.IRCurve;
import com.quantlearn.ircurves.IRCurveInstrument;
import com.quantlearn.ircurves.SingleCurveISDA;
import com.quantlearn.ircurves.tests.TestVanillaSwapWithISDACurve;
import com.quantlearn.schedule.BusDate;
import com.quantlearn.utils.VanillaSwapUtils;

public class TestSingleNameBootStrapCurve {
	public static String newline = System.getProperty("line.separator");	
	public IRCurve curve = null;
	public ImmutableList<IRCurveInstrument> instruments = null;
	public static BusDate today = new BusDate(2016,7,13);
	public List<IRCurveInstrument> cinstruments = new ArrayList<>();
	public List<IRCurveInstrument> oisswaps = new ArrayList<>();
	
	public static void main(String[] args) {
		try {			
			TestSingleNameBootStrapCurve t = new TestSingleNameBootStrapCurve();
			t.test();			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void test() throws Exception {
		CacheBuilderHelper.cacheCalendarName("NONE");
		CacheBuilderHelper.buildHolidayCache();
		BusDate refDate = today.shiftPeriod("2D", BusinessDayAdjustment.ModifiedFollowing, "ADD");		
		CacheBuilderHelper.cacheRefDate(refDate);	
		IRCurve curve = buildSingleCurve(today);
		
		List<Double> tenors = Arrays.asList(new Double[]{  1d, 2d, 3d, 4d, 5d, 10d, 30d });
		List<Double> spreads = Arrays.asList(new Double[]{ 0.0033, 0.0055, 0.0081, 0.0106, 0.0132, 0.0192, 0.0211 });
		int premiumFrequency = 4;
		int defaultFrequency = 52;
		int accruedPremiumFlag = 1;
		double RR = 0.39;
		
		SingleNameBootStrapCurve ccurve = new SingleNameBootStrapCurve(tenors, spreads, premiumFrequency, defaultFrequency, true, RR, curve);
				
		List<Double> ret = ccurve.bootstrapCDSSpreads();
		System.out.println(ret);
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
		
		/*cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.008205, "2Y", "1Y", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.00894, "3Y", "1Y", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.009665, "4Y", "1Y", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.010295, "5Y", "1Y", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.01102, "6Y", "1Y", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.011605, "7Y", "1Y", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.012275, "8Y", "1Y", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.012855, "9Y", "1Y", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.01331, "10Y", "1Y", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.014335, "12Y", "1Y", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.015345, "15Y", "1Y", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.016455, "20Y", "1Y", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.016955, "25Y", "1Y", "3M"));
		cinstruments.add(VanillaSwapUtils.buildMarketSwap(0.01727, "30Y", "1Y", "3M"));*/
				
		Collections.sort(cinstruments);
		instruments = ImmutableList.copyOf(cinstruments);
		//return new SingleCurveGlobalFit(today, instruments,new OnLogDF(),new Linear() );
		//return new SingleCurveGlobalFit(today, instruments,new OnLogDF(),new CubicSpline() );
		return new SingleCurveISDA(today, instruments,new OnLogDF(),new Linear() );
	}

}
