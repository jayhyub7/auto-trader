package com.auto.trader.indicator.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auto.trader.indicator.dto.AllComparisonResultDto;
import com.auto.trader.indicator.dto.IndicatorComparisonRequest;
import com.auto.trader.indicator.service.IndicatorComparisonService;
import com.auto.trader.trade.dto.CandleDto;
import com.auto.trader.trade.indicator.IndicatorCache;
import com.auto.trader.trade.indicator.IndicatorMemoryStore;
import com.auto.trader.trade.indicator.IndicatorUtil;
import com.auto.trader.trade.indicator.IndicatorUtil.DualIndicatorPoint;
import com.auto.trader.trade.indicator.IndicatorUtil.IndicatorPoint;
import com.auto.trader.trade.indicator.IndicatorUtil.VWBB;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/indicator")
@RequiredArgsConstructor
public class IndicatorComparisonController {

	private final IndicatorComparisonService indicatorComparisonService;

	@PostMapping("/backend-indicator")
	public AllComparisonResultDto backendIndicator(@RequestBody IndicatorComparisonRequest request) {
		String key = request.getSymbol() + "_" + request.getInterval();
		IndicatorCache cache = IndicatorMemoryStore.get(key);

		if (cache == null) {
			return new AllComparisonResultDto(); // empty
		}

		List<IndicatorPoint> rsi = convertIndicatorPoints(getLastN(cache.getRsi(), 30));
		List<DualIndicatorPoint> stoch = convertStochPoints(getLastN(cache.getStochRsi(), 30));
		VWBB vwbb = convertVWBB(limitVWBB(cache.getVwbb(), 30));

		return new AllComparisonResultDto(rsi, stoch, vwbb);
	}

	@PostMapping("/calculate-front-indicator")
	public AllComparisonResultDto compareBackend(@RequestBody List<CandleDto> request) {
		Map<String, List<?>> map = IndicatorUtil.calculateAllIndicators(request);

		List<IndicatorPoint> rsi = convertIndicatorPoints(getLastN(cast(map.get("rsi")), 30));
		List<DualIndicatorPoint> stoch = convertStochPoints(getLastN(cast(map.get("stochrsi")), 30));
		VWBB vwbb = convertVWBB(limitVWBB((VWBB) map.get("vwbb"), 30));

		return new AllComparisonResultDto(rsi, stoch, vwbb);
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> cast(Object obj) {
		return obj == null ? List.of() : (List<T>) obj;
	}

	private <T> List<T> getLastN(List<T> list, int n) {
		if (list == null || list.size() <= n)
			return list != null ? list : List.of();
		return list.subList(list.size() - n, list.size());
	}

	private VWBB limitVWBB(VWBB vwbb, int n) {
		if (vwbb == null)
			return null;
		return new VWBB(getLastN(vwbb.getUpper(), n), getLastN(vwbb.getBasis(), n), getLastN(vwbb.getLower(), n));
	}

	private List<IndicatorPoint> convertIndicatorPoints(List<IndicatorPoint> list) {
		return list
			.stream()
			.map(p -> new IndicatorPoint(p.getTime() / 1000L, p.getValue()))
			.collect(Collectors.toList());
	}

	private List<DualIndicatorPoint> convertStochPoints(List<DualIndicatorPoint> list) {
		return list
			.stream()
			.map(p -> new DualIndicatorPoint(p.getTime() / 1000L, p.getK(), p.getD()))
			.collect(Collectors.toList());
	}

	private VWBB convertVWBB(VWBB vwbb) {
		if (vwbb == null)
			return null;
		return new VWBB(
				vwbb.getUpper().stream().map(p -> new IndicatorPoint(p.getTime() / 1000L, p.getValue())).toList(),
				vwbb.getBasis().stream().map(p -> new IndicatorPoint(p.getTime() / 1000L, p.getValue())).toList(),
				vwbb.getLower().stream().map(p -> new IndicatorPoint(p.getTime() / 1000L, p.getValue())).toList());
	}
}
