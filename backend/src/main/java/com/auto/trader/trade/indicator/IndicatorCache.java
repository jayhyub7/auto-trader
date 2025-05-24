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
			List<IndicatorUtil.IndicatorPoint> upper = vwbb.getUpper();
			List<IndicatorUtil.IndicatorPoint> basis = vwbb.getBasis();
			List<IndicatorUtil.IndicatorPoint> lower = vwbb.getLower();
			int size = Math.min(upper.size(), Math.min(basis.size(), lower.size()));

			List<Map<String, Object>> vwbbList = new java.util.ArrayList<>();
			for (int i = 0; i < size; i++) {
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("time", upper.get(i).getTime());
				row.put("upper", upper.get(i).getValue());
				row.put("basis", basis.get(i).getValue());
				row.put("lower", lower.get(i).getValue());
				vwbbList.add(row);
			}
			map.put("vwbb", vwbbList);
		}
		return map;
	}
}