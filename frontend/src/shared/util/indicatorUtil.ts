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

export function calculateSMA(
  data: IndicatorPoint[],
  period: number
): IndicatorPoint[] {
  const result: IndicatorPoint[] = [];

  for (let i = 0; i < data.length; i++) {
    if (i < period - 1 || data[i].value == null) {
      result.push({ time: data[i].time, value: null });
      continue;
    }

    const window = data.slice(i - period + 1, i + 1);
    const valid = window.filter((d) => typeof d.value === "number");

    if (valid.length < period) {
      result.push({ time: data[i].time, value: null });
      continue;
    }

    const avg = valid.reduce((sum, d) => sum + d.value!, 0) / valid.length;

    result.push({ time: data[i].time, value: avg });
  }

  return result;
}

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

export function calculateStochRSI(
  data: Candle[],
  rsiPeriod = 14,
  stochPeriod = 14,
  kPeriod = 3,
  dPeriod = 3
): DualIndicatorPoint[] {
  const rsi = calculateRSI(data, rsiPeriod);
  const stochRsi: IndicatorPoint[] = [];

  for (let i = 0; i < rsi.length; i++) {
    if (i < stochPeriod - 1) {
      stochRsi.push({ time: rsi[i].time, value: null });
      continue;
    }

    const sliced = rsi.slice(i - stochPeriod + 1, i + 1);
    const minRsi = Math.min(...sliced.map((d) => d.value!));
    const maxRsi = Math.max(...sliced.map((d) => d.value!));

    const denominator = maxRsi - minRsi;
    if (denominator === 0) {
      stochRsi.push({ time: rsi[i].time, value: 0 });
    } else {
      const value = ((rsi[i].value! - minRsi) / denominator) * 100;
      stochRsi.push({ time: rsi[i].time, value });
    }
  }

  // ✅ Binance 기준으로 smoothing: SMA 사용
  const smoothedK = calculateSMA(stochRsi, kPeriod);
  const smoothedD = calculateSMA(smoothedK, dPeriod);

  const result: DualIndicatorPoint[] = [];
  for (let i = 0; i < stochRsi.length; i++) {
    result.push({
      time: stochRsi[i].time,
      k: smoothedK[i]?.value ?? null,
      d: smoothedD[i]?.value ?? null,
    });
  }

  return result;
}

export const calculateVWBB = (
  candles: Candle[],
  period = 20,
  multiplier = 2
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
      const volume = candles[j].volume ?? 1; // ✅ volumeWeightRatio 제거
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

export function formatTimestampKST(unixTime: number): string {
  const date = new Date(unixTime * 1000); // 초 단위 → 밀리초 단위로 변환
  if (isNaN(date.getTime())) return "Invalid time";

  return date.toLocaleString("ko-KR", {
    timeZone: "Asia/Seoul",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
}

export function calculateVWAP(candles: Candle[]): IndicatorPoint[] {
  const result: IndicatorPoint[] = [];
  let cumulativePV = 0;
  let cumulativeVolume = 0;

  for (let i = 0; i < candles.length; i++) {
    const candle = candles[i];
    const typicalPrice = (candle.high + candle.low + candle.close) / 3;
    cumulativePV += typicalPrice * candle.volume;
    cumulativeVolume += candle.volume;

    const vwap =
      cumulativeVolume === 0 ? null : cumulativePV / cumulativeVolume;
    result.push({ time: candle.time, value: vwap });
  }

  return result;
}
