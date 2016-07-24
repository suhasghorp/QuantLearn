package com.quantlearn.caching;

import com.google.common.cache.LoadingCache;
import com.google.common.base.Preconditions;
import com.quantlearn.schedule.BusDate;

public class RefDateUtils {
private static LoadingCache<String, Object> refDateCache;
	
	public static BusDate getRefDate() {
		refDateCache = CacheManager.getCache(CacheManager.CACHE_IDS.REFDATE);
		BusDate refDate = ((BusDate)refDateCache.getIfPresent("REF_DATE"));
		if (refDate == null) {
			Preconditions.checkNotNull(refDate, "Ref date was not added to Cache, please add it first");
		}
		return refDate;
	}
	public static void cacheRefDate(BusDate refDate) {
		refDateCache = CacheManager.getCache(CacheManager.CACHE_IDS.REFDATE);
		refDateCache.put("REF_DATE", refDate);
	}
}
