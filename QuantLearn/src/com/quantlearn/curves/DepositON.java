package com.quantlearn.curves;

import com.quantlearn.enums.BusinessDayAdjustment;
import com.quantlearn.enums.CurveInstrumentType;
import com.quantlearn.enums.DC;
import com.quantlearn.schedule.BusDate;
import com.quantlearn.schedule.Period;
import com.quantlearn.utils.FormulaUtil;

public class DepositON implements CurveInstrument {
	public BusDate settlementDate;     // reference date, will be used in curve building
    public BusDate maturityDate;     // end date of building block
    public Period Tenor;     // tenor of building block
    public double rateValue; // rate value of building block    
    public CurveInstrumentType curveInstrumentType;  // building block type
    public DC dayCount; // day count
    public BusinessDayAdjustment busDayAdjPay;
    public String tenorString;
    
	public DepositON() {	}
	public DepositON(BusDate settlementDate, double rateValue, String tenorString) {
		this.settlementDate = settlementDate;            
        this.rateValue = rateValue;
        this.tenorString = tenorString;
        this.Tenor = new Period(tenorString);
        this.curveInstrumentType = CurveInstrumentType.DEPO;
		this.busDayAdjPay = BusinessDayAdjustment.ModifiedFollowing;
		this.dayCount = DC._Act_360;
		this.maturityDate = this.settlementDate.shiftPeriod(tenorString, this.busDayAdjPay, "ADD");
	}
	
	@Override
	public double getRateValue() {
		return rateValue;
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
	public double getLastFromDate() {
		return this.settlementDate.getExcelSerial();
	}
	@Override
	public DepositON shiftUp1BP() {
		return new DepositON(this.settlementDate, this.rateValue + 0.0001, this.Tenor.getPeriodStringFormat());
	}
	@Override
	public DepositON shiftDown1BP() {
		return new DepositON(this.settlementDate, this.rateValue - 0.0001, this.Tenor.getPeriodStringFormat());
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
