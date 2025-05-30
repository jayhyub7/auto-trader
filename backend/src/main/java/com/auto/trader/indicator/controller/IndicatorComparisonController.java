package com.auto.trader.indicator.controller;

import java.util.Comparator;
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

		List<IndicatorPoint> rsi = convertIndicatorPoints(getSortedLastN(cache.getRsi(), 30));
		List<DualIndicatorPoint> stoch = convertStochPoints(getSortedLastNStoch(cache.getStochRsi(), 30));
		VWBB vwbb = convertVWBB(limitVWBB(cache.getVwbb(), 30));

		return new AllComparisonResultDto(rsi, stoch, vwbb);
	}

	@PostMapping("/calculate-front-indicator")
	public AllComparisonResultDto compareBackend(@RequestBody List<CandleDto> request) {
		Map<String, List<?>> map = IndicatorUtil.calculateAllIndicators(request);

		List<IndicatorPoint> rsi = convertIndicatorPoints(getSortedLastN(cast(map.get("rsi")), 30));
		List<DualIndicatorPoint> stoch = convertStochPoints(getSortedLastNStoch(cast(map.get("stochrsi")), 30));
		VWBB vwbb = convertVWBB(limitVWBB((VWBB) map.get("vwbb"), 30));

		return new AllComparisonResultDto(rsi, stoch, vwbb);
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> cast(Object obj) {
		return obj == null ? List.of() : (List<T>) obj;
	}

	private List<IndicatorPoint> getSortedLastN(List<IndicatorPoint> list, int n) {
		if (list == null || list.isEmpty())
			return List.of();
		return list
			.stream()
			.sorted(Comparator.comparingLong(IndicatorPoint::getTime))
			.skip(Math.max(0, list.size() - n))
			.collect(Collectors.toList());
	}

	private List<DualIndicatorPoint> getSortedLastNStoch(List<DualIndicatorPoint> list, int n) {
		if (list == null || list.isEmpty())
			return List.of();
		return list
			.stream()
			.sorted(Comparator.comparingLong(DualIndicatorPoint::getTime))
			.skip(Math.max(0, list.size() - n))
			.collect(Collectors.toList());
	}

	private VWBB limitVWBB(VWBB vwbb, int n) {
		if (vwbb == null)
			return null;

		Comparator<IndicatorPoint> comparator = Comparator.comparingLong(IndicatorPoint::getTime);

		List<IndicatorPoint> upper = vwbb
			.getUpper()
			.stream()
			.sorted(comparator)
			.skip(Math.max(0, vwbb.getUpper().size() - n))
			.collect(Collectors.toList());

		List<IndicatorPoint> basis = vwbb
			.getBasis()
			.stream()
			.sorted(comparator)
			.skip(Math.max(0, vwbb.getBasis().size() - n))
			.collect(Collectors.toList());

		List<IndicatorPoint> lower = vwbb
			.getLower()
			.stream()
			.sorted(comparator)
			.skip(Math.max(0, vwbb.getLower().size() - n))
			.collect(Collectors.toList());

		return new VWBB(upper, basis, lower);
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

	// 📈 시장 추세를 판별하기 위한 열거형(enum)
	// - BULL: 상승장 (가격이 지속적으로 상승 중인 구간)
	// - BEAR: 하락장 (가격이 지속적으로 하락 중인 구간)
	// - SIDEWAYS: 횡보장 (명확한 방향 없이 박스권 내에서 움직이는 구간)
	public enum MarketTrend {
		BULL, // 상승 추세
		BEAR, // 하락 추세
		SIDEWAYS // 횡보 구간
	}

	public MarketTrend detectMarketTrend(List<CandleDto> candles) {
		if (candles == null || candles.size() < 20)
			return MarketTrend.SIDEWAYS;

		List<CandleDto> recent = candles.subList(candles.size() - 20, candles.size());
		double first = recent.get(0).getClose();
		double last = recent.get(recent.size() - 1).getClose();
		double slope = (last - first) / first;

		if (slope > 0.015)
			return MarketTrend.BULL;
		else if (slope < -0.015)
			return MarketTrend.BEAR;
		else
			return MarketTrend.SIDEWAYS;
	}

}
