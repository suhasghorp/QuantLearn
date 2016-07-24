package com.quantlearn.curves;

import com.quantlearn.caching.RefDateUtils;
import com.quantlearn.enums.BuySell;
import com.quantlearn.enums.CurveInstrumentType;
import com.quantlearn.enums.Pay;
import com.quantlearn.schedule.BusDate;
import com.quantlearn.schedule.Period;
import com.quantlearn.schedule.Schedule;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Builder
@EqualsAndHashCode(exclude={"buySell", "notional", "fixedLeg", "floatingLeg", "fixedSchedule", "floatingSchedule"})
public class VanillaSwap implements CurveInstrument{
	
	private BusDate settleDate;     
	private BusDate maturityDate;  
	@Getter
	private double fixedRate; 
	private BuySell buySell;
	@Getter
	private double notional;
	@Getter
	private Pay pay;
	@Getter
	private SwapLeg fixedLeg;
	@Getter
	private SwapLeg floatingLeg;
	@Getter
	private Schedule fixedSchedule;
	@Getter
	private Schedule floatingSchedule;
	private String tenorString;
	
	@Override
	public BusDate getMaturityDate() {
		return this.maturityDate;
	}

	@Override
	public CurveInstrumentType getCurveInstrumentType() {
		return CurveInstrumentType.SWAP;
	}

	@Override
	public double getDiscountFactor() {		
		BusDate refDate = RefDateUtils.getRefDate();
        double yf = refDate.YF_365(this.maturityDate); 
        return Math.exp(- yf * (this.fixedRate));
	}

	@Override
	public double getRateValue() {
		return fixedRate;
	}
	@Override
	public double getLastFromDate() {
		double[] temp = this.getFloatingSchedule().getFromDatesArray();
		return temp[temp.length - 1];
	}
	
	@Override
	public VanillaSwap shiftUp1BP() {
		
		return VanillaSwap.builder()
				.settleDate(settleDate)
				.maturityDate(maturityDate)
				.fixedRate(fixedRate + 0.0001)
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
	@Override
	public VanillaSwap shiftDown1BP() {
		
		return VanillaSwap.builder()
				.settleDate(settleDate)
				.maturityDate(maturityDate)
				.fixedRate(fixedRate - 0.0001)
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

	@Override
	public int compareTo(CurveInstrument arg0) {
		return 0;
	}

	@Override
	public String getTenorString() {
		return this.tenorString;
	}



	
	
}
