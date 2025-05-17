// 파일: com.auto.trader.indicator.service.IndicatorComparisonService.java

package com.auto.trader.indicator.service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.auto.trader.indicator.dto.AllComparisonResultDto;
import com.auto.trader.indicator.dto.AllComparisonResultDto.CandleComparison;
import com.auto.trader.indicator.dto.AllComparisonResultDto.CandleDiff;
import com.auto.trader.indicator.dto.RsiComparisonRequestDto;
import com.auto.trader.indicator.dto.RsiComparisonResultDto;
import com.auto.trader.trade.dto.CandleDto;
import com.auto.trader.trade.indicator.IndicatorCache;
import com.auto.trader.trade.indicator.IndicatorMemoryStore;
import com.auto.trader.trade.indicator.IndicatorUtil;
import com.auto.trader.trade.indicator.IndicatorUtil.DualIndicatorPoint;
import com.auto.trader.trade.indicator.IndicatorUtil.IndicatorPoint;
import com.auto.trader.trade.indicator.IndicatorUtil.VWBB;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndicatorComparisonService {

	public List<RsiComparisonResultDto> compareRsi(RsiComparisonRequestDto dto) {
		List<CandleDto> candles = dto.getCandles();
		int rsiPeriod = 14;
		List<IndicatorPoint> frontendRsi = IndicatorUtil.calculateRSI(candles, rsiPeriod);

		String key = dto.getSymbol() + "_" + dto.getTimeframe();

		IndicatorCache cache = IndicatorMemoryStore.get(key);
		if (cache == null || cache.getRsi() == null)
			return Collections.emptyList();

		Map<Long, Double> backendRsiMap = cache
			.getRsi()
			.stream()
			.collect(Collectors.toMap(IndicatorPoint::getTime, IndicatorPoint::getValue));
		System.out.println("frontendRsi.size() : " + frontendRsi.size());
		return frontendRsi
			.stream()
			.filter(p -> p.getValue() != null && backendRsiMap.containsKey(p.getTime()))
			.sorted(Comparator.comparing(IndicatorPoint::getTime).reversed())
			.limit(30)
			.map(p -> {
				double backend = backendRsiMap.get(p.getTime());
				double diff = Math.abs(p.getValue() - backend);
				return new RsiComparisonResultDto(p.getTime(), p.getValue(), backend, diff);
			})
			.sorted(Comparator.comparing(RsiComparisonResultDto::getTime))
			.toList();
	}

	public AllComparisonResultDto compareAllIndicators(RsiComparisonRequestDto dto) {

		List<CandleDto> candles = dto.getCandles();
		CandleDto last = candles.get(candles.size() - 1);
		log.info("📦 프론트에서 보낸 마지막 캔들 시각: {}", IndicatorUtil.toKST(last.getTime() * 1000));

		int rsiPeriod = 14, emaPeriod = 14, smaPeriod = 20, vwbbPeriod = 20, multiplier = 2;
		int stochRsiPeriod = 14, k = 3, d = 3;
		String key = dto.getSymbol() + "_" + dto.getTimeframe();

		IndicatorCache cache = IndicatorMemoryStore.get(key);

		List<IndicatorPoint> frontendRsi = IndicatorUtil.calculateRSI(candles, rsiPeriod);
		List<IndicatorPoint> frontendEma = IndicatorUtil.calculateEMA(candles, emaPeriod);
		List<IndicatorPoint> frontendSma = IndicatorUtil.calculateSMA(candles, smaPeriod);
		VWBB frontendVwbb = IndicatorUtil.calculateVWBB(candles, vwbbPeriod, multiplier, 0.5);
		List<DualIndicatorPoint> frontendStoch = IndicatorUtil
			.calculateStochRSI(candles, stochRsiPeriod, stochRsiPeriod, k, d);

		Map<Long, Double> backendRsi = cache
			.getRsi()
			.stream()
			.filter(p -> p != null && p.getValue() != null)
			.collect(Collectors
				.toMap(p -> p.getTime() / 1000, // ✅ 수정된 부분
						IndicatorPoint::getValue));

		Map<Long, Double> backendEma = cache
			.getEma()
			.stream()
			.filter(p -> p != null && p.getValue() != null)
			.collect(Collectors.toMap(p -> p.getTime() / 1000, IndicatorPoint::getValue));

		Map<Long, Double> backendSma = cache
			.getSma()
			.stream()
			.filter(p -> p != null && p.getValue() != null)
			.collect(Collectors.toMap(p -> p.getTime() / 1000, IndicatorPoint::getValue));

		Map<Long, Double> upper = cache
			.getVwbb()
			.getUpper()
			.stream()
			.filter(p -> p != null && p.getValue() != null)
			.collect(Collectors.toMap(p -> p.getTime() / 1000, IndicatorPoint::getValue));

		Map<Long, Double> lower = cache
			.getVwbb()
			.getLower()
			.stream()
			.filter(p -> p != null && p.getValue() != null)
			.collect(Collectors.toMap(p -> p.getTime() / 1000, IndicatorPoint::getValue));

		Map<Long, Double> basis = cache
			.getVwbb()
			.getBasis()
			.stream()
			.filter(p -> p != null && p.getValue() != null)
			.collect(Collectors.toMap(p -> p.getTime() / 1000, IndicatorPoint::getValue));

		Map<Long, DualIndicatorPoint> stoch = cache
			.getStochRsi()
			.stream()
			.collect(Collectors.toMap(p -> p.getTime() / 1000, p -> p));

		List<AllComparisonResultDto.ComparisonPoint> rsi = compareSingle(frontendRsi, backendRsi);
		List<AllComparisonResultDto.ComparisonPoint> sma = compareSingle(frontendSma, backendSma);
		List<AllComparisonResultDto.ComparisonPoint> ema = compareSingle(frontendEma, backendEma);

		AllComparisonResultDto.VWBBComparison vwbb = new AllComparisonResultDto.VWBBComparison(
				compareSingle(frontendVwbb.getUpper(), upper), compareSingle(frontendVwbb.getLower(), lower),
				compareSingle(frontendVwbb.getBasis(), basis));

		List<AllComparisonResultDto.StochRsiComparisonPoint> stochList = frontendStoch
			.stream()
			.filter(f -> stoch.containsKey(f.getTime()))
			.map(f -> {
				DualIndicatorPoint b = stoch.get(f.getTime());
				double kDiff = diff(f.getK(), b.getK());
				double dDiff = diff(f.getD(), b.getD());
				return new AllComparisonResultDto.StochRsiComparisonPoint(f.getTime(), f.getK(), b.getK(), kDiff,
						f.getD(), b.getD(), dDiff);
			})
			.toList();

		System.out.println("▶️ [비교 KEY] " + key);

		System.out.println("🟦 frontendRsi.size: " + frontendRsi.size());
		System.out.println("🟦 frontendEma.size: " + frontendEma.size());
		System.out.println("🟦 frontendSma.size: " + frontendSma.size());
		System.out.println("🟦 frontendStoch.size: " + frontendStoch.size());
		System.out.println("🟦 frontendVwbb.upper.size: " + frontendVwbb.getUpper().size());

		System.out.println("🟥 backendRsi.size: " + backendRsi.size());
		System.out.println("🟥 backendEma.size: " + backendEma.size());
		System.out.println("🟥 backendSma.size: " + backendSma.size());
		System.out.println("🟥 upper.size: " + upper.size());
		System.out.println("🟥 lower.size: " + lower.size());
		System.out.println("🟥 basis.size: " + basis.size());
		System.out.println("🟥 stoch.size: " + stoch.size());

		List<CandleDto> backendCandles = cache
			.getCandles()
			.stream()
			.map(c -> new CandleDto(c.getTime() / 1000, // ✅ 초 단위로 변환
					c.getOpen(), c.getHigh(), c.getLow(), c.getClose(), c.getVolume()))
			.collect(Collectors.toList());

		Map<Long, CandleDto> frontMap = dto.getCandles().stream().collect(Collectors.toMap(CandleDto::getTime, c -> c));

		Map<Long, CandleDto> backMap = backendCandles.stream().collect(Collectors.toMap(CandleDto::getTime, c -> c));

		List<CandleComparison> candleComparisons = frontMap.keySet().stream().filter(backMap::containsKey).map(t -> {
			CandleDto f = frontMap.get(t);
			CandleDto b = backMap.get(t);
			CandleDiff diff = new CandleDiff(Math.abs(f.getClose() - b.getClose()),
					Math.abs(f.getVolume() - b.getVolume()));
			return new CandleComparison(t, f, b, diff);
		}).toList();
		// candleComparisons = null;
		return new AllComparisonResultDto(rsi, sma, ema, vwbb, stochList, candleComparisons);
	}

	private List<AllComparisonResultDto.ComparisonPoint> compareSingle(List<IndicatorPoint> front,
			Map<Long, Double> back) {

		return front
			.stream()
			.filter(p -> p.getValue() != null && back.containsKey(p.getTime()))
			.sorted(Comparator.comparing(IndicatorPoint::getTime).reversed())
			.limit(30)
			.map(p -> new AllComparisonResultDto.ComparisonPoint(p.getTime(), p.getValue(), back.get(p.getTime()),
					diff(p.getValue(), back.get(p.getTime()))))
			.sorted(Comparator.comparing(AllComparisonResultDto.ComparisonPoint::getTime))
			.toList();
	}

	private double diff(Double a, Double b) {
		if (a == null || b == null)
			return Double.NaN;
		return Math.abs(a - b);
	}
}