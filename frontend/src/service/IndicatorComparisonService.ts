// 📁 src/service/IndicatorComparisonService.ts

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

// 캔들 조회
const fetchLatestCandles = async (
  symbol: string,
  interval: string
): Promise<Candle[]> => {
  const intervalMsMap: Record<string, number> = {
    "1m": 60_000,
    "3m": 3 * 60_000,
    "5m": 5 * 60_000,
    "15m": 15 * 60_000,
    "1h": 60 * 60_000,
    "4h": 4 * 60 * 60_000,
  };
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

// 프론트 지표 계산 후 백엔드 캐시값과 비교
export const compareFrontendIndicators = async (
  symbol: string,
  interval: string
): Promise<AllComparisonResponse> => {
  const candles = await fetchLatestCandles(symbol, interval);

  // 프론트 지표 계산
  const rsi = calculateRSI(candles);
  const stochrsi = calculateStochRSI(candles);
  const vwbb = calculateVWBB(candles);

  const vwbbList = vwbb.upper.map((v, i) => ({
    time: v.time,
    upper: v.value,
    basis: vwbb.basis[i]?.value ?? 0,
    lower: vwbb.lower[i]?.value ?? 0,
  }));

  const frontendIndicators: Record<string, any[]> = {
    rsi,
    stochrsi,
    vwbb: vwbbList,
  };

  // 백엔드 캐시 지표 불러오기
  const res = await api.post<Record<string, any[]>>(
    "/indicator/fetch-cached-indicators",
    { symbol, interval }
  );
  const backendIndicators = res.data;

  // 비교 결과 구성
  const result: Record<string, any[]> = {};

  for (const key of Object.keys(frontendIndicators)) {
    const front = frontendIndicators[key];
    const back = backendIndicators[key] ?? [];

    const merged = front.map((fItem) => {
      const time = fItem.time;
      const bItem = back.find((b) => Math.floor(b.time / 1000) === time) || {};

      let timeDiffSec = null;
      if (typeof bItem.time === "number" && !isNaN(bItem.time)) {
        const backendTimeSec =
          bItem.time >= 1e12 ? Math.floor(bItem.time / 1000) : bItem.time;
        timeDiffSec = Math.abs(time - backendTimeSec);
      }

      const mergedItem: any = {
        time,
        frontend: fItem,
        backend: bItem,
        diff: {},
        timeDiffSec,
      };

      for (const k of Object.keys(fItem)) {
        if (k === "time") continue;
        const fVal = fItem[k] ?? 0;
        const bVal = bItem[k] ?? 0;
        const rawDiff = Math.abs(fVal - bVal);
        mergedItem.diff[k + "diff"] = rawDiff < 1e-8 ? 0 : +rawDiff.toFixed(6);
      }

      return mergedItem;
    });

    result[key] = merged;
  }

  return { result };
};

// 백엔드 기준 지표를 계산해서 캐시와 비교
export const compareBackendIndicators = async (
  symbol: string,
  interval: string
): Promise<AllComparisonResponse> => {
  const res = await api.post<AllComparisonResponse>(
    "/indicator/compare-backend",
    {
      symbol,
      interval,
    }
  );
  return res.data;
};
