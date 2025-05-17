// 파일: com.auto.trader.trade.indicator.IndicatorCache.java

package com.auto.trader.trade.indicator;

import java.util.List;

import com.auto.trader.trade.dto.CandleDto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IndicatorCache {

	private final List<CandleDto> candles; // 최대 500개
	private final List<IndicatorUtil.IndicatorPoint> rsi;
	private final List<IndicatorUtil.IndicatorPoint> ema;
	private final List<IndicatorUtil.IndicatorPoint> sma;
	private final List<IndicatorUtil.DualIndicatorPoint> stochRsi;
	private final IndicatorUtil.VWBB vwbb;
	private double currentPrice;
}