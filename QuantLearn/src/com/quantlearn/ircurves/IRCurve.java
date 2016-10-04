package com.quantlearn.ircurves;

import com.quantlearn.schedule.BusDate;

public interface IRCurve {
	double getDiscFactorForDate(BusDate d);
}
