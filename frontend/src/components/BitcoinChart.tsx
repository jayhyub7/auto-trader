// ✅ BitcoinChart.tsx (EMA/SMA 제거 버전)
import React, { useEffect, useRef, useState } from "react";
import { createChart } from "lightweight-charts";
import {
  calculateRSI,
  calculateStochRSI,
  calculateVWBB,
} from "@/shared/util/indicatorUtil";
import SubChart from "./SubChart";
import { Timeframe, TIMEFRAME_LABELS } from "@/constants/timeframe";

const API_URL = "https://fapi.binance.com/fapi/v1/klines";
const SOCKET_URL = "wss://fstream.binance.com/ws/btcusdt@kline_";
const DEFAULT_INTERVAL = Timeframe.ONE_MINUTE;
const INTERVAL_MAP = {
  [Timeframe.ONE_MINUTE]: "1m",
  [Timeframe.THREE_MINUTES]: "3m",
  [Timeframe.FIVE_MINUTES]: "5m",
  [Timeframe.FIFTEEN_MINUTES]: "15m",
  [Timeframe.ONE_HOUR]: "1h",
  [Timeframe.FOUR_HOURS]: "4h",
};

const BitcoinChart = () => {
  const [currentInterval, setCurrentInterval] = useState(DEFAULT_INTERVAL);
  const [indicators, setIndicators] = useState<string[]>([]);
  const [rsiData, setRsiData] = useState<any[]>([]);
  const [stochRsiData, setStochRsiData] = useState<any[]>([]);
  const chartRef = useRef<any>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const candleSeriesRef = useRef<any>(null);
  const candlesRef = useRef<any[]>([]);
  const socketRef = useRef<WebSocket | null>(null);

  const vwbbUpperRef = useRef<any>(null);
  const vwbbLowerRef = useRef<any>(null);
  const vwbbBasisRef = useRef<any>(null);

  const toggleIndicator = (name: string) => {
    setIndicators((prev) =>
      prev.includes(name) ? prev.filter((i) => i !== name) : [...prev, name]
    );
  };

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    if (chartRef.current) {
      try {
        chartRef.current.remove();
      } catch (e) {
        console.warn("chart.remove() error (already disposed)", e);
      }
      chartRef.current = null;
    }

    const chart = createChart(container, {
      width: container.clientWidth,
      height: 400,
      layout: {
        background: { color: "#0f172a" },
        textColor: "#e2e8f0",
      },
      grid: {
        vertLines: { color: "#1e293b" },
        horzLines: { color: "#1e293b" },
      },
      rightPriceScale: { borderColor: "#334155" },
    });

    chart.timeScale().applyOptions({
      borderColor: "#334155",
      timeVisible: true,
      secondsVisible: false,
    });

    const candleSeries = chart.addCandlestickSeries();
    chartRef.current = chart;
    candleSeriesRef.current = candleSeries;

    const fetchInitialCandles = async () => {
      const interval = INTERVAL_MAP[currentInterval];
      const intervalMs = {
        "1m": 60_000,
        "3m": 3 * 60_000,
        "5m": 5 * 60_000,
        "15m": 15 * 60_000,
        "1h": 60 * 60_000,
        "4h": 4 * 60 * 60_000,
      }[interval];

      const now = Date.now();
      const endTime = now - (now % intervalMs);
      const startTime = endTime - intervalMs * 500;

      const res = await fetch(
        `${API_URL}?symbol=BTCUSDT&interval=${interval}&startTime=${startTime}&endTime=${endTime}`
      );
      const raw = await res.json();
      const candles = raw.map((d: any) => ({
        time: d[0] / 1000,
        open: +d[1],
        high: +d[2],
        low: +d[3],
        close: +d[4],
        final: true,
      }));

      candlesRef.current = candles;
      candleSeriesRef.current.setData(candles);
      chartRef.current.timeScale().fitContent();
      updateIndicators();
    };

    const updateIndicators = () => {
      if (
        !chartRef.current ||
        typeof chartRef.current.addLineSeries !== "function"
      )
        return;
      const base = candlesRef.current.slice();

      if (indicators.includes("VWBB")) {
        const vwbb = calculateVWBB(base);
        if (!vwbbUpperRef.current) {
          vwbbUpperRef.current = chartRef.current.addLineSeries({
            color: "red",
          });
          vwbbLowerRef.current = chartRef.current.addLineSeries({
            color: "blue",
          });
          vwbbBasisRef.current = chartRef.current.addLineSeries({
            color: "orange",
          });
        }
        vwbbUpperRef.current.setData(
          vwbb.upper.filter((d) => typeof d.value === "number")
        );
        vwbbLowerRef.current.setData(
          vwbb.lower.filter((d) => typeof d.value === "number")
        );
        vwbbBasisRef.current.setData(
          vwbb.basis.filter((d) => typeof d.value === "number")
        );
      } else {
        vwbbUpperRef.current?.setData([]);
        vwbbLowerRef.current?.setData([]);
        vwbbBasisRef.current?.setData([]);
        vwbbUpperRef.current = null;
        vwbbLowerRef.current = null;
        vwbbBasisRef.current = null;
      }

      if (indicators.includes("RSI")) {
        setRsiData(calculateRSI(base));
      } else {
        setRsiData([]);
      }

      if (indicators.includes("STOCH_RSI")) {
        setStochRsiData(calculateStochRSI(base));
      } else {
        setStochRsiData([]);
      }
    };

    const connectSocket = () => {
      const ws = new WebSocket(`${SOCKET_URL}${INTERVAL_MAP[currentInterval]}`);
      socketRef.current = ws;
      ws.onmessage = (e) => {
        const msg = JSON.parse(e.data);
        const k = msg.k;
        const newCandle = {
          time: Math.floor(k.t / 1000),
          open: +k.o,
          high: +k.h,
          low: +k.l,
          close: +k.c,
          final: k.x,
        };

        const last = candlesRef.current.at(-1);
        if (!last) return;

        if (newCandle.time > last.time && last.final) {
          candlesRef.current.push(newCandle);
        } else if (newCandle.time === last.time) {
          candlesRef.current[candlesRef.current.length - 1] = newCandle;
        }

        try {
          candleSeriesRef.current?.update(newCandle);
        } catch (err) {}

        updateIndicators();
      };
      ws.onclose = () => setTimeout(connectSocket, 3000);
    };

    fetchInitialCandles();
    connectSocket();

    return () => {
      if (socketRef.current?.readyState === WebSocket.OPEN) {
        socketRef.current.close();
      }
      socketRef.current = null;

      try {
        chartRef.current?.remove();
      } catch (e) {
        console.warn("chart.remove() error", e);
      }
      chartRef.current = null;
      candleSeriesRef.current = null;
      vwbbUpperRef.current = null;
      vwbbLowerRef.current = null;
      vwbbBasisRef.current = null;
    };
  }, [currentInterval, indicators]);

  const safeTimeScale = chartRef.current?.timeScale?.();

  return (
    <div className="relative p-4">
      <div className="absolute top-2 left-2 flex gap-2 z-10">
        {Object.values(Timeframe).map((item) => (
          <button
            key={item}
            onClick={() => setCurrentInterval(item)}
            className={`px-2 py-1 text-xs rounded shadow font-semibold ${
              currentInterval === item
                ? "bg-yellow-400 text-black"
                : "bg-gray-700 text-gray-300"
            }`}
          >
            {TIMEFRAME_LABELS[item]}
          </button>
        ))}
      </div>
      <div className="absolute top-2 right-2 flex gap-2 z-10">
        {["RSI", "STOCH_RSI", "VWBB"].map((name) => (
          <button
            key={name}
            onClick={() => toggleIndicator(name)}
            className={`px-2 py-1 text-xs rounded font-semibold shadow ${
              indicators.includes(name)
                ? "bg-green-500 text-white"
                : "bg-gray-600 text-gray-300"
            }`}
          >
            {name}
          </button>
        ))}
      </div>
      <div
        ref={containerRef}
        className="w-full h-[400px] border border-slate-600 rounded-md"
      />
      {(indicators.includes("RSI") || indicators.includes("STOCH_RSI")) &&
        safeTimeScale && (
          <SubChart
            rsiData={indicators.includes("RSI") ? rsiData : []}
            stochRsiData={indicators.includes("STOCH_RSI") ? stochRsiData : []}
            mainTimeScale={safeTimeScale}
          />
        )}
    </div>
  );
};

export default BitcoinChart;
