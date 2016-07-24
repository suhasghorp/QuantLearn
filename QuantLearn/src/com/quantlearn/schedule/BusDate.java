package com.quantlearn.schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.quantlearn.caching.CacheManager;
import com.quantlearn.enums.BusinessDayAdjustment;
import com.quantlearn.enums.DC;
import com.quantlearn.enums.TenorType;

public class BusDate {

	private LocalDate theDate;
	private LocalDate startDate = LocalDate.of(1899, 12, 30);

	public BusDate(final int year, final int month, final int day) {
		theDate = LocalDate.of(year, month, day);
	}

	public BusDate(final LocalDate date) {
		theDate = date;
	}
	
	public BusDate() {
		theDate = LocalDate.now();
	}


	public BusDate(final double excelSerial) {
		theDate = startDate.plusDays((long) (excelSerial));
	}

	public double getExcelSerial() {
		return ChronoUnit.DAYS.between(startDate, theDate);
	}
	
	public LocalDate getDate() {
		return theDate;
	}
	@Override
	public String toString() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
		return this.getDate().format(formatter);
	}
	
	public double getDaysBetween(BusDate date2) {
		return ChronoUnit.DAYS.between(theDate, date2.getDate());
	}

	public double getYearFraction(BusDate d2, DC dayCount) {
        double outPut = 0;
        switch (dayCount) {
            case _30_360:
                outPut = this.YF_30_360_BB(d2);
                break;
            case _Act_360:
                outPut = this.YF_MM(d2);
                break;
            case _Act_365:
                outPut = this.YF_365(d2); ;
                break;
          
            default: break;
        }

        return outPut;	  
    }
	public double D_EF(final BusDate date2) {
		return date2.getExcelSerial() - this.getExcelSerial();
	}

	public double YF_ACT_360(final BusDate date2) {
		return (date2.getExcelSerial() - this.getExcelSerial()) / 360.0;
	}
	public double YF_MM(final BusDate date2) {
		return (date2.getExcelSerial() - this.getExcelSerial())/360.0;
	}
	public double YF_365(final BusDate date2) {
		return (date2.getExcelSerial() - this.getExcelSerial())/365.0;
	}

	public double YF_ACT_365(final BusDate date2) {
		return (date2.getExcelSerial() - this.getExcelSerial()) / 365.0;
	}

	public double YF_30_360_BB(final BusDate date2) {
		LocalDate theDate2 = date2.theDate;
		int D1 = theDate.getDayOfMonth();
		int D2 = theDate2.getDayOfMonth();
		int M1 = theDate.getMonthValue();
		int M2 = theDate2.getMonthValue();
		int Y1 = theDate.getYear();
		int Y2 = theDate2.getYear();

		if (D1 == 31)
			D1 = 30;
		if ((D2 == 31) && (D1 > 29))
			D2 = 30;

		return (double) (360 * (Y2 - Y1) + 30 * (M2 - M1) + (D2 - D1)) / 360;
	}

	public double YF_BB(final BusDate date2) {
		LocalDate theDate2 = date2.theDate;
		if (theDate2.getDayOfMonth() == 31 && (theDate.getDayOfMonth() != 30 && theDate.getDayOfMonth() != 31)) {
			return ((double) (theDate2.getDayOfMonth() - theDate.getDayOfMonth())
					+ 30 * (theDate2.getMonthValue() - theDate.getMonthValue())
					+ 360 * (theDate2.getYear() - theDate.getYear())) / 360;
		}
		;

		if (theDate.getMonthValue() == 2 && (theDate2.plusDays(1).getMonthValue() == 3)) {
			return (double) ((theDate2.getDayOfMonth() + theDate.lengthOfMonth() - theDate.getDayOfMonth())
					+ 360 * (theDate2.getYear() - theDate.getYear())
					+ 30 * (theDate2.getMonthValue() - theDate.getMonthValue() - 1)) / 360;
		}
		;
		double a = (30 - theDate.getDayOfMonth() > 0) ? 30 - theDate.getDayOfMonth() : 0;
		double b = (theDate2.getDayOfMonth() < 30) ? theDate2.getDayOfMonth() : 30;
		return (double) (a + b + 360 * (theDate2.getYear() - theDate.getYear())
				+ 30 * (theDate2.getMonthValue() - theDate.getMonthValue() - 1)) / 360;
	}

	public double YF_ACT_ACT(final BusDate date2) {

		LocalDate theDate2 = date2.theDate;
		double d1 = 365, d2 = 365;
		int Y1 = theDate.getYear(), Y2 = theDate2.getYear();

		if (theDate.isLeapYear())
			d1 = 366;
		if (theDate2.isLeapYear())
			d2 = 366;

		int diff = Y2 - Y1;

		if (diff == 0) {
			return (date2.getExcelSerial() - this.getExcelSerial()) / d1;
		} else {
			BusDate end1 = new BusDate(Y1, 12, 31);
			BusDate end2 = new BusDate(Y2 - 1, 12, 31);

			return (end1.getExcelSerial() - this.getExcelSerial()) / d1 + (diff - 1)
					+ (date2.getExcelSerial() - end2.getExcelSerial()) / d2;
		}
	}
	
	public boolean isUKHoliday(LocalDate d) {
		ImmutableSet<LocalDate> holidays= (ImmutableSet<LocalDate>) CacheManager.getCache(CacheManager.CACHE_IDS.UK).getIfPresent("UK");
		boolean hol =  (d.getDayOfWeek() == DayOfWeek.SUNDAY || d.getDayOfWeek() == DayOfWeek.SATURDAY
				|| holidays.contains(d));
		if (hol) {
			System.out.println(d + " was UK holiday/Sat/Sun");
		}
		return hol;
	}

	public boolean isHoliday(LocalDate d) {
		ImmutableSet<LocalDate> holidays= (ImmutableSet<LocalDate>) CacheManager.getCache(CacheManager.CACHE_IDS.UKNYSE).getIfPresent("UK+NYSE");
		boolean hol =  (d.getDayOfWeek() == DayOfWeek.SUNDAY || d.getDayOfWeek() == DayOfWeek.SATURDAY
				|| holidays.contains(d));
		if (hol) {
			System.out.println(d + " was holiday/Sat/Sun");
		}
		return hol;
	}

	public BusDate shift(int amount) {
		LocalDate adjusted = this.theDate;
		int iterations = Math.abs(amount);
		int increment = 0;
		if (amount > 0) {
			increment = 1;
		} else {
			increment = -1;
		}
		for (int i=0; i < iterations;i++) {
			adjusted = adjusted.plusDays(increment);
			//if( this.isHoliday(adjusted))
				//i-=1;
		}	
		return new BusDate(adjusted);
	}

	public LocalDate next(LocalDate dd, String holidayCalendarId) {
		LocalDate nextDate = dd.plusDays(1);
		return nextDate;
	}

	public LocalDate previous(LocalDate dd, String holidayCalendarId) {
		LocalDate previous = dd.plusDays(-1);
		return previous;
	}
	
	public BusDate getBusDayAdjust(BusinessDayAdjustment conv) {
        switch (conv) {
            case Following :
                return getFollowing();
                
            case ModifiedFollowing:
                return getModFoll();
                
            case Preceding:
                return getPreceding();
                
            case Unadjusted:
                return getUnadjusted();
                
            default: 
                break;
        }
        return this;
    }
	
	public static ArrayList<BusDate> getBusDayAdjust(ArrayList<BusDate> dates, BusinessDayAdjustment conv, String direction){
		return new ArrayList<BusDate>(dates.stream().map(d -> d.getBusDayAdjust(conv)).collect(Collectors.toList()));
	}

	public BusDate getModFoll() {
		ImmutableSet<LocalDate> holidays= (ImmutableSet<LocalDate>) CacheManager.getCache(CacheManager.CACHE_IDS.UKNYSE).getIfPresent("UK+NYSE");
		if (theDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
			if (theDate.getMonthValue() == theDate.plusDays(1).getMonthValue()) {
				return shift(1);
			} else {
				return shift(-2);
			}
		} else if (theDate.getDayOfWeek() == DayOfWeek.SATURDAY) {
			if (theDate.getMonthValue() == theDate.plusDays(2).getMonthValue()) {
				return shift(2);
			} else {
				return shift(-1);
			}
		} else if (holidays.contains(theDate)) {
			BusDate temp = new BusDate();
			if (theDate.getMonthValue() == theDate.plusDays(1).getMonthValue()) {
				temp = shift(1);
			} else {
				temp = shift(-1);
			}
			return temp.getModFoll();
		} else
			return new BusDate(this.getDate());
	}

	public BusDate getFollowing() {
		if (theDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
			return new BusDate(theDate.plusDays(1));
		}
		if (theDate.getDayOfWeek() == DayOfWeek.SATURDAY) {
			return new BusDate(theDate.plusDays(2));
		} else
			return new BusDate(this.getDate());
	}
	
	public static BusDate getIMMDate(int month, int year) {
		LocalDate d = LocalDate.of(year,  month, 1).minusDays(1);
		int weds = 0;
		while(weds < 3) {
			d = d.plusDays(1);
			if (d.getDayOfWeek() == DayOfWeek.WEDNESDAY) {
				weds++;
			}
		}
		return new BusDate(d);
	}
	
	public BusDate getPreceding() {
        
        if (theDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
        	return new BusDate(theDate.plusDays(-2));            
        }
        if (theDate.getDayOfWeek() == DayOfWeek.SATURDAY) {
           	return new BusDate(theDate.plusDays(-1));         
        }
        else return new BusDate(this.getDate());
    }
	
	public BusDate getUnadjusted() {
        return new BusDate(this.getDate());
    }
	
	public BusDate applyResetLag(String lag) {
		Period p = new Period(lag);
		int tenor = p.tenor;
        TenorType T = p.tenorType;
        LocalDate adjusted = this.theDate;
		int iterations = Math.abs(tenor);
		int decrement = 0;
		if (tenor > 0) {
			decrement = -1;
		}
		for (int i=0; i < iterations;i++) {
			adjusted = adjusted.plusDays(decrement);
			if( this.isUKHoliday(adjusted))
				i-=1;
		}	
		return new BusDate(adjusted);
	}
	
	public BusDate shiftPeriod(String period, BusinessDayAdjustment busDayAdj, String direction) {
		
		Period p = new Period(period);
		int tenor = p.tenor;
        TenorType T = p.tenorType;
        BusDate outPut = new BusDate();        
        if (direction == "SUBSTRACT")
        	tenor = tenor * -1;
		
        switch (T)
		{
			case D:
				outPut = new BusDate(this.getDate().plusDays(tenor));
				break;
			case W:
				outPut = new BusDate(this.getDate().plusDays(tenor * 7));
				break;
			case M:
				outPut = new BusDate(this.getDate().plusMonths(tenor));
				break;
			case Y:
				outPut = new BusDate(this.getDate().plusYears(tenor));
				break;				
			default:
				break;
		}
        switch (busDayAdj)
        {
        	case ModifiedFollowing:
        		outPut = outPut.getModFoll();
        		break;
        	case Following:
        		outPut = outPut.getFollowing();
        		break;
        	case Preceding:
        		outPut = outPut.getPreceding();
        		break;
        	case Unadjusted:
        		outPut = outPut.getUnadjusted();     
        		break;
        	default:
				break;
        		
        }        
		return outPut;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((theDate == null) ? 0 : theDate.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BusDate other = (BusDate) obj;
		if (theDate == null) {
			if (other.theDate != null)
				return false;
		} else if (!theDate.equals(other.theDate))
			return false;
		return true;
	}

}
