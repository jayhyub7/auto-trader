package com.auto.trader.indicator.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auto.trader.indicator.dto.AllComparisonResultDto;
import com.auto.trader.indicator.dto.IndicatorComparisonRequest;
import com.auto.trader.indicator.service.IndicatorComparisonService;
import com.auto.trader.trade.indicator.IndicatorCache;
import com.auto.trader.trade.indicator.IndicatorMemoryStore;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/indicator")
@RequiredArgsConstructor
public class IndicatorComparisonController {

	private final IndicatorComparisonService indicatorComparisonService;

	// ✅ 프론트 계산된 값 기반 비교
	@PostMapping("/compare-frontend")
	public AllComparisonResultDto compareFrontend(@RequestBody IndicatorComparisonRequest request) {
		return indicatorComparisonService
			.compareWithFrontendOnly(request.getSymbol(), request.getInterval(), request.getFrontendCandles());
	}

	// ✅ 백엔드 내부 메모리 기반 비교
	@PostMapping("/compare-backend")
	public AllComparisonResultDto compareBackend(@RequestBody IndicatorComparisonRequest request) {
		return indicatorComparisonService.compareWithBackendOnly(request.getSymbol(), request.getInterval());
	}

	@PostMapping("/fetch-cached-indicators")
	public ResponseEntity<Map<String, List<?>>> fetchCachedIndicators(@RequestBody IndicatorComparisonRequest request) {

		String key = request.getSymbol() + "_" + request.getInterval();
		IndicatorCache cache = IndicatorMemoryStore.get(key);

		if (cache == null) {
			return ResponseEntity.badRequest().build();
		}

		Map<String, List<?>> result = new LinkedHashMap<>();
		result.put("rsi", latest(cache.getRsi()));
		result.put("stochrsi", latest(cache.getStochRsi()));

		if (cache.getVwbb() != null) {
			result.put("vwbb_upper", latest(cache.getVwbb().getUpper()));
			result.put("vwbb_basis", latest(cache.getVwbb().getBasis()));
			result.put("vwbb_lower", latest(cache.getVwbb().getLower()));
		}

		return ResponseEntity.ok(result);
	}

	private static <T> List<T> latest(List<T> list) {
		return list == null || list.size() <= 30 ? list : list.subList(list.size() - 30, list.size());
	}

}
