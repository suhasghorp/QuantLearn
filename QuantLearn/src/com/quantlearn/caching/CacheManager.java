package com.quantlearn.caching;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;

public class CacheManager {
	 
    /**
     * Enum defining the different types of cache managed by the class
     */
    public enum CACHE_IDS {
    	HOLIDAYS,
    	REFDATE,
        CALENDAR,
        CACHE_ID2,
        CACHE_ID3
    }
 
    /**
     * Map containing all the caches
     */
    public final static Map<CACHE_IDS, LoadingCache> caches = new ConcurrentHashMap<CACHE_IDS, LoadingCache>();
 
    /**
     * Adds or replace one of the caches
     *
     * @param cacheId Identificator of the Cache
     * @param cache   Cache to be added
     */
    public static void addCache(CACHE_IDS cacheId, LoadingCache cache) {
        caches.put(cacheId, cache);
    }
 
    /**
     * Retrieves one of the managed cache
     *
     * @param cacheId Identificator of the Cache
     * @return The cache if it exists or null if it doesn't
     */
    public static LoadingCache<String,Object> getCache(CACHE_IDS cacheId) {
        return caches.get(cacheId);
    }
 
    /**
     * Retrieves directly a cached object from one of the defined caches
     * @param cacheId
     * @param elementId
     * @return
     */
    public static Object getCachedObject(CACHE_IDS cacheId, String elementId) {
        Object result = null;
 
        LoadingCache cache = getCache(cacheId);
        if (cache != null){
            result = cache.getIfPresent(elementId);
        }
 
        return result;
    }
    
    public static ImmutableSet<LocalDate> getCachedHolidays() {
    	ImmutableSet<LocalDate> holidays = null;
    	LoadingCache cache = CacheManager.getCache(CacheManager.CACHE_IDS.HOLIDAYS);
    	if (cache == null) {
    		holidays = ImmutableSet.of();
    	} else {
    		holidays = (ImmutableSet<LocalDate>)cache.getIfPresent("HOLIDAYS");
    	}
    	return holidays;
    }
    
    public static ImmutableSet<LocalDate> getCachedUKHolidays() {
    	ImmutableSet<LocalDate> holidays = null;
    	LoadingCache cache = CacheManager.getCache(CacheManager.CACHE_IDS.HOLIDAYS);
    	if (cache == null) {
    		holidays = ImmutableSet.of();
    	} else {
    		holidays = (ImmutableSet<LocalDate>)cache.getIfPresent("UK");
    	}
    	return holidays;
    }
}