// 파일: com.auto.trader.trade.indicator.IndicatorCache.java

package com.auto.trader.trade.indicator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.auto.trader.trade.dto.CandleDto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IndicatorCache {

	private final List<CandleDto> candles; // 최대 500개
	private final List<IndicatorUtil.IndicatorPoint> rsi;
	private final List<IndicatorUtil.IndicatorPoint> ema;
	private final List<IndicatorUtil.IndicatorPoint> sma;
	private final List<IndicatorUtil.DualIndicatorPoint> stochRsi;
	private final IndicatorUtil.VWBB vwbb;
	private double currentPrice;

	// 내부 지표를 통합 Map 으로 반환
	public Map<String, List<?>> toMap() {
		Map<String, List<?>> map = new LinkedHashMap<>();
		if (rsi != null)
			map.put("rsi", rsi);
		if (ema != null)
			map.put("ema", ema);
		if (sma != null)
			map.put("sma", sma);
		if (stochRsi != null)
			map.put("stochRsi", stochRsi);
		if (vwbb != null) {
			map.put("vwbb_upper", vwbb.getUpper());
			map.put("vwbb_basis", vwbb.getBasis());
			map.put("vwbb_lower", vwbb.getLower());
		}
		return map;
	}
}