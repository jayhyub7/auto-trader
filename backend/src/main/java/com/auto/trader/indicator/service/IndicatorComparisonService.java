package com.auto.trader.indicator.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.auto.trader.indicator.dto.AllComparisonResultDto;
import com.auto.trader.trade.dto.CandleDto;
import com.auto.trader.trade.indicator.IndicatorCache;
import com.auto.trader.trade.indicator.IndicatorKeyMap;
import com.auto.trader.trade.indicator.IndicatorMemoryStore;
import com.auto.trader.trade.indicator.IndicatorUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndicatorComparisonService {

	private final ObjectMapper objectMapper;

	/**
	 * ✅ 프론트 계산 결과와 백엔드 메모리 지표를 비교
	 */
	public AllComparisonResultDto compareWithFrontendOnly(String symbol, String interval,
			List<CandleDto> frontendCandles) {
		if (frontendCandles == null || frontendCandles.isEmpty()) {
			log.warn("❌ [프론트 비교] 캔들이 비어 있음");
			throw new IllegalArgumentException("프론트 캔들이 비어 있습니다");
		}

		Map<String, List<?>> frontMap = IndicatorUtil.calculateAllIndicators(frontendCandles);

		String key = symbol + "_" + interval;
		IndicatorCache cache = IndicatorMemoryStore.get(key);
		if (cache == null) {
			log.warn("❌ [프론트 비교] IndicatorCache 없음: {}", key);
			throw new IllegalStateException("IndicatorCache 없음: " + key);
		}

		Map<String, List<?>> backMap = cache.toMap();

		return compare(frontMap, backMap);
	}

	/**
	 * ✅ 백엔드 메모리 기준 지표를 재계산하여 비교
	 */
	public AllComparisonResultDto compareWithBackendOnly(String symbol, String interval) {
		String key = symbol + "_" + interval;
		IndicatorCache cache = IndicatorMemoryStore.get(key);
		if (cache == null) {
			log.warn("❌ [백엔드 비교] IndicatorCache 없음: {}", key);
			throw new IllegalStateException("IndicatorCache 없음: " + key);
		}

		Map<String, List<?>> frontMap = cache.toMap(); // 기존 저장된 지표
		Map<String, List<?>> backMap = IndicatorUtil.calculateAllIndicators(cache.getCandles()); // 지금 다시 계산

		return compare(frontMap, backMap);
	}

	private AllComparisonResultDto compare(Map<String, List<?>> frontMap, Map<String, List<?>> backMap) {
		Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();

		for (String indicator : frontMap.keySet()) {
			List<?> frontList = frontMap.get(indicator);
			List<?> backList = backMap.getOrDefault(indicator, Collections.emptyList());

			List<String> keys = IndicatorKeyMap.getKeys(indicator);
			if (keys.isEmpty())
				continue;

			System.out.println("frontList");
			System.out.println(frontList);
			System.out.println("backList");
			System.out.println(backList);

			int size = Math.min(frontList.size(), backList.size());
			List<Map<String, Object>> merged = new ArrayList<>();

			for (int i = 0; i < size; i++) {
				Map<String, Object> item = new LinkedHashMap<>();
				Map<String, Object> f = new LinkedHashMap<>();
				Map<String, Object> b = new LinkedHashMap<>();
				Map<String, Object> d = new LinkedHashMap<>();

				Object frontObj = frontList.get(i);
				Object backObj = backList.get(i);

				long time = extractTime(frontObj);

				item.put("time", time);
				d.put("time", time);

				for (String key : keys) {
					double fv = extractValue(frontObj, key);
					double bv = extractValue(backObj, key);
					f.put(key, fv);
					b.put(key, bv);
					d.put(key + "diff", Math.abs(fv - bv));
				}

				item.put("frontend", f);
				item.put("backend", b);
				item.put("diff", d);
				merged.add(item);
			}

			result.put(indicator, merged);
		}

		return new AllComparisonResultDto(result);
	}

	private double parseDouble(Object o) {
		if (o == null)
			return 0.0;
		try {
			return Double.parseDouble(o.toString());
		} catch (Exception e) {
			return 0.0;
		}
	}

	private long extractTime(Object obj) {
		try {
			if (obj instanceof IndicatorUtil.IndicatorPoint point)
				return point.getTime();
			if (obj instanceof IndicatorUtil.DualIndicatorPoint dual)
				return dual.getTime();
		} catch (Exception e) {
			log.warn("⚠️ time 추출 실패: {}", obj);
		}
		return 0L;
	}

	private double extractValue(Object obj, String key) {
		try {
			if (obj instanceof IndicatorUtil.IndicatorPoint point) {
				return "value".equals(key) ? safe(point.getValue()) : 0.0;
			}
			if (obj instanceof IndicatorUtil.DualIndicatorPoint dual) {
				return switch (key) {
				case "k" -> safe(dual.getK());
				case "d" -> safe(dual.getD());
				default -> 0.0;
				};
			}
		} catch (Exception e) {
			log.warn("⚠️ value 추출 실패: {}", obj);
		}
		return 0.0;
	}

	private double safe(Double d) {
		return d != null ? d : 0.0;
	}
}
