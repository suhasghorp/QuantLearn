package com.quantlearn.utils;

import com.quantlearn.caching.RefDateUtils;
import com.quantlearn.enums.BusinessDayAdjustment;
import com.quantlearn.enums.BuySell;
import com.quantlearn.enums.DC;
import com.quantlearn.enums.FixFloat;
import com.quantlearn.enums.Pay;
import com.quantlearn.enums.Rule;
import com.quantlearn.ircurves.SwapLeg;
import com.quantlearn.ircurves.VanillaSwap;
import com.quantlearn.schedule.BusDate;
import com.quantlearn.schedule.Schedule;

public class VanillaSwapUtils {
	
	public static VanillaSwap buildMarketSwap(double fixedrate, String tenorString, String fixFreq, String floatFreq) {
		BusDate settlementDate = RefDateUtils.getRefDate();
		BusDate maturityDate = settlementDate.shiftPeriod(tenorString, BusinessDayAdjustment.Unadjusted, "ADD");	
		
		SwapLeg fixedLeg = SwapLeg.builder()
				.swapScheduleGeneratorRule(Rule.Backward)
				.payFreq(fixFreq)
				.busDayRollsAdj(BusinessDayAdjustment.ModifiedFollowing)
				.lagPayment("0D")
				.busDayPayAdj(BusinessDayAdjustment.ModifiedFollowing)
				.dayCount(DC._30_360)
				.fixedFloating(FixFloat.Fixed)
				.underlyingRateTenor("")
				.build();
		
		SwapLeg floatingLeg = SwapLeg.builder()
				.swapScheduleGeneratorRule(Rule.Backward)
				.payFreq(floatFreq)
				.busDayRollsAdj(BusinessDayAdjustment.ModifiedFollowing)
				.lagPayment("0D")
				.busDayPayAdj(BusinessDayAdjustment.ModifiedFollowing)
				.dayCount(DC._Act_360)
				.fixedFloating(FixFloat.Floating)
				.underlyingRateTenor(floatFreq)
				.build();
		
		Schedule fixedSchedule = Schedule.builder()
				.startDate(settlementDate)
				.endDate(maturityDate)
				.stringTenor(fixFreq)
				.generatorRule(Rule.Backward)
				.busDayAdjRolls(BusinessDayAdjustment.ModifiedFollowing)
				.busDayAdjPay(BusinessDayAdjustment.ModifiedFollowing)
				.payLag("0D")
				.resetLag("0D")
				.build();
		fixedSchedule.buildSchedule();
		
		Schedule floatingSchedule = Schedule.builder()
				.startDate(settlementDate)
				.endDate(maturityDate)
				.stringTenor(floatFreq)
				.generatorRule(Rule.Backward)
				.busDayAdjRolls(BusinessDayAdjustment.ModifiedFollowing)
				.busDayAdjPay(BusinessDayAdjustment.ModifiedFollowing)
				.payLag("0D")
				.resetLag("2D")
				.build();	
		floatingSchedule.buildSchedule();
		maturityDate = floatingSchedule.payDates.get(floatingSchedule.payDates.size()-1);
		
		return VanillaSwap.builder()
				.settleDate(settlementDate)
				.maturityDate(maturityDate)
				.fixedRate(fixedrate)
				.tenorString(tenorString)
				.buySell(BuySell.BUY)
				.pay(Pay.FIXED)
				.notional(1)
				.fixedLeg(fixedLeg)
				.floatingLeg(floatingLeg)
				.fixedSchedule(fixedSchedule)
				.floatingSchedule(floatingSchedule)
				.build();	
	}
	
	public static VanillaSwap buildMarketOISSwap(double fixedrate, String tenorString, String fixFreq, String floatFreq) {
		BusDate settlementDate = RefDateUtils.getRefDate();
		BusDate maturityDate = settlementDate.shiftPeriod(tenorString, BusinessDayAdjustment.Unadjusted, "ADD");	
		
		SwapLeg fixedLeg = SwapLeg.builder()
				.swapScheduleGeneratorRule(Rule.Backward)
				.payFreq(fixFreq)
				.busDayRollsAdj(BusinessDayAdjustment.Unadjusted)
				.lagPayment("0D")
				.busDayPayAdj(BusinessDayAdjustment.Unadjusted)
				.dayCount(DC._Act_360)
				.fixedFloating(FixFloat.Fixed)
				.build();
		
		SwapLeg floatingLeg = SwapLeg.builder()
				.swapScheduleGeneratorRule(Rule.Backward)
				.payFreq(floatFreq)
				.busDayRollsAdj(BusinessDayAdjustment.Unadjusted)
				.lagPayment("0D")
				.busDayPayAdj(BusinessDayAdjustment.Unadjusted)
				.dayCount(DC._Act_360)
				.fixedFloating(FixFloat.Floating)
				.underlyingRateTenor("1D")
				.build();
		
		Schedule fixedSchedule = Schedule.builder()
				.startDate(settlementDate)
				.endDate(maturityDate)
				.stringTenor(fixFreq)
				.generatorRule(Rule.Backward)
				.busDayAdjRolls(BusinessDayAdjustment.Unadjusted)
				.busDayAdjPay(BusinessDayAdjustment.Unadjusted)
				.payLag("0D")
				.resetLag("0D")
				.build();
		fixedSchedule.buildSchedule();
		fixedSchedule.printSchedule();
		
		Schedule floatingSchedule = Schedule.builder()
				.startDate(settlementDate)
				.endDate(maturityDate)
				.stringTenor(floatFreq)
				.generatorRule(Rule.Backward)
				.busDayAdjRolls(BusinessDayAdjustment.Unadjusted)
				.busDayAdjPay(BusinessDayAdjustment.Unadjusted)
				.payLag("0D")
				.resetLag("0D")
				.build();	
		floatingSchedule.buildSchedule();
		floatingSchedule.printSchedule();
		
		return VanillaSwap.builder()
				.settleDate(settlementDate)
				.maturityDate(maturityDate)
				.fixedRate(fixedrate)
				.tenorString(tenorString)
				.buySell(BuySell.BUY)
				.pay(Pay.FIXED)
				.notional(1)
				.fixedLeg(fixedLeg)
				.floatingLeg(floatingLeg)
				.fixedSchedule(fixedSchedule)
				.floatingSchedule(floatingSchedule)
				.build();	
	}

}
