package com.quantlearn.curves;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

import org.apache.commons.math3.analysis.UnivariateFunction;

import com.quantlearn.enums.BusinessDayAdjustment;
import com.quantlearn.enums.CurveInstrumentType;
import com.quantlearn.enums.DC;
import com.quantlearn.schedule.BusDate;
import com.quantlearn.schedule.Period;

public class EuroDollarFuture implements CurveInstrument {
	public BusDate endDate;     // end date of building block
    public Period Tenor;     // tenor of building block
    public double rateValue; // rate value of building block    
    public CurveInstrumentType curveInstrumentType;  // building block type
    public DC dayCount; // day count
    public BusinessDayAdjustment busDayAdjPay;
    public BusDate IMMDate;
    public double price;
    public UnivariateFunction interp;
    public int month;
    public int year;
    public String tenorString;
    
	public EuroDollarFuture() {	}
	public EuroDollarFuture(double price, int month, int year, UnivariateFunction interp) {
		this.price = price;
        this.rateValue = (100.0 - price)/100.0;
        this.tenorString = YearMonth.of(year,month).getMonth().getDisplayName(TextStyle.SHORT, Locale.US) + "-" + YearMonth.of(year,month).getYear();
        this.Tenor = new Period("3M");
        this.curveInstrumentType = CurveInstrumentType.EDF;
		this.busDayAdjPay = BusinessDayAdjustment.ModifiedFollowing;
		this.dayCount = DC._Act_360;
		this.month = month;
		this.year = year;
		this.IMMDate = BusDate.getIMMDate(month, year);
		//endDate is really the notional repayment date
		this.endDate = new BusDate(this.IMMDate.getDate().plusMonths(3));
		this.interp = interp;
	}
	public EuroDollarFuture(double price, int month, int year) {
		this(price,month,year,null);
	}
	
	@Override
	public double getRateValue() {
		return getDiscountFactor();
	}
	public double calcPrice() {
		return 100 - (this.rateValue * 100);
	}
	public double getEndSerial() {
		return endDate.getExcelSerial();
	}
	public Period getPeriod() {
		return Tenor;
	}
	/*public double getYF() {
		return this.refDate.YF_ACT_360(this.endDate);
	}*/
	@Override
	public CurveInstrumentType getCurveInstrumentType() {
		return CurveInstrumentType.EDF;
	}
	@Override
	public double getDiscountFactor() {
		double term = (int)this.IMMDate.D_EF(this.endDate);
		double tempDF = 1 / (1 + ((100.0 - this.price) / 100.0) * term / 360);
		if (interp != null) {
			return interp.value(this.IMMDate.getExcelSerial()) * tempDF;
		} else return tempDF;
	}
	@Override
	public BusDate getMaturityDate() {
		return this.endDate;
	}
	@Override
	public double getLastFromDate() {
		return this.IMMDate.getExcelSerial();
	}
	@Override
	public EuroDollarFuture shiftUp1BP() {
		//EDF prices are opposite
		return new EuroDollarFuture(this.price - 0.0001, this.month, this.year, this.interp);
	}
	@Override
	public EuroDollarFuture shiftDown1BP() {
		return new EuroDollarFuture(this.price + 0.0001, this.month, this.year, this.interp);
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
