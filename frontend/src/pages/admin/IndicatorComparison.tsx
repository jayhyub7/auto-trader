// 파일: IndicatorComparison.tsx

import React, { useState } from "react";
import {
  compareFrontendIndicators,
  compareBackendIndicators,
  AllComparisonResponse,
} from "@/service/IndicatorComparisonService";
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

const INDICATORS = ["rsi", "stochrsi", "vwbb"] as const;

const formatTimestamp = (ts: number) => {
  const date = new Date(ts * 1000);
  return date.toLocaleString("ko-KR", {
    hour12: false,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
};

const getDiffColor = (diff: number) => {
  if (diff < 1) return "text-green-600";
  if (diff < 10) return "text-yellow-500";
  return "text-red-500";
};

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

  const handleCompareFrontend = async () => {
    const res = await compareFrontendIndicators(SYMBOL, interval);
    const frontend = res.result.frontend;
    const backend = res.result.backend;
    const merged: AllComparisonResponse["result"] = {};

    for (const key of INDICATORS) {
      const front = (frontend?.[key] ?? []).slice(-30);
      const back = backend?.[key] ?? [];
      const len = Math.min(front.length, back.length);
      const sorted = Array.from({ length: len }, (_, i) => {
        const f = front[i];
        const b = back[i];
        const diff: any = {};
        Object.keys(f).forEach((k) => {
          if (k !== "time" && b[k] != null && f[k] != null) {
            diff[k + "diff"] = Math.abs(f[k] - b[k]);
          }
        });
        return { frontend: f, backend: b, diff, timeDiffSec: b.time - f.time };
      }).sort((a, b) => b.frontend.time - a.frontend.time);

      merged[key] = sorted;
    }

    setResult(merged);
    setExecutionMeta({ type: "프론트 계산", time: Date.now() });
  };

  const handleCompareBackend = async () => {
    const res = await compareBackendIndicators(SYMBOL, interval);
    const frontend = res.result.frontend;
    const backend = res.result.backend;
    const merged: AllComparisonResponse["result"] = {};

    for (const key of INDICATORS) {
      const front = frontend?.[key] ?? [];
      const back = backend?.[key] ?? [];
      const len = Math.min(front.length, back.length);
      const sorted = Array.from({ length: len }, (_, i) => {
        const f = front[i];
        const b = back[i];
        const diff: any = {};
        Object.keys(f).forEach((k) => {
          if (k !== "time" && b[k] != null && f[k] != null) {
            diff[k + "diff"] = Math.abs(f[k] - b[k]);
          }
        });
        return { frontend: f, backend: b, diff, timeDiffSec: b.time - f.time };
      }).sort((a, b) => b.frontend.time - a.frontend.time);

      merged[key] = sorted;
    }

    setResult(merged);
    setExecutionMeta({ type: "백엔드 계산", time: Date.now() });
  };

  const renderTripleRow = (item: any, idx: number) => {
    const keys = Object.keys(item.frontend ?? {});
    return (
      <div
        key={idx}
        className="grid grid-cols-3 gap-2 border rounded bg-white text-xs font-mono p-4"
      >
        {/* 프론트 */}
        <div>
          <div className="font-bold text-gray-700">프론트</div>
          <div>{formatTimestamp(item.frontend?.time)}</div>
          <div className="text-gray-400">
            (TIMESTAMP: {item.frontend?.time})
          </div>
          {keys.map((k) => (
            <div key={k} className="text-green-600">
              {k}: {item.frontend?.[k]?.toFixed?.(4) ?? "--"}
            </div>
          ))}
        </div>

        {/* 지표비교 */}
        <div>
          <div className="font-bold text-gray-700">지표비교</div>
          <div>
            시간차: 00:00:{String(item.timeDiffSec ?? 0).padStart(2, "0")}
          </div>
          <div className="text-gray-400">
            TIMESTAMP: {item.backend?.time ?? "--"}
          </div>
          {Object.entries(item.diff ?? {}).map(([k, v]) => (
            <div key={k} className={`${getDiffColor(v as number)} font-bold`}>
              {k}: {(v as number).toFixed(4)}
            </div>
          ))}
        </div>

        {/* 백엔드 */}
        <div>
          <div className="font-bold text-gray-700">백엔드</div>
          <div>{formatTimestamp(item.backend?.time)}</div>
          <div className="text-gray-400">(TIMESTAMP: {item.backend?.time})</div>
          {keys.map((k) => (
            <div key={k} className="text-purple-600">
              {k}: {item.backend?.[k]?.toFixed?.(4) ?? "--"}
            </div>
          ))}
        </div>
      </div>
    );
  };

  return (
    <div className="text-black bg-gray-100 min-h-screen">
      <div className="sticky top-0 z-50 bg-gray-100 p-4 shadow-md">
        <h1 className="text-2xl font-bold mb-2">
          📊 지표 비교 (백엔드 vs 프론트)
        </h1>

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

        {result && (
          <Tabs
            value={activeTab ?? INDICATORS[0]}
            onValueChange={setActiveTab}
            className="w-full"
          >
            <TabsList className="mb-2 flex flex-wrap gap-2 justify-start">
              {INDICATORS.map((key) => (
                <TabsTrigger
                  key={key}
                  value={key}
                  className="font-semibold text-left px-4 capitalize"
                >
                  {key}
                </TabsTrigger>
              ))}
            </TabsList>

            {INDICATORS.map((key) => {
              const value = result[key] ?? [];
              return (
                <TabsContent key={key} value={key}>
                  <div className="p-4 space-y-4 overflow-y-auto max-h-[calc(100vh-180px)]">
                    <div className="text-base font-semibold text-green-600 mb-2">
                      📌 {key.toUpperCase()} 원본 비교 결과
                    </div>
                    {value.map((item: any, idx: number) =>
                      renderTripleRow(item, idx)
                    )}
                  </div>
                </TabsContent>
              );
            })}
          </Tabs>
        )}

        {executionMeta && (
          <div className="text-sm text-gray-700">
            <span className="font-semibold">실행 유형:</span>{" "}
            {executionMeta.type}
            &nbsp;&nbsp;|&nbsp;&nbsp;
            <span className="font-semibold">실행 시각:</span>{" "}
            {new Date(executionMeta.time).toLocaleString()} (
            {executionMeta.time})
          </div>
        )}
      </div>
    </div>
  );
};

export default IndicatorComparison;
