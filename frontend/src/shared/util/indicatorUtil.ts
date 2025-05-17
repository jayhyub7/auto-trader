// 파일: src/shared/util/indicatorUtil.ts

export interface IndicatorPoint {
  time: number;
  value: number | null;
}

export interface DualIndicatorPoint {
  time: number;
  k: number | null;
  d: number | null;
}

export interface Candle {
  time: number;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

export const calculateEMA = (data: Candle[], period = 20): IndicatorPoint[] => {
  const k = 2 / (period + 1);
  let emaPrev = data[0].close;

  return data.map((d, i) => {
    if (i === 0) return { time: d.time, value: emaPrev };
    emaPrev = d.close * k + emaPrev * (1 - k);
    return { time: d.time, value: emaPrev };
  });
};

export const calculateSMA = (data: Candle[], period = 14): IndicatorPoint[] => {
  return data.map((d, i) => {
    if (i < period - 1) return { time: d.time, value: null };
    const sum = data
      .slice(i - period + 1, i + 1)
      .reduce((acc, cur) => acc + cur.close, 0);
    return { time: d.time, value: sum / period };
  });
};

export const calculateRSI = (data: Candle[], period = 14): IndicatorPoint[] => {
  let avgGain = 0;
  let avgLoss = 0;
  const result: IndicatorPoint[] = [];

  for (let i = 1; i < data.length; i++) {
    const diff = data[i].close - data[i - 1].close;
    const gain = diff > 0 ? diff : 0;
    const loss = diff < 0 ? -diff : 0;

    if (i <= period) {
      avgGain += gain;
      avgLoss += loss;
      result.push({ time: data[i].time, value: null });
    } else if (i === period + 1) {
      avgGain = avgGain / period;
      avgLoss = avgLoss / period;
      const rs = avgLoss === 0 ? 100 : avgGain / avgLoss;
      const rsi = 100 - 100 / (1 + rs);
      result.push({ time: data[i].time, value: rsi });
    } else {
      avgGain = (avgGain * (period - 1) + gain) / period;
      avgLoss = (avgLoss * (period - 1) + loss) / period;
      const rs = avgLoss === 0 ? 100 : avgGain / avgLoss;
      const rsi = 100 - 100 / (1 + rs);
      result.push({ time: data[i].time, value: rsi });
    }
  }

  return result;
};

export const calculateStochRSI = (
  data: Candle[],
  rsiPeriod = 14,
  stochPeriod = 14,
  kPeriod = 3,
  dPeriod = 3
): DualIndicatorPoint[] => {
  const rsi = calculateRSI(data, rsiPeriod);
  const stochK: (number | null)[] = new Array(rsi.length).fill(null);

  for (let i = stochPeriod; i < rsi.length; i++) {
    const slice = rsi.slice(i - stochPeriod + 1, i + 1).map((d) => d.value);
    const valid = slice.filter((v): v is number => v !== null);

    if (valid.length < stochPeriod) continue;

    const current = rsi[i].value;
    const min = Math.min(...valid);
    const max = Math.max(...valid);

    if (current != null && max !== min) {
      stochK[i] = ((current - min) / (max - min)) * 100;
    }
  }

  const smooth = (
    arr: (number | null)[],
    period: number
  ): (number | null)[] => {
    return arr.map((_, i) => {
      if (i < period - 1) return null;
      const values = arr
        .slice(i - period + 1, i + 1)
        .filter((v): v is number => v !== null);
      if (values.length < period) return null;
      return values.reduce((sum, v) => sum + v, 0) / period;
    });
  };

  const smoothedK = smooth(stochK, kPeriod);
  const smoothedD = smooth(smoothedK, dPeriod);

  return rsi.map((d, i) => ({
    time: d.time,
    k: smoothedK[i],
    d: smoothedD[i],
  }));
};

export const calculateVWBB = (
  candles: Candle[],
  period = 20,
  multiplier = 2,
  volumeWeightRatio = 0.5
): {
  upper: IndicatorPoint[];
  lower: IndicatorPoint[];
  basis: IndicatorPoint[];
} => {
  const vwma = candles.map((_, i) => {
    if (i < period - 1) return null;
    let volSum = 0;
    let priceVolSum = 0;
    for (let j = i - period + 1; j <= i; j++) {
      const price = candles[j].close;
      const volume = (candles[j].volume ?? 1) * volumeWeightRatio;
      volSum += volume;
      priceVolSum += price * volume;
    }
    return priceVolSum / volSum;
  });

  const std = candles.map((_, i) => {
    if (i < period - 1 || vwma[i] == null) return null;
    const mean = vwma[i]!;
    const variance =
      candles
        .slice(i - period + 1, i + 1)
        .reduce((sum, d) => sum + Math.pow(d.close - mean, 2), 0) / period;
    return Math.sqrt(variance);
  });

  return {
    basis: vwma.map((v, i) => ({ time: candles[i].time, value: v })),
    upper: vwma.map((v, i) =>
      v == null || std[i] == null
        ? { time: candles[i].time, value: null }
        : { time: candles[i].time, value: v + multiplier * std[i]! }
    ),
    lower: vwma.map((v, i) =>
      v == null || std[i] == null
        ? { time: candles[i].time, value: null }
        : { time: candles[i].time, value: v - multiplier * std[i]! }
    ),
  };
};

export const formatTimestampKST = (timestamp: number): string => {
  const date = new Date(timestamp * 1000); // ms로 변환
  const kstOffset = 9 * 60 * 60 * 1000; // UTC+9
  const kst = new Date(date.getTime() + kstOffset);
  return kst.toISOString().replace("T", " ").substring(0, 19);
};
