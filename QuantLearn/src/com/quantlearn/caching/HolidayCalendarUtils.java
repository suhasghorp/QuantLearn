package com.quantlearn.caching;

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.temporal.TemporalAdjusters.dayOfWeekInMonth;
import static java.time.temporal.TemporalAdjusters.firstInMonth;
import static java.time.temporal.TemporalAdjusters.lastInMonth;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class HolidayCalendarUtils {
	public static HashSet<LocalDate> generateHolidays(String id){
		HashSet<LocalDate> holidays = null;
		if ("UK".equals(id)) {
			holidays = generateLondon();
		} else if ("USGS".equals(id)) {
			holidays = generateUsGovtSecurities();
		} else if ("USNY".equals(id)) {
			holidays = generateUsNewYork();
		} else if ("NYFD".equals(id)) {
			holidays = generateNewYorkFed();
		} else if ("NYSE".equals(id)) {
			holidays = generateNewYorkStockExchange();
		} else if ("NONE".equals(id)) {
			//DO NOTHING
		} else {
			throw new NotImplementedException();
		}
		return holidays;
	}

	private static LocalDate first(int year, int month) {
		return LocalDate.of(year, month, 1);
	}

	private static LocalDate bumpToMon(LocalDate date) {
		if (date.getDayOfWeek() == SATURDAY) {
			return date.plusDays(2);
		} else if (date.getDayOfWeek() == SUNDAY) {
			return date.plusDays(1);
		}
		return date;
	}

	private static LocalDate bumpSunToMon(LocalDate date) {
		if (date.getDayOfWeek() == SUNDAY) {
			return date.plusDays(1);
		}
		return date;
	}

	private static LocalDate bumpToFriOrMon(LocalDate date) {
		if (date.getDayOfWeek() == SATURDAY) {
			return date.minusDays(1);
		} else if (date.getDayOfWeek() == SUNDAY) {
			return date.plusDays(1);
		}
		return date;
	}

	static LocalDate easter(int year) {
		int a = year % 19;
		int b = year / 100;
		int c = year % 100;
		int d = b / 4;
		int e = b % 4;
		int f = (b + 8) / 25;
		int g = (b - f + 1) / 3;
		int h = (19 * a + b - d - g + 15) % 30;
		int i = c / 4;
		int k = c % 4;
		int l = (32 + 2 * e + 2 * i - h - k) % 7;
		int m = (a + 11 * h + 22 * l) / 451;
		int month = (h + l - 7 * m + 114) / 31;
		int day = ((h + l - 7 * m + 114) % 31) + 1;
		return LocalDate.of(year, month, day);
	}

	private static LocalDate date(int year, int month, int day) {
		return LocalDate.of(year, month, day);
	}

	private static LocalDate christmas(int year) {
		LocalDate base = LocalDate.of(year, 12, 25);
		if (base.getDayOfWeek() == SATURDAY || base.getDayOfWeek() == SUNDAY) {
			return LocalDate.of(year, 12, 27);
		}
		return base;
	}

	private static LocalDate boxingDay(int year) {
		LocalDate base = LocalDate.of(year, 12, 26);
		if (base.getDayOfWeek() == SATURDAY || base.getDayOfWeek() == SUNDAY) {
			return LocalDate.of(year, 12, 28);
		}
		return base;
	}

	private static void removeSatSun(HashSet<LocalDate> holidays) {
		holidays.removeIf(date -> date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY);
	}

	static HashSet<LocalDate> generateLondon() {
		HashSet<LocalDate> holidays = new HashSet<LocalDate>(2000);
		for (int year = 1950; year <= 2099; year++) {
			// new year
			if (year >= 1974) {
				holidays.add(bumpToMon(first(year, 1)));
			}
			// easter
			holidays.add(easter(year).minusDays(2));
			holidays.add(easter(year).plusDays(1));
			// early May
			if (year == 1995) {
				// ve day
				holidays.add(date(1995, 5, 8));
			} else if (year >= 1978) {
				holidays.add(first(year, 5).with(firstInMonth(MONDAY)));
			}
			// spring
			if (year == 2002) {
				// golden jubilee
				holidays.add(date(2002, 6, 3));
				holidays.add(date(2002, 6, 4));
			} else if (year == 2012) {
				// diamond jubilee
				holidays.add(date(2012, 6, 4));
				holidays.add(date(2012, 6, 5));
			} else if (year == 1967 || year == 1970) {
				holidays.add(first(year, 5).with(lastInMonth(MONDAY)));
			} else if (year < 1971) {
				// whitsun
				holidays.add(easter(year).plusDays(50));
			} else {
				holidays.add(first(year, 5).with(lastInMonth(MONDAY)));
			}
			// summer
			if (year < 1965) {
				holidays.add(first(year, 8).with(firstInMonth(MONDAY)));
			} else if (year < 1971) {
				holidays.add(first(year, 8).with(lastInMonth(SATURDAY)).plusDays(2));
			} else {
				holidays.add(first(year, 8).with(lastInMonth(MONDAY)));
			}
			// christmas
			holidays.add(christmas(year));
			holidays.add(boxingDay(year));
		}
		holidays.add(date(2011, 4, 29)); // royal wedding
		holidays.add(date(1999, 12, 31)); // millennium
		removeSatSun(holidays);
		return holidays;
	}

	private static void usCommon(HashSet<LocalDate> holidays, int year, boolean bumpBack, boolean columbusVeteran,
			int mlkStartYear) {
		// new year, adjusted if Sunday
		holidays.add(bumpSunToMon(date(year, 1, 1)));
		// martin luther king
		if (year >= mlkStartYear) {
			holidays.add(date(year, 1, 1).with(dayOfWeekInMonth(3, MONDAY)));
		}
		// washington
		if (year < 1971) {
			holidays.add(bumpSunToMon(date(year, 2, 22)));
		} else {
			holidays.add(date(year, 2, 1).with(dayOfWeekInMonth(3, MONDAY)));
		}
		// memorial
		if (year < 1971) {
			holidays.add(bumpSunToMon(date(year, 5, 30)));
		} else {
			holidays.add(date(year, 5, 1).with(lastInMonth(MONDAY)));
		}
		// labor day
		holidays.add(date(year, 9, 1).with(firstInMonth(MONDAY)));
		// columbus day
		if (columbusVeteran) {
			if (year < 1971) {
				holidays.add(bumpSunToMon(date(year, 10, 12)));
			} else {
				holidays.add(date(year, 10, 1).with(dayOfWeekInMonth(2, MONDAY)));
			}
		}
		// veterans day
		if (columbusVeteran) {
			if (year >= 1971 && year < 1978) {
				holidays.add(date(year, 10, 1).with(dayOfWeekInMonth(4, MONDAY)));
			} else {
				holidays.add(bumpSunToMon(date(year, 11, 11)));
			}
		}
		// thanksgiving
		holidays.add(date(year, 11, 1).with(dayOfWeekInMonth(4, THURSDAY)));
		// independence day & christmas day
		if (bumpBack) {
			holidays.add(bumpToFriOrMon(date(year, 7, 4)));
			holidays.add(bumpToFriOrMon(date(year, 12, 25)));
		} else {
			holidays.add(bumpSunToMon(date(year, 7, 4)));
			holidays.add(bumpSunToMon(date(year, 12, 25)));
		}
	}

	// generate USGS
	// http://www.sifma.org/services/holiday-schedule/
	static HashSet<LocalDate> generateUsGovtSecurities() {
		HashSet<LocalDate> holidays = new HashSet<>(2000);
		for (int year = 1950; year <= 2099; year++) {
			usCommon(holidays, year, true, true, 1986);
			// good friday, in 1999/2007 only a partial holiday
			holidays.add(easter(year).minusDays(2));
			// hurricane sandy
			if (year == 2012) {
				holidays.add(date(year, 10, 30));
			}
		}
		removeSatSun(holidays);
		return holidays;
	}

	static HashSet<LocalDate> generateUsNewYork() {
		HashSet<LocalDate> holidays = new HashSet<>(2000);
		for (int year = 1950; year <= 2099; year++) {
			usCommon(holidays, year, false, true, 1986);
		}
		removeSatSun(holidays);
		return holidays;
	}

	static HashSet<LocalDate> generateNewYorkFed() {
		HashSet<LocalDate> holidays = new HashSet<>(2000);
		for (int year = 1950; year <= 2099; year++) {
			usCommon(holidays, year, false, true, 1986);
		}
		removeSatSun(holidays);
		return holidays;
	}

	static HashSet<LocalDate> generateNewYorkStockExchange() {
		HashSet<LocalDate> holidays = new HashSet<>(2000);
		for (int year = 1950; year <= 2099; year++) {
			usCommon(holidays, year, true, false, 1998);
			// good friday
			holidays.add(easter(year).minusDays(2));
		}
		// Lincoln day 1896-1953
		// Columbus day 1909-1953
		// Veterans day 1934-1953
		for (int i = 1950; i <= 1953; i++) {
			holidays.add(date(i, 2, 12));
			holidays.add(date(i, 10, 12));
			holidays.add(date(i, 11, 11));
		}
		// election day, Tue after first Monday of November
		for (int i = 1950; i <= 1968; i++) {
			holidays.add(date(i, 11, 1).with(TemporalAdjusters.nextOrSame(MONDAY)).plusDays(1));
		}
		holidays.add(date(1972, 11, 7));
		holidays.add(date(1976, 11, 2));
		holidays.add(date(1980, 11, 4));
		// special days
		holidays.add(date(1955, 12, 24)); // Christmas Eve
		holidays.add(date(1956, 12, 24)); // Christmas Eve
		holidays.add(date(1958, 12, 26)); // Day after Christmas
		holidays.add(date(1961, 5, 29)); // Decoration day
		holidays.add(date(1963, 11, 25)); // Death of John F Kennedy
		holidays.add(date(1965, 12, 24)); // Christmas Eve
		holidays.add(date(1968, 2, 12)); // Lincoln birthday
		holidays.add(date(1968, 4, 9)); // Death of Martin Luther King
		holidays.add(date(1968, 6, 12)); // Paperwork crisis
		holidays.add(date(1968, 6, 19)); // Paperwork crisis
		holidays.add(date(1968, 6, 26)); // Paperwork crisis
		holidays.add(date(1968, 7, 3)); // Paperwork crisis
		holidays.add(date(1968, 7, 5)); // Day after independence
		holidays.add(date(1968, 7, 10)); // Paperwork crisis
		holidays.add(date(1968, 7, 17)); // Paperwork crisis
		holidays.add(date(1968, 7, 24)); // Paperwork crisis
		holidays.add(date(1968, 7, 31)); // Paperwork crisis
		holidays.add(date(1968, 8, 7)); // Paperwork crisis
		holidays.add(date(1968, 8, 13)); // Paperwork crisis
		holidays.add(date(1968, 8, 21)); // Paperwork crisis
		holidays.add(date(1968, 8, 28)); // Paperwork crisis
		holidays.add(date(1968, 9, 4)); // Paperwork crisis
		holidays.add(date(1968, 9, 11)); // Paperwork crisis
		holidays.add(date(1968, 9, 18)); // Paperwork crisis
		holidays.add(date(1968, 9, 25)); // Paperwork crisis
		holidays.add(date(1968, 10, 2)); // Paperwork crisis
		holidays.add(date(1968, 10, 9)); // Paperwork crisis
		holidays.add(date(1968, 10, 16)); // Paperwork crisis
		holidays.add(date(1968, 10, 23)); // Paperwork crisis
		holidays.add(date(1968, 10, 30)); // Paperwork crisis
		holidays.add(date(1968, 11, 6)); // Paperwork crisis
		holidays.add(date(1968, 11, 13)); // Paperwork crisis
		holidays.add(date(1968, 11, 20)); // Paperwork crisis
		holidays.add(date(1968, 11, 27)); // Paperwork crisis
		holidays.add(date(1968, 12, 4)); // Paperwork crisis
		holidays.add(date(1968, 12, 11)); // Paperwork crisis
		holidays.add(date(1968, 12, 18)); // Paperwork crisis
		holidays.add(date(1968, 12, 25)); // Paperwork crisis
		holidays.add(date(1968, 12, 31)); // Paperwork crisis
		holidays.add(date(1969, 2, 10)); // Snow
		holidays.add(date(1969, 3, 31)); // Death of Dwight Eisenhower
		holidays.add(date(1969, 7, 21)); // Lunar exploration
		holidays.add(date(1972, 12, 28)); // Death of Harry Truman
		holidays.add(date(1973, 1, 25)); // Death of Lyndon Johnson
		holidays.add(date(1977, 7, 14)); // Blackout
		holidays.add(date(1985, 9, 27)); // Hurricane Gloria
		holidays.add(date(1994, 4, 27)); // Death of Richard Nixon
		holidays.add(date(2001, 9, 11)); // 9/11 attack
		holidays.add(date(2001, 9, 12)); // 9/11 attack
		holidays.add(date(2001, 9, 13)); // 9/11 attack
		holidays.add(date(2001, 9, 14)); // 9/11 attack
		holidays.add(date(2004, 6, 11)); // Death of Ronald Reagan
		holidays.add(date(2007, 1, 2)); // Death of Gerald Ford
		holidays.add(date(2012, 10, 30)); // Hurricane Sandy
		removeSatSun(holidays);
		return holidays;
	}
}
