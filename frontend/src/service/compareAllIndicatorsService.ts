// 파일: src/service/compareAllIndicatorsService.ts

import api from "@/shared/util/axios";
import { Candle } from "@/shared/util/indicatorUtil";

export interface AllComparisonResponse {
  rsi: ComparisonPoint[];
  sma: ComparisonPoint[];
  ema: ComparisonPoint[];
  vwbb: {
    upper: ComparisonPoint[];
    lower: ComparisonPoint[];
    basis: ComparisonPoint[];
  };
  stochRsi: StochRsiComparisonPoint[];
  candles: Candle[];
}

export interface ComparisonPoint {
  time: number;
  frontend: number;
  backend: number;
  diff: number;
}

export interface StochRsiComparisonPoint {
  time: number;
  kFrontend: number;
  kBackend: number;
  kDiff: number;
  dFrontend: number;
  dBackend: number;
  dDiff: number;
}

export const compareAllIndicators = async (
  symbol: string,
  timeframe: string,
  candles: Candle[]
): Promise<AllComparisonResponse> => {
  const res = await api.post<AllComparisonResponse>("/indicator/compare-all", {
    symbol,
    timeframe,
    candles,
  });
  return res.data;
};
