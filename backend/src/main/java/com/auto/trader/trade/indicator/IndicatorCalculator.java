// 파일: com.auto.trader.trade.indicator.IndicatorCalculator.java

package com.auto.trader.trade.indicator;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.auto.trader.trade.dto.CandleDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class IndicatorCalculator {

	public void calculateAndStore(String symbol, String timeframe, List<CandleDto> candles) {

		try {
			if (candles.size() < 50)
				return;

			candles.sort(Comparator.comparingLong(CandleDto::getTime));

			// 🔄 마지막 캔들까지 포함하여 계산
			List<CandleDto> trimmed = candles;

			List<IndicatorUtil.IndicatorPoint> rsi = IndicatorUtil.calculateRSI(trimmed, 14);
			List<IndicatorUtil.IndicatorPoint> ema = IndicatorUtil.calculateEMA(trimmed, 14);
			List<IndicatorUtil.IndicatorPoint> closePoints = trimmed
				.stream()
				.map(c -> new IndicatorUtil.IndicatorPoint(c.getTime(), c.getClose()))
				.collect(Collectors.toList());
			List<IndicatorUtil.IndicatorPoint> sma = IndicatorUtil.calculateSMA(closePoints, 14);
			List<IndicatorUtil.DualIndicatorPoint> stoch = IndicatorUtil.calculateStochRSI(trimmed, 14, 14, 3, 3);

			IndicatorUtil.VWBB vwbb = IndicatorUtil.calculateVWBB(trimmed, 20, 2);

			double currentPrice = candles.get(candles.size() - 1).getClose();

			IndicatorCache cache = new IndicatorCache(candles, rsi, ema, sma, stoch, vwbb, currentPrice);

			String key = symbol + "_" + timeframe;
			IndicatorMemoryStore.put(key, cache);

		} catch (Exception e) {
			log.error("❌ 지표 계산 실패 [{}_{}]", symbol, timeframe, e);
		}
	}
}