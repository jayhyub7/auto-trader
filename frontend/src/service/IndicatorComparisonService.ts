// 포지션 비교용 서비스
// 파일: src/service/IndicatorComparisonService.ts

import api from "@/shared/util/axios";
import {
  Candle,
  calculateRSI,
  calculateStochRSI,
  calculateVWBB,
} from "@/shared/util/indicatorUtil";

export interface AllComparisonResponse {
  result: {
    [key: string]: any[];
  };
}

const intervalMsMap: Record<string, number> = {
  "1m": 60_000,
  "3m": 3 * 60_000,
  "5m": 5 * 60_000,
  "15m": 15 * 60_000,
  "1h": 60 * 60_000,
  "4h": 4 * 60 * 60_000,
};

const fetchLatestCandles = async (
  symbol: string,
  interval: string
): Promise<Candle[]> => {
  const intervalMs = intervalMsMap[interval];
  const now = Date.now();
  const endTime = now - (now % intervalMs) + intervalMs;
  const startTime = endTime - 500 * intervalMs;

  const res = await fetch(
    `https://fapi.binance.com/fapi/v1/klines?symbol=${symbol}&interval=${interval}&startTime=${startTime}&endTime=${endTime}`
  );
  const raw = await res.json();

  return raw.map((d: any) => ({
    time: d[0] / 1000,
    open: +d[1],
    high: +d[2],
    low: +d[3],
    close: +d[4],
    volume: +d[5],
  }));
};

export const compareFrontendIndicators = async (
  symbol: string,
  interval: string
): Promise<AllComparisonResponse> => {
  const candles = await fetchLatestCandles(symbol, interval);

  const frontRsi = calculateRSI(candles);
  const frontStoch = calculateStochRSI(candles);
  const frontVwbb = calculateVWBB(candles);

  const frontResult = mergeIndicators({
    rsi: frontRsi,
    stochrsi: frontStoch,
    vwbb: frontVwbb,
  });

  const backendRes = await api.post("/indicator/backend-indicator", {
    symbol,
    interval,
  });

  const backend = backendRes.data;
  const backendResult = mergeIndicators({
    rsi: backend.rsi,
    stochrsi: backend.stochRSI,
    vwbb: backend.vwbb,
  });

  const result = {
    frontend: frontResult,
    backend: backendResult,
  };

  return { result };
};

export const compareBackendIndicators = async (
  symbol: string,
  interval: string
): Promise<AllComparisonResponse> => {
  const candles = await fetchLatestCandles(symbol, interval);

  const frontRsi = calculateRSI(candles).slice(-30);
  const frontStoch = calculateStochRSI(candles).slice(-30);
  const vwbbFull = calculateVWBB(candles);
  const frontVwbb = {
    upper: vwbbFull.upper.slice(-30),
    basis: vwbbFull.basis.slice(-30),
    lower: vwbbFull.lower.slice(-30),
  };

  const frontend = mergeIndicators({
    rsi: frontRsi,
    stochrsi: frontStoch,
    vwbb: frontVwbb,
  });

  const backendRes = await api.post("/indicator/backend-indicator", {
    symbol,
    interval,
  });

  const backend = mergeIndicators({
    rsi: backendRes.data.rsi,
    stochrsi: backendRes.data.stochRSI,
    vwbb: backendRes.data.vwbb,
  });

  return {
    result: {
      frontend,
      backend,
    },
  };
};

function mergeIndicators(sources: {
  rsi?: { time: number; value: number }[];
  stochrsi?: { time: number; k: number; d: number }[];
  vwbb?: {
    upper: { time: number; value: number }[];
    basis: { time: number; value: number }[];
    lower: { time: number; value: number }[];
  };
}) {
  const rsi: { time: number; value: number }[] = [];
  const stochrsi: { time: number; k: number; d: number }[] = [];
  const vwbb: {
    time: number;
    upper?: number;
    basis?: number;
    lower?: number;
  }[] = [];

  if (sources.rsi) {
    for (const item of sources.rsi) {
      rsi.push({ time: item.time, value: item.value });
    }
  }

  if (sources.stochrsi) {
    for (const item of sources.stochrsi) {
      stochrsi.push({ time: item.time, k: item.k, d: item.d });
    }
  }

  if (sources.vwbb) {
    const upper = sources.vwbb.upper ?? [];
    const basis = sources.vwbb.basis ?? [];
    const lower = sources.vwbb.lower ?? [];
    const size = Math.min(upper.length, basis.length, lower.length);

    for (let i = 0; i < size; i++) {
      vwbb.push({
        time: upper[i].time,
        upper: upper[i].value,
        basis: basis[i].value,
        lower: lower[i].value,
      });
    }
  }

  return {
    rsi,
    stochrsi,
    vwbb,
  };
}

function computeDiff(
  front: Record<string, number>,
  back: Record<string, number>
) {
  const diff: Record<string, number> = {};
  for (const key of Object.keys(front)) {
    const fv = front[key] ?? 0;
    const bv = back[key] ?? 0;
    diff[key + "diff"] = Math.abs(fv - bv);
  }
  return diff;
}
