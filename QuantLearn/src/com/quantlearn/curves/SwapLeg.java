package com.quantlearn.curves;

import com.quantlearn.enums.BusinessDayAdjustment;
import com.quantlearn.enums.DC;
import com.quantlearn.enums.FixFloat;
import com.quantlearn.enums.Rule;

import lombok.Builder;
import lombok.Getter;

@Builder
public class SwapLeg {
	private Rule swapScheduleGeneratorRule;  // rule to generate swap schedule
	private String payFreq; // pay frequency of leg
	private BusinessDayAdjustment busDayRollsAdj;  // business day conventions for rolls
	private String lagPayment; // lag between final day and payment date
	private BusinessDayAdjustment busDayPayAdj; // business day conventions for payment
	@Getter
	private DC dayCount; // day count
	private FixFloat fixedFloating;  // Fix or Floating
	@Getter
	private String underlyingRateTenor;  // underlying rate tenor 
	
}
