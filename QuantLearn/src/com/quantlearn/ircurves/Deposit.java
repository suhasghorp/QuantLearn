package com.quantlearn.ircurves;

import com.quantlearn.enums.BusinessDayAdjustment;
import com.quantlearn.enums.CurveInstrumentType;
import com.quantlearn.enums.DC;
import com.quantlearn.schedule.BusDate;
import com.quantlearn.schedule.Period;
import com.quantlearn.utils.FormulaUtil;

public class Deposit implements IRCurveInstrument {
	public BusDate settlementDate;     // reference date, will be used in curve building
    public BusDate maturityDate;     // end date of building block
    public Period Tenor;     // tenor of building block
    public double rateValue; // rate value of building block    
    public CurveInstrumentType curveInstrumentType;  // building block type
    public DC dayCount; // day count
    public BusinessDayAdjustment busDayAdjPay;
    public String tenorString;
    
	public Deposit() {	}
	public Deposit(BusDate settlementDate, double rateValue, String tenorString) {
		this.settlementDate = settlementDate;            
        this.rateValue = rateValue;
        this.Tenor = new Period(tenorString);
        this.curveInstrumentType = CurveInstrumentType.DEPO;
		this.busDayAdjPay = BusinessDayAdjustment.ModifiedFollowing;
		this.dayCount = DC._Act_360;
		this.maturityDate = this.settlementDate.shiftPeriod(tenorString, this.busDayAdjPay, "ADD");
		this.tenorString = tenorString;
	}
	
	@Override
	public double getRateValue() {
		return rateValue;
	}
	@Override
	public void setRateValue(double rateValue) {
		this.rateValue = rateValue;
	}
	@Override
	public CurveInstrumentType getCurveInstrumentType() {
		return CurveInstrumentType.DEPO;
	}
	@Override
	public double getDiscountFactor() {
		double yf = settlementDate.getYearFraction(this.maturityDate, this.dayCount); 
        return FormulaUtil.DFsimple(yf, this.rateValue);
	}
	@Override
	public BusDate getMaturityDate() {
		return maturityDate;
	}
	@Override
	public void setMaturityDate(String tenorString) {
		this.maturityDate = settlementDate.shiftPeriod(tenorString, BusinessDayAdjustment.Unadjusted, "ADD");	
	}
	@Override
	public double getLastFromDate() {
		return this.settlementDate.getExcelSerial();
	}
	@Override
	public Deposit shiftUp1BP() {
		return new Deposit(this.settlementDate, this.rateValue + 0.0001, this.Tenor.getPeriodStringFormat());
	}
	@Override
	public Deposit shiftDown1BP() {
		return new Deposit(this.settlementDate, this.rateValue - 0.0001, this.Tenor.getPeriodStringFormat());
	}
	@Override
	public int compareTo(IRCurveInstrument arg0) {
		return 0;
	}
	@Override
	public String getTenorString() {
		return this.tenorString;
	}
}
