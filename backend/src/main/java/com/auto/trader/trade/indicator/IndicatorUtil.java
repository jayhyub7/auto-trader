package com.auto.trader.trade.indicator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
  }

  @Getter
  public static class VWBB {
    private List<IndicatorPoint> upper;
    private List<IndicatorPoint> lower;
    private List<IndicatorPoint> basis;

    private VWBB(List<IndicatorPoint> upper, List<IndicatorPoint> lower,
        List<IndicatorPoint> basis) {
      this.upper = upper;
      this.lower = lower;
      this.basis = basis;
    }
  }

  // ✅ 실전 기준 RSI
  public static List<IndicatorPoint> calculateRSI(List<CandleDto> candles, int period) {
    List<IndicatorPoint> rsiPoints = new ArrayList<>();
    if (candles.size() < period + 1)
      return rsiPoints;

    double avgGain = 0;
    double avgLoss = 0;

    for (int i = 1; i <= period; i++) {
      double diff = candles.get(i).getClose() - candles.get(i - 1).getClose();
      if (diff > 0)
        avgGain += diff;
      else
        avgLoss -= diff;
      rsiPoints.add(new IndicatorPoint(candles.get(i).getTime(), null));
    }

    avgGain /= period;
    avgLoss /= period;

    for (int i = period + 1; i < candles.size(); i++) {
      double diff = candles.get(i).getClose() - candles.get(i - 1).getClose();
      double gain = diff > 0 ? diff : 0;
      double loss = diff < 0 ? -diff : 0;

      avgGain = (avgGain * (period - 1) + gain) / period;
      avgLoss = (avgLoss * (period - 1) + loss) / period;

      double rs = avgLoss == 0 ? 100 : avgGain / avgLoss;
      double rsi = 100 - (100 / (1 + rs));
      rsiPoints.add(new IndicatorPoint(candles.get(i).getTime(), rsi));
    }

    return rsiPoints;
  }

  // ✅ 실전 기준 StochRSI + smoothing
  public static List<DualIndicatorPoint> calculateStochRSI(List<CandleDto> candles, int rsiPeriod,
      int stochPeriod, int kPeriod, int dPeriod) {

    List<IndicatorPoint> rsi = calculateRSI(candles, rsiPeriod);
    List<Double> stochK = new ArrayList<>(Collections.nCopies(rsi.size(), null));

    for (int i = stochPeriod; i < rsi.size(); i++) {
      List<Double> window = rsi.subList(i - stochPeriod + 1, i + 1).stream()
          .map(IndicatorPoint::getValue).filter(Objects::nonNull).collect(Collectors.toList());

      if (window.size() < stochPeriod)
        continue;

      double min = Collections.min(window);
      double max = Collections.max(window);
      Double current = rsi.get(i).getValue();

      if (current != null && max != min) {
        stochK.set(i, ((current - min) / (max - min)) * 100);
      }
    }

    List<Double> smoothK = smooth(stochK, kPeriod);
    List<Double> smoothD = smooth(smoothK, dPeriod);

    List<DualIndicatorPoint> result = new ArrayList<>();
    for (int i = 0; i < rsi.size(); i++) {
      result.add(new DualIndicatorPoint(rsi.get(i).getTime(), smoothK.get(i), smoothD.get(i)));
    }

    return result;
  }

  private static List<Double> smooth(List<Double> data, int period) {
    List<Double> smoothed = new ArrayList<>(Collections.nCopies(data.size(), null));
    for (int i = period - 1; i < data.size(); i++) {
      List<Double> window = data.subList(i - period + 1, i + 1).stream().filter(Objects::nonNull)
          .collect(Collectors.toList());
      if (window.size() < period)
        continue;
      double avg = window.stream().mapToDouble(d -> d).average().orElse(0);
      smoothed.set(i, avg);
    }
    return smoothed;
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

    double volumeWeightRatio = 0.5;

    for (int i = period - 1; i < size; i++) {
      double volSum = 0, priceVolSum = 0;
      for (int j = i - period + 1; j <= i; j++) {
        double price = candles.get(j).getClose();
        double volume =
            (candles.get(j).getVolume() != 0 ? candles.get(j).getVolume() : 1) * volumeWeightRatio;
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
