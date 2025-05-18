// IndicatorComparison.tsx (한글 라벨 + 색상 강조 적용)

import React, { useState } from "react";
import {
  compareAllIndicators,
  AllComparisonResponse,
} from "@/service/compareAllIndicatorsService";
import { Candle, formatTimestampKST } from "@/shared/util/indicatorUtil";
import { Card, CardContent } from "@/components/ui/card";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs";

const API_URL = "https://fapi.binance.com/fapi/v1/klines";
const SYMBOL = "BTCUSDT";

const INTERVAL_MS = {
  "1m": 60_000,
  "3m": 3 * 60_000,
  "5m": 5 * 60_000,
  "15m": 15 * 60_000,
  "1h": 60 * 60_000,
  "4h": 4 * 60 * 60_000,
} as const;
type Interval = keyof typeof INTERVAL_MS;

const TABS: { key: string; label: string }[] = [
  { key: "rsi", label: "RSI" },
  { key: "stochRsi", label: "Stoch RSI" },
  { key: "vwbb", label: "VWBB" },
  { key: "candles", label: "Candles" },
];

const IndicatorComparison = () => {
  const [result, setResult] = useState<AllComparisonResponse | null>(null);
  const [interval, setInterval] = useState<Interval>("1m");

  const fetchLatestCandles = async (): Promise<Candle[]> => {
    const intervalMs = INTERVAL_MS[interval];
    const now = Date.now();
    const endTime = now - (now % intervalMs) + intervalMs;
    const startTime = endTime - 500 * intervalMs;
    const res = await fetch(
      `${API_URL}?symbol=${SYMBOL}&interval=${interval}&startTime=${startTime}&endTime=${endTime}`
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
    const res = await compareAllIndicators(SYMBOL, interval, candles);

    console.log(
      "마지막 캔들 시간 : ",
      formatTimestampKST(candles[candles.length - 1].time)
    );
    const filtered = Object.fromEntries(
      Object.entries(res.result).map(([key, value]) => {
        if (key === "vwbb" && typeof value === "object" && value !== null) {
          return [
            key,
            {
              upper: Array.isArray(value.upper)
                ? value.upper.filter(isValid).sort((a, b) => b.time - a.time)
                : [],
              lower: Array.isArray(value.lower)
                ? value.lower.filter(isValid).sort((a, b) => b.time - a.time)
                : [],
              basis: Array.isArray(value.basis)
                ? value.basis.filter(isValid).sort((a, b) => b.time - a.time)
                : [],
            },
          ];
        } else if (Array.isArray(value)) {
          return [key, value.filter(isValid).sort((a, b) => b.time - a.time)];
        }
        return [key, value];
      })
    );

    setResult({ ...filtered });
  };

  const getAvgDiff = (arr: any[], diffKey = "diff") => {
    const valid = arr
      .map((item) => item?.[diffKey])
      .filter((d) => typeof d === "number" && !isNaN(d));
    if (valid.length === 0) return null;
    const avg = valid.reduce((a, b) => a + b, 0) / valid.length;
    if (avg < 0.001) return "✅ 오차가 거의 없습니다.";
    if (avg < 1)
      return `⚠️ 약간의 오차가 있습니다. (평균오차: ${avg.toFixed(4)})`;
    return `❗ 오차가 높습니다. (평균오차: ${avg.toFixed(4)})`;
  };

  const hasContent = (v: any): boolean => {
    if (Array.isArray(v)) return v.length > 0;
    if (typeof v === "object" && v !== null)
      return Object.values(v).some(isValid);
    return false;
  };

  const renderColoredRow = (item: any) => {
    const diff =
      typeof item.diff === "number"
        ? item.diff.toFixed(4)
        : item.diff?.close !== undefined
        ? `Close: ${item.diff.close.toFixed(
            4
          )}, Vol: ${item.diff.volume?.toFixed(4)}`
        : "-";

    return (
      <div className="p-3 bg-gray-200 rounded text-xs font-mono">
        <div className="text-gray-500 mb-1">
          🕒 {formatTimestampKST(item.time)}
        </div>
        <div className="text-green-600">
          프론트: {JSON.stringify(item.frontend)}
        </div>
        <div className="text-purple-600">
          백엔드: {JSON.stringify(item.backend)}
        </div>
        <div className="font-bold text-blue-600">diff: {diff}</div>
      </div>
    );
  };

  return (
    <div className="p-6 text-black bg-gray-100 min-h-screen">
      <h1 className="text-2xl font-bold mb-4">
        📊 지표 비교 (백엔드 vs 프론트)
      </h1>

      <div className="mb-4 flex gap-2">
        {["1m", "3m", "5m", "15m", "1h", "4h"].map((v) => (
          <button
            key={v}
            onClick={() => setInterval(v as Interval)}
            className={`px-3 py-1 rounded font-semibold shadow text-xs ${
              interval === v
                ? "bg-yellow-400 text-black"
                : "bg-gray-700 text-white"
            }`}
          >
            {v}
          </button>
        ))}
        <button
          onClick={handleCompare}
          className="ml-4 px-5 py-2 bg-blue-600 hover:bg-blue-700 rounded shadow text-white"
        >
          🔍 비교 실행
        </button>
      </div>

      {result && (
        <Tabs defaultValue="rsi" className="w-full">
          <TabsList className="mb-4 flex flex-wrap gap-2 justify-start">
            {TABS.filter(
              ({ key }) => result[key] && hasContent(result[key])
            ).map(({ key, label }) => (
              <TabsTrigger
                key={key}
                value={key}
                className="font-semibold text-left px-4"
              >
                {label}
              </TabsTrigger>
            ))}
          </TabsList>

          {TABS.map(({ key, label }) => {
            const value = result[key];
            if (!value || !hasContent(value)) return null;

            let summary = null;
            if (key === "vwbb" && typeof value === "object") {
              summary = [
                `🔼 Upper: ${getAvgDiff(value.upper) ?? "데이터 없음"}`,
                `🔽 Lower: ${getAvgDiff(value.lower) ?? "데이터 없음"}`,
                `📊 Basis: ${getAvgDiff(value.basis) ?? "데이터 없음"}`,
              ].join("\n");
            } else if (key === "stochRsi") {
              summary = [
                `🟡 K: ${getAvgDiff(value, "kdiff") ?? "데이터 없음"}`,
                `🔵 D: ${getAvgDiff(value, "ddiff") ?? "데이터 없음"}`,
              ].join("\n");
            } else {
              summary = getAvgDiff(value);
            }

            return (
              <TabsContent key={key} value={key}>
                <Card className="bg-white text-sm">
                  <CardContent className="p-4 space-y-3">
                    <div className="text-base font-semibold text-green-600 mb-2 whitespace-pre-line">
                      {summary ?? "비교할 데이터가 없습니다."}
                    </div>

                    <div className="space-y-2">
                      {key === "vwbb" && typeof value === "object"
                        ? value.upper.map((_, i) => {
                            const u = value.upper[i];
                            const l = value.lower[i];
                            const b = value.basis[i];
                            if (!u || !l || !b) return null;
                            return (
                              <div
                                key={i}
                                className="p-3 bg-gray-200 rounded text-xs font-mono"
                              >
                                <pre>
                                  {JSON.stringify(
                                    {
                                      time: formatTimestampKST(u.time),
                                      upper: u,
                                      basis: b,
                                      lower: l,
                                    },
                                    null,
                                    2
                                  )}
                                </pre>
                              </div>
                            );
                          })
                        : key === "stochRsi"
                        ? value.map((item: any, idx: number) => (
                            <div
                              key={idx}
                              className="p-3 bg-gray-200 rounded text-xs font-mono"
                            >
                              <div className="text-gray-500 mb-1">
                                🕒 {formatTimestampKST(item.time)}
                              </div>
                              <div className="text-green-600">
                                프론트 K: {item.frontend_k}
                              </div>
                              <div className="text-purple-600">
                                백엔드 K: {item.backend_k}
                              </div>
                              <div
                                className={`font-bold ${
                                  item.kdiff > 10
                                    ? "text-red-500"
                                    : item.kdiff < 1
                                    ? "text-green-600"
                                    : "text-yellow-500"
                                }`}
                              >
                                K diff: {item.kdiff?.toFixed(4)}
                              </div>
                              <div className="text-green-600 mt-2">
                                프론트 D: {item.frontend_d}
                              </div>
                              <div className="text-purple-600">
                                백엔드 D: {item.backend_d}
                              </div>
                              <div
                                className={`font-bold ${
                                  item.ddiff > 10
                                    ? "text-red-500"
                                    : item.ddiff < 1
                                    ? "text-green-600"
                                    : "text-yellow-500"
                                }`}
                              >
                                D diff: {item.ddiff?.toFixed(4)}
                              </div>
                            </div>
                          ))
                        : Array.isArray(value)
                        ? value.map((item: any, idx: number) => (
                            <div key={idx}>{renderColoredRow(item)}</div>
                          ))
                        : null}
                    </div>
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
