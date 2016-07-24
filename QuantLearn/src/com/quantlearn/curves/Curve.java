package com.quantlearn.curves;

import com.quantlearn.schedule.BusDate;

public interface Curve {
	double getDiscFactorForDate(BusDate d);
}
