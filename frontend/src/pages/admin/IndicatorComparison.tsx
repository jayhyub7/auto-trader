// 파일: IndicatorComparison.tsx

import React, { useState } from "react";
import {
  compareFrontendIndicators,
  compareBackendIndicators,
  AllComparisonResponse,
} from "@/service/IndicatorComparisonService";
import { Candle, formatTimestampKST } from "@/shared/util/indicatorUtil";
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

const IndicatorComparison = () => {
  const [result, setResult] = useState<AllComparisonResponse["result"] | null>(
    null
  );
  const [interval, setInterval] = useState<Interval>("1m");
  const [executionMeta, setExecutionMeta] = useState<{
    type: string;
    time: number;
  } | null>(null);
  const [activeTab, setActiveTab] = useState<string | undefined>(undefined);

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

  const handleCompareFrontend = async () => {
    const res = await compareFrontendIndicators(SYMBOL, interval);
    const limitedResult: AllComparisonResponse["result"] = {};
    for (const key of Object.keys(res.result)) {
      const original = res.result[key];
      limitedResult[key] = original.slice(-30);
    }
    setResult(limitedResult);
    setExecutionMeta({ type: "프론트 계산", time: Date.now() });
  };

  const handleCompareBackend = async () => {
    const res = await compareBackendIndicators(SYMBOL, interval);
    setResult(res.result);
    setExecutionMeta({ type: "백엔드 계산", time: Date.now() });
  };

  const getDiffColor = (diff: number) => {
    if (diff > 10) return "text-red-500";
    if (diff < 1) return "text-green-600";
    return "text-yellow-500";
  };

  const renderTripleRow = (item: any, indicatorKey: string) => {
    const isVWBB = indicatorKey.toLowerCase() === "vwbb";
    const front = isVWBB ? item.frontend?.value ?? {} : item.frontend ?? {};
    const back = isVWBB ? item.backend?.value ?? {} : item.backend ?? {};
    const diff = isVWBB ? item.diff?.value ?? {} : item.diff ?? {};

    const keys = Object.keys(front);
    const time = item.time;
    const frontTime = formatTimestampKST(time);
    const backTime = formatTimestampKST(
      item.backend?.time ? item.backend.time / 1000 : time
    );
    const backendTimestamp = item.backend?.time ?? null;

    const timeDiff =
      typeof item.timeDiffSec === "number" && Number.isFinite(item.timeDiffSec)
        ? item.timeDiffSec.toString().padStart(2, "0")
        : "--";

    return (
      <div
        key={time}
        className="grid grid-cols-3 gap-2 border rounded bg-white text-xs font-mono p-4"
      >
        {/* 프론트 */}
        <div>
          <div className="font-bold text-gray-700">프론트</div>
          <div>{frontTime}</div>
          <div className="text-gray-500">(TIMESTAMP: {Math.floor(time)})</div>
          {keys.map((k) => (
            <div key={k} className="text-green-600">
              {k}: {front[k]?.toFixed?.(4) ?? "--"}
            </div>
          ))}
        </div>

        {/* 지표비교 */}
        <div>
          <div className="font-bold text-gray-700">지표비교</div>
          <div>시간차: 00:00:{timeDiff}</div>
          <div className="text-gray-500">
            TIMESTAMP: {backendTimestamp ? Math.floor(backendTimestamp) : "--"}
          </div>
          {keys
            .filter((k) => k !== "time")
            .map((k) => (
              <div
                key={k}
                className={`${getDiffColor(
                  Math.abs(diff[k + "diff"]) ?? 0
                )} font-bold`}
              >
                {k} diff: {(Math.abs(diff[k + "diff"]) ?? 0).toFixed(4)}
              </div>
            ))}
        </div>

        {/* 백엔드 */}
        <div>
          <div className="font-bold text-gray-700">백엔드</div>
          <div>{backTime}</div>
          <div className="text-gray-500">
            (TIMESTAMP: {backendTimestamp ? Math.floor(backendTimestamp) : "--"}
            )
          </div>
          {keys.map((k) => (
            <div key={k} className="text-purple-600">
              {k}: {back[k]?.toFixed?.(4) ?? "--"}
            </div>
          ))}
        </div>
      </div>
    );
  };

  return (
    <div className="text-black bg-gray-100 min-h-screen">
      {/* 상단 고정 영역 */}
      <div className="sticky top-0 z-50 bg-gray-100 p-4 shadow-md">
        <h1 className="text-2xl font-bold mb-2">
          📊 지표 비교 (백엔드 vs 프론트)
        </h1>

        {/* interval + 버튼 */}
        <div className="mb-2 flex gap-2 flex-wrap">
          {Object.keys(INTERVAL_MS).map((v) => (
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
            onClick={handleCompareFrontend}
            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 rounded shadow text-white"
          >
            🔍 프론트 계산 후 비교
          </button>

          <button
            onClick={handleCompareBackend}
            className="px-4 py-2 bg-purple-600 hover:bg-purple-700 rounded shadow text-white"
          >
            🔁 백엔드 계산 요청 후 비교
          </button>
        </div>

        {/* 지표 탭 */}
        {result && (
          <Tabs
            value={activeTab ?? Object.keys(result)[0]}
            onValueChange={setActiveTab}
            className="w-full"
          >
            <TabsList className="mb-2 flex flex-wrap gap-2 justify-start">
              {Object.keys(result).map((key) => (
                <TabsTrigger
                  key={key}
                  value={key}
                  className="font-semibold text-left px-4 capitalize"
                >
                  {key}
                </TabsTrigger>
              ))}
            </TabsList>

            {Object.entries(result).map(([key, value]: any) => {
              const sorted = Array.isArray(value)
                ? [...value].sort((a, b) => b.time - a.time)
                : [];

              return (
                <TabsContent key={key} value={key}>
                  <div className="p-4 space-y-4 overflow-y-auto max-h-[calc(100vh-180px)]">
                    <div className="text-base font-semibold text-green-600 mb-2">
                      📌 {key.toUpperCase()} 지표 비교 결과
                    </div>
                    {sorted.length > 0
                      ? sorted.map((item) => renderTripleRow(item, key))
                      : "데이터 없음"}
                  </div>
                </TabsContent>
              );
            })}
          </Tabs>
        )}

        {/* 실행 정보 */}
        {executionMeta && (
          <div className="text-sm text-gray-700">
            <span className="font-semibold">실행 유형:</span>{" "}
            {executionMeta.type} &nbsp;&nbsp;|&nbsp;&nbsp;
            <span className="font-semibold">실행 시각:</span>{" "}
            {formatTimestampKST(Math.floor(executionMeta.time / 1000))} (
            {Math.floor(executionMeta.time / 1000)})
          </div>
        )}
      </div>
    </div>
  );
};

export default IndicatorComparison;
