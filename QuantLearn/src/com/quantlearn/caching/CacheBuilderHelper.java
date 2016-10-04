package com.quantlearn.caching;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.quantlearn.schedule.BusDate;

public class CacheBuilderHelper {
	 
    public static void buildHolidayCache() {
    	String calendarID = (String)(CacheManager.getCache(CacheManager.CACHE_IDS.CALENDAR).getIfPresent("CALENDAR"));
    	
    	HashSet<LocalDate> h = new HashSet<LocalDate>(2000);
    	ImmutableSet<LocalDate> holidays = null;
    	
    	if (calendarID.contains("+")) {
			String[] ids = calendarID.split("\\+");				
			for (String region : ids) {
				h.addAll(HolidayCalendarUtils.generateHolidays(region));
			}			
		} else {
			h = HolidayCalendarUtils.generateHolidays(calendarID);			
		}	
    	if (h != null)
    		holidays = ImmutableSet.copyOf(h);
    	
    	LoadingCache<String, ImmutableSet<LocalDate>> holidayCache = CacheBuilder.newBuilder()
    			.build(
                    new CacheLoader() {
                        @Override
                        public Object load(Object key) throws Exception {
                            return null;
                        }
                    }); 
    	if (holidays != null) {
    		holidayCache.put(calendarID, holidays);
    		CacheManager.addCache(CacheManager.CACHE_IDS.HOLIDAYS, holidayCache);
    	}
    	/*if (holidayCalendarID.equals("UK+NYSE")) {
    		holidayCache.put(holidayCalendarID, holidays);
    		CacheManager.addCache(CacheManager.CACHE_IDS.UKNYSE, holidayCache);
    	} else if (holidayCalendarID.equals("UK")) {
    		holidayCache.put(holidayCalendarID, holidays);
    		CacheManager.addCache(CacheManager.CACHE_IDS.UK, holidayCache);
    	}*/
    }
    
    public static void buildUKHolidayCache() {
    	
    	String calendarID = "UK";    	
    	HashSet<LocalDate> h = new HashSet<LocalDate>(2000);
    	ImmutableSet<LocalDate> holidays = null;
    	h = HolidayCalendarUtils.generateHolidays(calendarID);		
	
    	if (h != null)
    		holidays = ImmutableSet.copyOf(h);
    	
    	LoadingCache<String, ImmutableSet<LocalDate>> holidayCache = CacheBuilder.newBuilder()
    			.build(
                    new CacheLoader() {
                        @Override
                        public Object load(Object key) throws Exception {
                            return null;
                        }
                    }); 
    	if (holidays != null) {
    		holidayCache.put(calendarID, holidays);
    		CacheManager.addCache(CacheManager.CACHE_IDS.HOLIDAYS, holidayCache);
    	}
    	
    }
    
    public static void cacheRefDate(BusDate refDate) {
    	LoadingCache<String, BusDate> refDateCache = CacheBuilder.newBuilder()
    			.build(
                    new CacheLoader() {
                        @Override
                        public Object load(Object key) throws Exception {
                            return null;
                        }
                    });               
    	refDateCache.put("REF_DATE", refDate);
    	CacheManager.addCache(CacheManager.CACHE_IDS.REFDATE, refDateCache);
    }
    
    public static void cacheCalendarName(String calendar) {
    	LoadingCache<String, String> refCalCache = CacheBuilder.newBuilder()
    			.build(
                    new CacheLoader() {
                        @Override
                        public Object load(Object key) throws Exception {
                            return null;
                        }
                    });               
    	refCalCache.put("CALENDAR", calendar);
    	CacheManager.addCache(CacheManager.CACHE_IDS.CALENDAR, refCalCache);
    	buildHolidayCache();
    	buildUKHolidayCache(); //for libor reset dates
    }
}
