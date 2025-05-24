package com.auto.trader.trade.indicator;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndicatorKeyMap {

	private static final Map<String, List<String>> KEY_MAP = new HashMap<>();

	static {
		KEY_MAP.put("rsi", List.of("value"));
		KEY_MAP.put("ema", List.of("value"));
		KEY_MAP.put("sma", List.of("value"));
		KEY_MAP.put("stochrsi", List.of("k", "d"));
		KEY_MAP.put("vwbb_upper", List.of("upper"));
		KEY_MAP.put("vwbb_lower", List.of("lower"));
		KEY_MAP.put("vwbb_basis", List.of("basis"));
	}

	public static List<String> getKeys(String indicatorName) {
		return KEY_MAP.getOrDefault(indicatorName.toLowerCase(), Collections.emptyList());
	}

	public static boolean supports(String indicatorName) {
		return KEY_MAP.containsKey(indicatorName.toLowerCase());
	}

	public static Map<String, List<String>> getAll() {
		return Collections.unmodifiableMap(KEY_MAP);
	}
}
