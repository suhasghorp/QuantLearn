package com.quantlearn.schedule;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.stream.Collectors;

public class DropDateSchedule {
	
	private boolean adjusted; // true = adjust for business days
	private boolean arrears; // true = fixingDays will be counted from at the end of period
    private boolean firstShortPeriod; // true = the short period is at beginning: rolling from the end (backward) 
    private int fixingDays; // number of days before for taking the fixing
    private String holidayCalendarId;
	private ArrayList<BusDate> dates; 
    private ArrayList<BusDate> adjustedDates; 
    
    public DropDateSchedule(LocalDate startDate, LocalDate endDate, int paymentPerYear, boolean firstShortPeriod,
        boolean adjusted, boolean arrears, int fixingDays, String holidayCalendarId) {            
        this.adjusted = adjusted;
        this.arrears = arrears;
        this.fixingDays = fixingDays;
        this.holidayCalendarId = holidayCalendarId;
        this.dates = createDateMatrix(startDate, endDate, paymentPerYear, firstShortPeriod);
        this.adjustedDates = this.adjust();
        
    }
    
    private ArrayList<BusDate> createDateMatrix(LocalDate startDate, LocalDate endDate, int paymentPerYear, boolean firstShortPeriod){
    	int monthsBetween = (int)(ChronoUnit.MONTHS.between(startDate, endDate));
    	int remainder = 0;
    	int i = 0;
    	
    	int freq = (int) 12/paymentPerYear;
    	if ((monthsBetween % freq) != 0) remainder = 1;
    	monthsBetween = monthsBetween / freq;
    	int n = remainder + monthsBetween + 1;
    	ArrayList<BusDate> dates = new ArrayList<BusDate>(n);
    	dates.add(new BusDate(startDate));
    	
    	
		if (firstShortPeriod) {
			for (i = 1; i < n; i++) {
				dates.add(new BusDate(endDate.plusMonths(freq *(-n + i +1))));
			}
		} else {
			for (i = 1; i < n; i++) {
				dates.add(new BusDate(startDate.plusMonths(freq * i)));
			}
			dates.add(dates.size(), new BusDate(endDate));
		}
		
		return dates;    	
    }
    
    private ArrayList<BusDate> adjust() {
    	return new ArrayList<BusDate>(dates.stream().map(d -> d.getModFoll()).collect(Collectors.toList()));
    }
    
    private ArrayList<BusDate> getFixingDates() {
    	ArrayList<BusDate> temp = null;
    	
    	if (this.adjusted) {
    		temp = new ArrayList(adjustedDates.subList(0, adjustedDates.size()-1));
    	} else {
    		temp = new ArrayList(dates.subList(0, dates.size()-1));
    	}
    	ArrayList<BusDate> ret = new ArrayList<BusDate>();
    	if (!this.arrears) {
    		for (int i = 0; i < temp.size(); i++) {
                ret.add(temp.get(i).shift(this.fixingDays));
            }
    	} else {
    		 for (int i = 0; i < temp.size(); i++) {
                 ret.add(temp.get(i + 1).shift(this.fixingDays));
             }
    	}
    	return ret;    	
    }
    
    private ArrayList<BusDate> getFromDates() {
    	if (this.adjusted) {
    		return new ArrayList(adjustedDates.subList(0, adjustedDates.size()-1));
    	} else {
    		return new ArrayList(dates.subList(0, dates.size()-1));
    	}
    	
    }
    private ArrayList<BusDate> getToDates() {
    	ArrayList<BusDate> ret = new ArrayList<BusDate>();
    	for (int i = 0; i < dates.size() - 1; i++) {
    		if (adjusted) {
    			ret.add(adjustedDates.get(i+1));
    		} else {
    			ret.add(dates.get(i+1));
    		}            
        }
    	return ret;
    }
    private ArrayList<BusDate> getPaymentDates() {
    	
    	ArrayList<BusDate> ret = new ArrayList<BusDate>();
    	for (int i = 0; i < dates.size() - 1; i++) {
            ret.add(adjustedDates.get(i+1));
        }
    	return ret;
    }
    public void printLongSchedule() {
		ArrayList<BusDate> fixingDates = this.getFixingDates();
		ArrayList<BusDate> fromDates = this.getFromDates();
		ArrayList<BusDate> toDates = this.getToDates();
		ArrayList<BusDate> paymentDates = this.getPaymentDates();
		int n = fixingDates.size();
		System.out.println("\nFixing	\t\t From  \t\t  To \t\t  PayDate");
		for(int i = 0; i < n; i++) {
			System.out.println(	fixingDates.get(i).toString() + "____" +
								fromDates.get(i).toString() + "____" + 
								toDates.get(i).toString() + "____" + 
								paymentDates.get(i).toString());
		}
	}
}
