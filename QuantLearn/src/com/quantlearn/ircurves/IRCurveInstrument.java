package com.quantlearn.ircurves;

import com.quantlearn.enums.CurveInstrumentType;
import com.quantlearn.schedule.BusDate;
import com.quantlearn.schedule.Period;

public interface IRCurveInstrument extends Comparable<IRCurveInstrument>{
	CurveInstrumentType getCurveInstrumentType();
	double getDiscountFactor();
	double getRateValue();
	void setRateValue(double fixedRate);
	BusDate getMaturityDate();	
	void setMaturityDate(String tenorString);
	double getLastFromDate();
	String getTenorString();
	IRCurveInstrument shiftUp1BP();
	IRCurveInstrument shiftDown1BP();
	
	@Override
	default int compareTo(IRCurveInstrument y) {
		double X = getMaturityDate().getExcelSerial();
        double Y = ((IRCurveInstrument)y).getMaturityDate().getExcelSerial();
        return Double.compare(X, Y);
	}

}
