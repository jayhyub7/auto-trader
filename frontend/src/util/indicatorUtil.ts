export interface Candle {
  time: number;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

export interface LinePoint {
  time: number;
  value: number | null;
}

export interface DualLinePoint {
  time: number;
  k: number | null;
  d: number | null;
}

export const calculateEMA = (data: Candle[], period = 20): LinePoint[] => {
  const k = 2 / (period + 1);
  let emaPrev = data[0].close;

  return data.map((d, i) => {
    if (i === 0) return { time: d.time, value: emaPrev };
    emaPrev = d.close * k + emaPrev * (1 - k);
    return { time: d.time, value: emaPrev };
  });
};

export const calculateSMA = (data: Candle[], period = 14): LinePoint[] => {
  return data.map((d, i) => {
    if (i < period - 1) return { time: d.time, value: null };
    const sum = data.slice(i - period + 1, i + 1).reduce((acc, cur) => acc + cur.close, 0);
    return { time: d.time, value: sum / period };
  });
};

export const calculateRSI = (data: Candle[], period = 14): LinePoint[] => {
  let gains = 0;
  let losses = 0;

  for (let i = 1; i <= period; i++) {
    const diff = data[i].close - data[i - 1].close;
    if (diff >= 0) gains += diff;
    else losses -= diff;
  }

  let avgGain = gains / period;
  let avgLoss = losses / period;

  const result: LinePoint[] = data.map((d, i) => ({ time: d.time, value: null }));
  for (let i = period + 1; i < data.length; i++) {
    const diff = data[i].close - data[i - 1].close;
    const gain = diff >= 0 ? diff : 0;
    const loss = diff < 0 ? -diff : 0;

    avgGain = (avgGain * (period - 1) + gain) / period;
    avgLoss = (avgLoss * (period - 1) + loss) / period;

    const rs = avgLoss === 0 ? 100 : avgGain / avgLoss;
    const rsi = 100 - 100 / (1 + rs);

    result[i] = { time: data[i].time, value: rsi };
  }

  return result;
};

export const calculateStochRSI = (data: Candle[], period = 14, signalPeriod = 3): DualLinePoint[] => {
  const rsi = calculateRSI(data, period);
  const stochK: (number | null)[] = new Array(rsi.length).fill(null);

  for (let i = period * 2; i < rsi.length; i++) {
    const slice = rsi.slice(i - period + 1, i + 1);
    const values = slice.map((d) => d.value).filter((v): v is number => v !== null);
    if (values.length < period) continue;

    const min = Math.min(...values);
    const max = Math.max(...values);
    const current = rsi[i].value;

    if (current !== null && max !== min) {
      stochK[i] = ((current - min) / (max - min)) * 100;
    }
  }

  const result: DualLinePoint[] = rsi.map((d, i) => {
    const k = stochK[i];
    let dVal: number | null = null;

    if (k !== null && i >= signalPeriod) {
      const recentK = stochK.slice(i - signalPeriod + 1, i + 1).filter((v): v is number => v !== null);
      if (recentK.length === signalPeriod) {
        dVal = recentK.reduce((sum, v) => sum + v, 0) / signalPeriod;
      }
    }

    return {
      time: d.time,
      k,
      d: dVal,
    };
  });

  return result;
};

// VWBB 계산
// ✅ 프론트 VWBB 계산 (가중 이동평균 + 가중 표준편차 기반)
export const calculateVWBB = (
  candles: Candle[],
  period = 20,
  multiplier = 2,
  volumeWeightRatio = 0.5 // ⬅️ 비율 조정값 추가
): {
  upper: LinePoint[];
  lower: LinePoint[];
  basis: LinePoint[];
} => {
  const vwma = candles.map((_, i) => {

    if (i < period - 1) return null;
    let volSum = 0;
    let priceVolSum = 0;
    for (let j = i - period + 1; j <= i; j++) {
      const price = candles[j].close;
      const volume = (candles[j].volume ?? 1) * volumeWeightRatio; // ⬅️ 비율 조정 적용
      volSum += volume;
      priceVolSum += price * volume;
    }
    return priceVolSum / volSum;
  });

  const std = candles.map((_, i) => {
    if (i < period - 1 || vwma[i] == null) return null;
    const mean = vwma[i]!;
    const variance = candles
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


/*
export const calculateVWBB = (candles: Candle[], period = 20, multiplier = 2): {
  upper: LinePoint[];
  lower: LinePoint[];
  basis: LinePoint[];
} => {
  const vwma = candles.map((_, i) => {
    if (i < period - 1) return null;
    let volSum = 0;
    let priceVolSum = 0;
    for (let j = i - period + 1; j <= i; j++) {
      const price = candles[j].close;
      const volume = candles[j].volume ?? 1; // 없으면 기본값 1
      volSum += volume;
      priceVolSum += price * volume;
    }
    return priceVolSum / volSum;
  });

  const std = candles.map((_, i) => {
    if (i < period - 1) return null;
    const mean = vwma[i]!;
    const variance = candles
      .slice(i - period + 1, i + 1)
      .reduce((sum, d) => sum + Math.pow(d.close - mean, 2), 0) / period;
    return Math.sqrt(variance);
  });

  return {
    basis: vwma.map((v, i) => (v == null ? { time: candles[i].time, value: null } : { time: candles[i].time, value: v })),
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
*/
