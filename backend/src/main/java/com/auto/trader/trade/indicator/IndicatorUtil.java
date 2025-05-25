package com.auto.trader.trade.indicator;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.auto.trader.trade.dto.CandleDto;

import lombok.Getter;

public class IndicatorUtil {

	@Getter
	public static class IndicatorPoint {
		private long time;
		private Double value;

		public IndicatorPoint(long time, Double value) {
			this.time = time;
			this.value = value;
		}

		@Override
		public String toString() {
			return String.format("[time=%s, value=%.4f]", toKST(time * 1000), value);
		}
	}

	@Getter
	public static class DualIndicatorPoint {
		private long time;
		private Double k;
		private Double d;

		public DualIndicatorPoint(long time, Double k, Double d) {
			this.time = time;
			this.k = k;
			this.d = d;
		}

		@Override
		public String toString() {
			return String.format("[time=%s, K=%.4f, D=%.4f]", toKST(time * 1000), k, d);
		}
	}

	@Getter
	public static class VWBB {
		private List<IndicatorPoint> upper;
		private List<IndicatorPoint> lower;
		private List<IndicatorPoint> basis;

		public VWBB(List<IndicatorPoint> upper, List<IndicatorPoint> lower, List<IndicatorPoint> basis) {
			this.upper = upper;
			this.lower = lower;
			this.basis = basis;
		}

		@Override
		public String toString() {
			return String
				.format("VWBB\n  upper=%s\n  basis=%s\n  lower=%s", upper != null ? upper.size() + "pts" : "null",
						basis != null ? basis.size() + "pts" : "null", lower != null ? lower.size() + "pts" : "null");
		}
	}

	public static Map<String, List<? extends Object>> calculateAllIndicators(List<CandleDto> candles) {
		Map<String, List<? extends Object>> result = new HashMap<>();
		result.put("rsi", calculateRSI(candles, 14));
		result.put("stochRsi", calculateStochRSI(candles, 14, 14, 3, 3));

		VWBB vwbb = calculateVWBB(candles, 20, 2.0);

		List<Map<String, Object>> merged = new ArrayList<>();
		List<IndicatorPoint> upper = vwbb.getUpper();
		List<IndicatorPoint> basis = vwbb.getBasis();
		List<IndicatorPoint> lower = vwbb.getLower();

		int size = Math.min(upper.size(), Math.min(basis.size(), lower.size()));

		for (int i = 0; i < size; i++) {
			Map<String, Object> item = new LinkedHashMap<>();
			long time = upper.get(i).getTime();
			item.put("time", time);

			Map<String, Object> part = new LinkedHashMap<>();
			part.put("upper", safe(upper.get(i).getValue()));
			part.put("basis", safe(basis.get(i).getValue()));
			part.put("lower", safe(lower.get(i).getValue()));

			item.put("value", part); // value에 3개 다 묶기 (프론트 구조에 맞게 조정 가능)
			merged.add(item);
		}

		result.put("vwbb", merged);
		return result;
	}

	private static double safe(Double d) {
		return d != null ? d : 0.0;
	}

	public static List<IndicatorPoint> calculateEMA(List<CandleDto> data, int period) {
		List<IndicatorPoint> result = new ArrayList<>();
		if (data.isEmpty())
			return result;

		double k = 2.0 / (period + 1);
		double emaPrev = data.get(0).getClose();

		for (int i = 0; i < data.size(); i++) {
			CandleDto d = data.get(i);
			if (i == 0) {
				result.add(new IndicatorPoint(d.getTime(), emaPrev));
			} else {
				emaPrev = d.getClose() * k + emaPrev * (1 - k);
				result.add(new IndicatorPoint(d.getTime(), emaPrev));
			}
		}
		return result;
	}

	public static List<IndicatorPoint> calculateSMA(List<IndicatorPoint> data, int period) {
		List<IndicatorPoint> result = new ArrayList<>();
		for (int i = 0; i < data.size(); i++) {
			if (i < period - 1 || data.get(i).getValue() == null) {
				result.add(new IndicatorPoint(data.get(i).getTime(), null));
				continue;
			}

			List<IndicatorPoint> window = data.subList(i - period + 1, i + 1);
			List<IndicatorPoint> valid = window.stream().filter(d -> d.getValue() != null).collect(Collectors.toList());

			if (valid.size() < period) {
				result.add(new IndicatorPoint(data.get(i).getTime(), null));
				continue;
			}

			double avg = valid.stream().mapToDouble(IndicatorPoint::getValue).average().orElse(0);
			result.add(new IndicatorPoint(data.get(i).getTime(), avg));
		}

		return result;
	}

	public static List<IndicatorPoint> calculateRSI(List<CandleDto> data, int period) {
		double avgGain = 0;
		double avgLoss = 0;
		List<IndicatorPoint> result = new ArrayList<>();

		for (int i = 1; i < data.size(); i++) {
			double diff = data.get(i).getClose() - data.get(i - 1).getClose();
			double gain = Math.max(diff, 0);
			double loss = Math.max(-diff, 0);

			if (i <= period) {
				avgGain += gain;
				avgLoss += loss;
				result.add(new IndicatorPoint(data.get(i).getTime(), null));
			} else if (i == period + 1) {
				avgGain /= period;
				avgLoss /= period;
				double rs = avgLoss == 0 ? 100 : avgGain / avgLoss;
				double rsi = 100 - 100 / (1 + rs);
				result.add(new IndicatorPoint(data.get(i).getTime(), rsi));
			} else {
				avgGain = (avgGain * (period - 1) + gain) / period;
				avgLoss = (avgLoss * (period - 1) + loss) / period;
				double rs = avgLoss == 0 ? 100 : avgGain / avgLoss;
				double rsi = 100 - 100 / (1 + rs);
				result.add(new IndicatorPoint(data.get(i).getTime(), rsi));
			}
		}

		return result;
	}

	public static List<DualIndicatorPoint> calculateStochRSI(List<CandleDto> data, int rsiPeriod, int stochPeriod,
			int kPeriod, int dPeriod) {
		List<IndicatorPoint> rsi = calculateRSI(data, rsiPeriod);
		List<IndicatorPoint> stochRsi = new ArrayList<>();

		for (int i = 0; i < rsi.size(); i++) {
			if (i < stochPeriod - 1) {
				stochRsi.add(new IndicatorPoint(rsi.get(i).getTime(), null));
				continue;
			}

			List<Double> window = rsi
				.subList(i - stochPeriod + 1, i + 1)
				.stream()
				.map(IndicatorPoint::getValue)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

			if (window.size() < stochPeriod) {
				stochRsi.add(new IndicatorPoint(rsi.get(i).getTime(), null));
				continue;
			}

			double min = Collections.min(window);
			double max = Collections.max(window);
			double current = rsi.get(i).getValue();

			double value = (max - min == 0) ? 0 : ((current - min) / (max - min)) * 100;
			stochRsi.add(new IndicatorPoint(rsi.get(i).getTime(), value));
		}

		List<IndicatorPoint> smoothedK = calculateSMA(stochRsi, kPeriod);
		List<IndicatorPoint> smoothedD = calculateSMA(smoothedK, dPeriod);

		List<DualIndicatorPoint> result = new ArrayList<>();
		for (int i = 0; i < rsi.size(); i++) {
			Double k = i < smoothedK.size() ? smoothedK.get(i).getValue() : null;
			Double d = i < smoothedD.size() ? smoothedD.get(i).getValue() : null;
			result.add(new DualIndicatorPoint(rsi.get(i).getTime(), k, d));
		}

		return result;
	}

	public static VWBB calculateVWBB(List<CandleDto> candles, int period, double multiplier) {
		int size = candles.size();
		List<Double> vwma = new ArrayList<>(Collections.nCopies(size, null));
		List<Double> std = new ArrayList<>(Collections.nCopies(size, null));

		for (int i = period - 1; i < size; i++) {
			double volSum = 0, priceVolSum = 0;
			for (int j = i - period + 1; j <= i; j++) {
				double price = candles.get(j).getClose();
				double volume = candles.get(j).getVolume();
				if (volume == 0)
					volume = 1; // 프론트 동일
				volSum += volume;
				priceVolSum += price * volume;
			}
			vwma.set(i, priceVolSum / volSum);
		}

		for (int i = period - 1; i < size; i++) {
			if (vwma.get(i) == null)
				continue;
			double mean = vwma.get(i);
			double sum = 0;
			for (int j = i - period + 1; j <= i; j++) {
				double diff = candles.get(j).getClose() - mean;
				sum += diff * diff;
			}
			std.set(i, Math.sqrt(sum / period));
		}

		List<IndicatorPoint> basis = new ArrayList<>();
		List<IndicatorPoint> upper = new ArrayList<>();
		List<IndicatorPoint> lower = new ArrayList<>();

		for (int i = 0; i < size; i++) {
			long time = candles.get(i).getTime();
			Double mean = vwma.get(i);
			Double deviation = std.get(i);
			basis.add(new IndicatorPoint(time, mean));
			upper
				.add(new IndicatorPoint(time,
						(mean != null && deviation != null) ? mean + multiplier * deviation : null));
			lower
				.add(new IndicatorPoint(time,
						(mean != null && deviation != null) ? mean - multiplier * deviation : null));
		}

		return new VWBB(upper, lower, basis);
	}

	public static String toKST(long millis) {
		ZonedDateTime zdt = Instant.ofEpochMilli(millis).atZone(ZoneId.of("Asia/Seoul"));
		return zdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
	}

	public static CandlePattern detectCandlePattern(CandleDto current, CandleDto previous) {
		double open = current.getOpen();
		double close = current.getClose();
		double high = current.getHigh();
		double low = current.getLow();

		double body = Math.abs(close - open);
		double lowerWick = Math.min(open, close) - low;
		double upperWick = high - Math.max(open, close);

		// 🔹 Hammer: 긴 아랫꼬리 양봉
		if (lowerWick > body * 2 && upperWick < body * 0.3 && close > open) {
			return CandlePattern.HAMMER;
		}

		// 🔹 Shooting Star: 긴 윗꼬리 음봉
		if (upperWick > body * 2 && lowerWick < body * 0.3 && close < open) {
			return CandlePattern.SHOOTING_STAR;
		}

		// 🔹 Engulfing: 전 캔들을 완전히 덮는 양/음봉
		if (previous != null) {
			boolean bullishEngulfing = previous.getClose() < previous.getOpen() && // 이전 음봉
					close > open && // 현재 양봉
					close > previous.getOpen() && open < previous.getClose();

			if (bullishEngulfing) {
				return CandlePattern.BULLISH_ENGULFING;
			}

			boolean bearishEngulfing = previous.getClose() > previous.getOpen() && // 이전 양봉
					close < open && // 현재 음봉
					close < previous.getOpen() && open > previous.getClose();

			if (bearishEngulfing) {
				return CandlePattern.BEARISH_ENGULFING;
			}
		}

		return CandlePattern.NONE;
	}

}
