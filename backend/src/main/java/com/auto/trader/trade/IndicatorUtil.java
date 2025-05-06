package com.auto.trader.trade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.auto.trader.trade.dto.CandleDto;

import lombok.AllArgsConstructor;
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
	    }
	    @Getter
	    public static class DualIndicatorPoint {
	        private long time;
	        private Double k;
	        private Double d;
	        // getters/setters
	        public DualIndicatorPoint(long time, Double k, Double d) {
	            this.time = time;
	            this.k = k;
	            this.d = d;
	        }
	    }
	    @Getter
	    public static class VWBB {
	        private List<IndicatorPoint> upper;
	        private List<IndicatorPoint> lower;
	        private List<IndicatorPoint> basis;
	        // getters/setters
	        private VWBB(List<IndicatorPoint> upper, List<IndicatorPoint> lower, List<IndicatorPoint> basis) {
	        	this.upper = upper;
	        	this.lower = lower;
	        	this.basis = basis;
	        }
	    }

	public static List<IndicatorPoint> calculateRSI(List<CandleDto> candles, int period) {
		List<IndicatorPoint> rsiPoints = new ArrayList<>();
		if (candles.size() < period + 1)
			return rsiPoints;

		double gainSum = 0;
		double lossSum = 0;

		for (int i = 1; i <= period; i++) {
			double diff = candles.get(i).getClose() - candles.get(i - 1).getClose();
			if (diff >= 0)
				gainSum += diff;
			else
				lossSum += -diff;
		}

		double avgGain = gainSum / period;
		double avgLoss = lossSum / period;
		double rs = avgLoss == 0 ? 100 : avgGain / avgLoss;
		rsiPoints.add(new IndicatorPoint(candles.get(period).getTime(), 100 - (100 / (1 + rs))));

		for (int i = period + 1; i < candles.size(); i++) {
			double diff = candles.get(i).getClose() - candles.get(i - 1).getClose();
			double gain = diff > 0 ? diff : 0;
			double loss = diff < 0 ? -diff : 0;

			avgGain = ((avgGain * (period - 1)) + gain) / period;
			avgLoss = ((avgLoss * (period - 1)) + loss) / period;
			rs = avgLoss == 0 ? 100 : avgGain / avgLoss;

			double rsi = 100 - (100 / (1 + rs));
			rsiPoints.add(new IndicatorPoint(candles.get(i).getTime(), rsi));
		}

		return rsiPoints;
	}

	public static List<DualIndicatorPoint> calculateStochRSI(List<CandleDto> candles, int period, int signalPeriod) {
		List<IndicatorPoint> rsi = calculateRSI(candles, period);
		List<Double> stochK = new ArrayList<>(Collections.nCopies(rsi.size(), null));

		for (int i = period * 2; i < rsi.size(); i++) {
			List<Double> window = new ArrayList<>();
			for (int j = i - period + 1; j <= i; j++) {
				Double v = rsi.get(j).getValue();
				if (v != null)
					window.add(v);
			}

			if (window.size() < period)
				continue;

			double min = Collections.min(window);
			double max = Collections.max(window);
			Double current = rsi.get(i).getValue();

			if (current != null && max != min) {
				stochK.set(i, (current - min) / (max - min) * 100);
			}
		}

		List<DualIndicatorPoint> result = new ArrayList<>();
		for (int i = 0; i < rsi.size(); i++) {
			Double k = stochK.get(i);
			Double dVal = null;

			if (k != null && i >= signalPeriod) {
				List<Double> recentK = stochK.subList(i - signalPeriod + 1, i + 1).stream().filter(Objects::nonNull)
						.collect(Collectors.toList());

				if (recentK.size() == signalPeriod) {
					dVal = recentK.stream().mapToDouble(v -> v).average().orElse(0);
				}
			}

			result.add(new DualIndicatorPoint(rsi.get(i).getTime(), k, dVal));
		}

		return result;
	}

	public static List<IndicatorPoint> calculateEMA(List<CandleDto> candles, int period) {
		List<IndicatorPoint> result = new ArrayList<>();
		if (candles.isEmpty())
			return result;

		double k = 2.0 / (period + 1);
		double emaPrev = candles.get(0).getClose();

		for (int i = 0; i < candles.size(); i++) {
			CandleDto c = candles.get(i);
			if (i == 0) {
				result.add(new IndicatorPoint(c.getTime(), emaPrev));
			} else {
				emaPrev = c.getClose() * k + emaPrev * (1 - k);
				result.add(new IndicatorPoint(c.getTime(), emaPrev));
			}
		}

		return result;
	}

	public static List<IndicatorPoint> calculateSMA(List<CandleDto> candles, int period) {
		List<IndicatorPoint> result = new ArrayList<>();

		for (int i = 0; i < candles.size(); i++) {
			if (i < period - 1) {
				result.add(new IndicatorPoint(candles.get(i).getTime(), null));
			} else {
				double sum = 0;
				for (int j = i - period + 1; j <= i; j++) {
					sum += candles.get(j).getClose();
				}
				double sma = sum / period;
				result.add(new IndicatorPoint(candles.get(i).getTime(), sma));
			}
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
				double volume = candles.get(j).getVolume() != 0 ? candles.get(j).getVolume() : 1;
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
			upper.add(new IndicatorPoint(time,
					(mean != null && deviation != null) ? mean + multiplier * deviation : null));
			lower.add(new IndicatorPoint(time,
					(mean != null && deviation != null) ? mean - multiplier * deviation : null));
		}

		return new VWBB(upper, lower, basis);
	}

}
