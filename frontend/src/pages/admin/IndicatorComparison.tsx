// IndicatorComparison.tsx

import React, { useState } from "react";
import {
  compareAllIndicators,
  AllComparisonResponse,
} from "@/service/compareAllIndicatorsService";
import { Candle, formatTimestampKST } from "@/shared/util/indicatorUtil";
import { Card, CardContent } from "@/components/ui/card";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs";

// ✅ 차트와 동일한 구조
const API_URL = "https://api.binance.com/api/v3/klines";
const SYMBOL = "BTCUSDT";
const INTERVAL = "1m";

const IndicatorComparison = () => {
  const [result, setResult] = useState<AllComparisonResponse | null>(null);

  const fetchLatestCandles = async (): Promise<Candle[]> => {
    const now = Date.now();
    const endTime = now - (now % 60000);
    const startTime = endTime - 500 * 60 * 1000;
    const res = await fetch(
      `${API_URL}?symbol=${SYMBOL}&interval=${INTERVAL}&startTime=${startTime}&endTime=${endTime}`
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

  const isValid = (item: any): boolean => {
    if (!item || typeof item !== "object") return false;
    return Object.values(item).every(
      (v) => v !== null && v !== "NaN" && !Number.isNaN(v)
    );
  };

  const handleCompare = async () => {
    const candles = await fetchLatestCandles();
    const res = await compareAllIndicators(SYMBOL, INTERVAL, candles);

    const filtered = Object.fromEntries(
      Object.entries(res).map(([key, value]) => {
        if (key === "vwbb" && typeof value === "object" && value !== null) {
          return [
            key,
            {
              upper: Array.isArray(value.upper)
                ? value.upper.filter(isValid)
                : [],
              lower: Array.isArray(value.lower)
                ? value.lower.filter(isValid)
                : [],
              basis: Array.isArray(value.basis)
                ? value.basis.filter(isValid)
                : [],
            },
          ];
        } else if (Array.isArray(value)) {
          return [key, value.filter(isValid)];
        }
        return [key, value];
      })
    );

    setResult({ ...filtered });
  };

  const getSummaryMessage = (key: string, value: any): string => {
    const getAvgDiff = (arr: any[], diffKey = "diff") => {
      const valid = arr
        .map((item) => item?.[diffKey])
        .filter((d) => typeof d === "number" && !isNaN(d));
      if (valid.length === 0) return null;
      const avg = valid.reduce((a, b) => a + b, 0) / valid.length;

      let message = "";
      if (avg < 0.001) message = "✅ 오차가 거의 없습니다.";
      else if (avg < 1) message = "⚠️ 약간의 오차가 있습니다.";
      else message = "❗ 오차가 높습니다.";

      return `${message} (평균오차: ${avg.toFixed(4)})`;
    };

    if (key === "vwbb" && typeof value === "object" && value !== null) {
      const upperMsg = getAvgDiff(value.upper);
      const lowerMsg = getAvgDiff(value.lower);
      const basisMsg = getAvgDiff(value.basis);

      return [
        `🔼 Upper: ${upperMsg ?? "데이터 없음"}`,
        `🔽 Lower: ${lowerMsg ?? "데이터 없음"}`,
        `📊 Basis: ${basisMsg ?? "데이터 없음"}`,
      ].join("\n");
    }

    if (key === "stochRsi" && Array.isArray(value)) {
      const kMsg = getAvgDiff(value, "kdiff");
      const dMsg = getAvgDiff(value, "ddiff");

      return [
        `🟡 K: ${kMsg ?? "데이터 없음"}`,
        `🔵 D: ${dMsg ?? "데이터 없음"}`,
      ].join("\n");
    }

    if (!Array.isArray(value) || value.length === 0) {
      return "비교할 데이터가 없습니다.";
    }

    const diffMsg = getAvgDiff(value);
    return diffMsg ?? "비교할 데이터가 없습니다.";
  };

  return (
    <div className="p-6 text-white bg-black min-h-screen">
      <h1 className="text-2xl font-bold mb-4">
        📊 지표 비교 (백엔드 vs 프론트)
      </h1>
      <button
        onClick={handleCompare}
        className="mb-6 px-5 py-2 bg-blue-600 hover:bg-blue-700 rounded shadow"
      >
        🔍 비교 실행
      </button>

      {result && (
        <Tabs defaultValue="rsi" className="w-full">
          <TabsList className="mb-4 flex flex-wrap gap-2 justify-start">
            {Object.keys(result).map((key) => (
              <TabsTrigger
                key={key}
                value={key}
                className="font-semibold text-left px-4"
              >
                {key.toUpperCase()}
              </TabsTrigger>
            ))}
          </TabsList>

          {Object.entries(result).map(([key, value]) => {
            let sortedValue = value;
            if (key === "vwbb" && typeof value === "object") {
              sortedValue = {
                upper: [...value.upper].sort((a, b) => b.time - a.time),
                lower: [...value.lower].sort((a, b) => b.time - a.time),
                basis: [...value.basis].sort((a, b) => b.time - a.time),
              };
            } else if (Array.isArray(value) && value[0]?.time) {
              sortedValue = [...value].sort((a, b) => b.time - a.time);
            }

            return (
              <TabsContent key={key} value={key}>
                <Card className="bg-zinc-900 text-sm">
                  <CardContent className="p-4 space-y-3">
                    <div className="text-base font-semibold text-green-400 mb-2 whitespace-pre-line">
                      {getSummaryMessage(key, sortedValue)}
                    </div>

                    {key === "vwbb" && typeof sortedValue === "object" ? (
                      sortedValue.upper.map((_, idx: number) => {
                        const upper = sortedValue.upper[idx];
                        const lower = sortedValue.lower[idx];
                        const basis = sortedValue.basis[idx];
                        if (!upper || !lower || !basis) return null;

                        return (
                          <div
                            key={`vwbb-${idx}`}
                            className="p-3 bg-zinc-800 rounded-md text-gray-200 font-mono mb-2"
                          >
                            <pre className="whitespace-pre-wrap text-gray-300 text-xs">
                              {JSON.stringify(
                                {
                                  time: `${upper.time} (${formatTimestampKST(
                                    upper.time
                                  )})`,
                                  upperfrontend: upper.frontend,
                                  upperbackend: upper.backend,
                                  upperdiff: upper.diff,
                                  basisfrontend: basis.frontend,
                                  basisbackend: basis.backend,
                                  basisdiff: basis.diff,
                                  lowerfrontend: lower.frontend,
                                  lowerbackend: lower.backend,
                                  lowerdiff: lower.diff,
                                },
                                null,
                                2
                              )}
                            </pre>
                          </div>
                        );
                      })
                    ) : Array.isArray(sortedValue) ? (
                      sortedValue.map((item: any, idx: number) => (
                        <div
                          key={idx}
                          className="p-3 bg-zinc-800 rounded-md text-gray-200 font-mono"
                        >
                          <pre className="whitespace-pre-wrap text-gray-300 text-xs">
                            {JSON.stringify(
                              item.time
                                ? {
                                    ...item,
                                    time: `${item.time} (${formatTimestampKST(
                                      item.time
                                    )})`,
                                  }
                                : item,
                              null,
                              2
                            )}
                          </pre>
                        </div>
                      ))
                    ) : (
                      <pre className="whitespace-pre-wrap text-gray-300">
                        {JSON.stringify(sortedValue, null, 2)}
                      </pre>
                    )}
                  </CardContent>
                </Card>
              </TabsContent>
            );
          })}
        </Tabs>
      )}
    </div>
  );
};

export default IndicatorComparison;
