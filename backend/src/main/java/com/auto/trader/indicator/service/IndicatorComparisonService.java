package com.auto.trader.indicator.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.auto.trader.indicator.dto.AllComparisonRequestDto;
import com.auto.trader.indicator.dto.AllComparisonResultDto;
import com.auto.trader.trade.dto.CandleDto;
import com.auto.trader.trade.indicator.IndicatorCache;
import com.auto.trader.trade.indicator.IndicatorMemoryStore;
import com.auto.trader.trade.indicator.IndicatorUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IndicatorComparisonService {

	public AllComparisonResultDto compareAll(AllComparisonRequestDto request) {
		String key = request.getSymbol() + "_" + request.getTimeframe();
		IndicatorCache backend = IndicatorMemoryStore.get(key);
		List<CandleDto> frontendCandles = request.getCandles();

		Map<String, Object> result = new HashMap<>();

		if (backend == null || backend.getRsi() == null) {
			result.put("error", "백엔드 캐시에 지표 데이터가 없습니다.");
			return AllComparisonResultDto.builder().result(result).build();
		}

		// ✅ RSI 비교
		List<IndicatorUtil.IndicatorPoint> frontendRsi = IndicatorUtil.calculateRSI(frontendCandles, 14);
		List<IndicatorUtil.IndicatorPoint> backendRsi = backend.getRsi();
		List<Map<String, Object>> rsiDiffList = new ArrayList<>();
		for (int i = 0; i < Math.min(frontendRsi.size(), backendRsi.size()); i++) {
			var f = frontendRsi.get(i);
			var b = backendRsi.get(i);
			if (f.getValue() == null || b.getValue() == null)
				continue;
			double diff = Math.abs(f.getValue() - b.getValue());
			rsiDiffList
				.add(Map.of("time", f.getTime(), "frontend", f.getValue(), "backend", b.getValue(), "diff", diff));
		}
		result.put("rsi", rsiDiffList);

		// ✅ StochRSI 비교
		List<IndicatorUtil.DualIndicatorPoint> frontendStoch = IndicatorUtil
			.calculateStochRSI(frontendCandles, 14, 14, 3, 3);
		List<IndicatorUtil.DualIndicatorPoint> backendStoch = backend.getStochRsi();
		List<Map<String, Object>> stochDiffList = new ArrayList<>();
		for (int i = 0; i < Math.min(frontendStoch.size(), backendStoch.size()); i++) {
			var f = frontendStoch.get(i);
			var b = backendStoch.get(i);
			if (f.getK() == null || b.getK() == null || f.getD() == null || b.getD() == null)
				continue;
			double kdiff = Math.abs(f.getK() - b.getK());
			double ddiff = Math.abs(f.getD() - b.getD());
			stochDiffList
				.add(Map
					.of("time", f.getTime(), "frontend_k", f.getK(), "backend_k", b.getK(), "kdiff", kdiff,
							"frontend_d", f.getD(), "backend_d", b.getD(), "ddiff", ddiff));
		}
		result.put("stochRsi", stochDiffList);

		// ✅ VWBB 비교
		IndicatorUtil.VWBB frontendVwbb = IndicatorUtil.calculateVWBB(frontendCandles, 20, 2.0);
		IndicatorUtil.VWBB backendVwbb = backend.getVwbb();
		List<Map<String, Object>> upperList = new ArrayList<>();
		List<Map<String, Object>> lowerList = new ArrayList<>();
		List<Map<String, Object>> basisList = new ArrayList<>();

		for (int i = 0; i < Math.min(frontendVwbb.getUpper().size(), backendVwbb.getUpper().size()); i++) {
			var f = frontendVwbb.getUpper().get(i);
			var b = backendVwbb.getUpper().get(i);
			if (f.getValue() == null || b.getValue() == null)
				continue;
			upperList
				.add(Map
					.of("time", f.getTime(), "frontend", f.getValue(), "backend", b.getValue(), "diff",
							Math.abs(f.getValue() - b.getValue())));
		}
		for (int i = 0; i < Math.min(frontendVwbb.getLower().size(), backendVwbb.getLower().size()); i++) {
			var f = frontendVwbb.getLower().get(i);
			var b = backendVwbb.getLower().get(i);
			if (f.getValue() == null || b.getValue() == null)
				continue;
			lowerList
				.add(Map
					.of("time", f.getTime(), "frontend", f.getValue(), "backend", b.getValue(), "diff",
							Math.abs(f.getValue() - b.getValue())));
		}
		for (int i = 0; i < Math.min(frontendVwbb.getBasis().size(), backendVwbb.getBasis().size()); i++) {
			var f = frontendVwbb.getBasis().get(i);
			var b = backendVwbb.getBasis().get(i);
			if (f.getValue() == null || b.getValue() == null)
				continue;
			basisList
				.add(Map
					.of("time", f.getTime(), "frontend", f.getValue(), "backend", b.getValue(), "diff",
							Math.abs(f.getValue() - b.getValue())));
		}
		result.put("vwbb", Map.of("upper", upperList, "lower", lowerList, "basis", basisList));

		List<CandleDto> backendCandles = backend.getCandles();
		List<Map<String, Object>> candleDiffList = new ArrayList<>();

		for (int i = 0; i < Math.min(frontendCandles.size(), backendCandles.size()); i++) {
			var f = frontendCandles.get(i);
			var b = backendCandles.get(i);
			if (f == null || b == null)
				continue;

			Map<String, Object> entry = new HashMap<>();
			entry.put("time", f.getTime());

			entry
				.put("frontend",
						Map
							.of("open", f.getOpen(), "high", f.getHigh(), "low", f.getLow(), "close", f.getClose(),
									"volume", f.getVolume()));

			entry
				.put("backend",
						Map
							.of("open", b.getOpen(), "high", b.getHigh(), "low", b.getLow(), "close", b.getClose(),
									"volume", b.getVolume()));

			entry
				.put("diff",
						Map
							.of("close", Math.abs(f.getClose() - b.getClose()), "volume",
									Math.abs(f.getVolume() - b.getVolume())));

			candleDiffList.add(entry);
		}

		result.put("candles", candleDiffList);

		return AllComparisonResultDto.builder().result(result).build();
	}
}
