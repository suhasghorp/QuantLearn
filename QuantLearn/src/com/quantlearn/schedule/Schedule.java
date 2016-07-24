package com.quantlearn.schedule;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Iterables;
import com.quantlearn.enums.BusinessDayAdjustment;
import com.quantlearn.enums.DC;
import com.quantlearn.enums.Rule;
import com.quantlearn.utils.StreamUtil;
import com.quantlearn.utils.StreamUtil.Pair;

import lombok.Builder;

@Builder
public class Schedule {
	
	public ArrayList<BusDate> fromDates;
	public ArrayList<BusDate> toDates;
	public ArrayList<BusDate> payDates;
	public ArrayList<BusDate> resetDates;
	
	private BusDate startDate;
	private BusDate endDate;
	private String stringTenor;
	private Rule generatorRule;
	private BusinessDayAdjustment busDayAdjRolls;
	private BusinessDayAdjustment busDayAdjPay;
	private String payLag;	
	private String resetLag = "0D";
	
	public void buildSchedule() {
		LocalDate dt = LocalDate.of(2016,7,15).plusYears(2);
		ArrayList<BusDate> dates = new ArrayList<BusDate>();
		switch(generatorRule) {
		case Forward:
			dates.add(startDate);
			while (endDate.getDate().isAfter(Iterables.getLast(dates).getDate())) {
				BusDate nextDate = Iterables.getLast(dates).shiftPeriod(stringTenor, BusinessDayAdjustment.Unadjusted, "ADD");	
				dates.add(nextDate);
			}
			if (Iterables.getLast(dates).getDate().isAfter(endDate.getDate())) {
				dates.remove(Iterables.getLast(dates));
				dates.add(dates.size(), endDate);
			}
			break;
		case Backward:
			dates.add(endDate);
			Period p = new Period(stringTenor);
			int i = 1;
			while (startDate.getDate().isBefore(Iterables.getLast(dates).getDate())) {
                Period pp = new Period(p.tenor * i, p.tenorType);
                dates.add(endDate.shiftPeriod(pp.getPeriodStringFormat(), BusinessDayAdjustment.Unadjusted, "SUBSTRACT"));
                i++;
            }
			if (Iterables.getLast(dates).getDate().isBefore(startDate.getDate())) { // before start date
				dates.remove(Iterables.getLast(dates));
				dates.add(dates.size(), startDate);
			}
			Collections.reverse(dates);;
			break;
		}
		dates = BusDate.getBusDayAdjust(dates, busDayAdjRolls, "ADD");
		fromDates = new ArrayList<BusDate>(dates.subList(0, dates.size()-1));
		toDates = new ArrayList<BusDate>(dates.subList(1, dates.size()));
		payDates = new ArrayList<BusDate>(toDates.stream().map(d -> d.shiftPeriod(payLag, busDayAdjPay, "ADD")).collect(Collectors.toList()));
		payDates.forEach(d -> d.getBusDayAdjust(busDayAdjPay));
		resetDates = new ArrayList<BusDate>(fromDates.stream().map(d -> d.applyResetLag(resetLag)).collect(Collectors.toList()));
	}
	
	public ArrayList<Double> getYF(DC dayCountConv){
		Stream<Pair<BusDate,BusDate>> zip = StreamUtil.zip(fromDates.stream(), toDates.stream());
        return new ArrayList<Double>(zip.map(p -> p._1.getYearFraction(p._2, dayCountConv)).collect(Collectors.toList()));
	}
	
	public double[] getYFArray(DC dayCountConv){
		List<Double> temp = getYF(dayCountConv);
		Double[] boxed = temp.toArray(new Double[temp.size()]);
		return Stream.of(boxed).mapToDouble(Double::doubleValue).toArray();
	}
	
	public double[] getPayDatesArray() {
		List<Double> temp = payDates.stream().map(d -> d.getExcelSerial()).collect(Collectors.toList());
		Double[] boxed = temp.toArray(new Double[temp.size()]);
		return Stream.of(boxed).mapToDouble(Double::doubleValue).toArray();
	}
	public double[] getFromDatesArray() {
		List<Double> temp = fromDates.stream().map(f -> f.getExcelSerial()).collect(Collectors.toList());
		Double[] boxed = temp.toArray(new Double[temp.size()]);
		return Stream.of(boxed).mapToDouble(Double::doubleValue).toArray();
	}
	
	public void printSchedule() {
		SimpleDateFormat formatter = new SimpleDateFormat("ddd MM/dd/yyyy");
		int n = fromDates.size();
		System.out.println("\nResetDate	\t\t	From  \t\t  To \t\t  PayDate");
		for(int i = 0; i < n; i++) {
			System.out.println(	resetDates.get(i) + "____" +
								fromDates.get(i) + "____" + 
								toDates.get(i) + "____" + 
								payDates.get(i));
		}
	}
}
