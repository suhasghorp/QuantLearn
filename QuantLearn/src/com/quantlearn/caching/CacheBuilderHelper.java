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
	 
    public static void buildHolidayCache(String holidayCalendarID) {
    	HashSet<LocalDate> h = new HashSet<LocalDate>(2000);
    	ImmutableSet<LocalDate> holidays = null;
    	
    	if (holidayCalendarID.contains("+")) {
			String[] ids = holidayCalendarID.split("\\+");				
			for (String region : ids) {
				h.addAll(HolidayCalendarUtils.generateHolidays(region));
			}			
		} else {
			h = HolidayCalendarUtils.generateHolidays(holidayCalendarID);			
		}	
    	holidays = ImmutableSet.copyOf(h);
    	
    	LoadingCache<String, ImmutableSet<LocalDate>> holidayCache = CacheBuilder.newBuilder()
    			.build(
                    new CacheLoader() {
                        @Override
                        public Object load(Object key) throws Exception {
                            return null;
                        }
                    }); 
    	if (holidayCalendarID.equals("UK+NYSE")) {
    		holidayCache.put(holidayCalendarID, holidays);
    		CacheManager.addCache(CacheManager.CACHE_IDS.UKNYSE, holidayCache);
    	} else if (holidayCalendarID.equals("UK")) {
    		holidayCache.put(holidayCalendarID, holidays);
    		CacheManager.addCache(CacheManager.CACHE_IDS.UK, holidayCache);
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
}
