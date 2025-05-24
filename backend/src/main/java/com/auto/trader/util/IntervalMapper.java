package com.auto.trader.util;

import java.util.Map;

public class IntervalMapper {

	private static final Map<String, Long> INTERVAL_TO_MILLIS = Map
		.of("1m", 60_000L, "3m", 3 * 60_000L, "5m", 5 * 60_000L, "15m", 15 * 60_000L, "1h", 60 * 60_000L, "4h",
				4 * 60 * 60_000L, "1d", 24 * 60 * 60_000L);

	public static long getMillis(String interval) {
		Long millis = INTERVAL_TO_MILLIS.get(interval);
		if (millis == null) {
			throw new IllegalArgumentException("지원하지 않는 interval: " + interval);
		}
		return millis;
	}
}