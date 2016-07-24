package com.quantlearn.curves;

import com.quantlearn.enums.CurveInstrumentType;
import com.quantlearn.schedule.BusDate;
import com.quantlearn.schedule.Period;

public interface CurveInstrument extends Comparable<CurveInstrument>{
	CurveInstrumentType getCurveInstrumentType();
	double getDiscountFactor();
	double getRateValue();
	BusDate getMaturityDate();	
	double getLastFromDate();
	String getTenorString();
	CurveInstrument shiftUp1BP();
	CurveInstrument shiftDown1BP();
	
	@Override
	default int compareTo(CurveInstrument y) {
		double X = getMaturityDate().getExcelSerial();
        double Y = ((CurveInstrument)y).getMaturityDate().getExcelSerial();
        return Double.compare(X, Y);
	}

}
