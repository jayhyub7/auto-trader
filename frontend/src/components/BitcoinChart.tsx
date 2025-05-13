// 📁 components/BitcoinChart.tsx

import React, { useEffect, useRef, useState } from "react";
import { createChart } from "lightweight-charts";
import {
  calculateEMA,
  calculateSMA,
  calculateRSI,
  calculateStochRSI,
  calculateVWBB,
} from "@/util/indicatorUtil";
import SubChart from "./SubChart";
import { Timeframe, TIMEFRAME_LABELS } from "@/constants/timeframe";

interface BitcoinChartProps {
  interval?: Timeframe;
}

const API_URL = "https://api.binance.com/api/v3/klines";
const DEFAULT_INTERVAL = Timeframe.ONE_MINUTE;
const INTERVAL_MAP: Record<Timeframe, string> = {
  [Timeframe.ONE_MINUTE]: "1m",
  [Timeframe.THREE_MINUTES]: "3m",
  [Timeframe.FIVE_MINUTES]: "5m",
  [Timeframe.FIFTEEN_MINUTES]: "15m",
  [Timeframe.ONE_HOUR]: "1h",
  [Timeframe.FOUR_HOURS]: "4h",
};

const BitcoinChart: React.FC<BitcoinChartProps> = ({ interval = DEFAULT_INTERVAL }) => {
  const [currentInterval, setCurrentInterval] = useState<Timeframe>(interval);
  const chartContainerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<any>(null);
  const candleSeriesRef = useRef<any>(null);
  const emaSeriesRef = useRef<any>(null);
  const smaSeriesRefs = useRef<Record<number, any>>({});
  const candlesRef = useRef<any[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [indicators, setIndicators] = useState<string[]>([]);
  const [rsiData, setRsiData] = useState<any[]>([]);
  const [stochRsiData, setStochRsiData] = useState<any[]>([]);
  const vwbbUpperSeriesRef = useRef<any>(null);
  const vwbbLowerSeriesRef = useRef<any>(null);
  const vwbbBasisSeriesRef = useRef<any>(null);
  const socketRef = useRef<WebSocket | null>(null);

  const toggleIndicator = (name: string) => {
    setIndicators((prev) =>
      prev.includes(name) ? prev.filter((i) => i !== name) : [...prev, name]
    );
  };

  useEffect(() => {
    const container = chartContainerRef.current;
    if (!container) return;

    if (chartRef.current) {
      chartRef.current.remove();
      chartRef.current = null;
      candleSeriesRef.current = null;
      emaSeriesRef.current = null;
      smaSeriesRefs.current = {};
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
      tickMarkFormatter: (time) => {
        const date = new Date(time * 1000);
        const hours = date.getHours().toString().padStart(2, "0");
        const minutes = date.getMinutes().toString().padStart(2, "0");
        return `${hours}:${minutes}`;
      },
    });

    chartRef.current = chart;
    const series = chart.addCandlestickSeries();
    candleSeriesRef.current = series;

    const fetchInitialData = async () => {
      setIsLoading(true);
      try {
        const mappedInterval = INTERVAL_MAP[currentInterval] || "1m";
        const now = Date.now();
        const endTime = now - (now % 60000);
        const startTime = endTime - 500 * 60 * 1000;

        const res = await fetch(`${API_URL}?symbol=BTCUSDT&interval=${mappedInterval}&startTime=${startTime}&endTime=${endTime}`);
        const raw = await res.json();

        const round = (v: number, decimals = 4) =>
          Math.round(v * 10 ** decimals) / 10 ** decimals;

        const formatted = raw.map((d: any) => ({
          time: d[0] / 1000,
          open: round(parseFloat(d[1])),
          high: round(parseFloat(d[2])),
          low: round(parseFloat(d[3])),
          close: round(parseFloat(d[4])),
          final: true,
        }));

        candlesRef.current = formatted;
        series.setData(formatted);
        chart.timeScale().fitContent();

        updateIndicators();
        connectWebSocket();
      } catch (e) {
        console.error("데이터 로딩 실패", e);
      }
      setIsLoading(false);
    };

    const updateIndicators = () => {

      console.log("🧪 프론트 VWBB 캔들 시작 time:", candlesRef.current[0].time);
        console.log("🧪 프론트 VWBB 캔들 마지막 time:", candlesRef.current.at(-1)?.time);
        console.log("🧪 프론트 VWBB 캔들 수:", candlesRef.current.length);

      let baseData = [...candlesRef.current];
      if (!baseData.at(-1)?.final) baseData = baseData.slice(0, -1);

      if (indicators.includes("EMA") && emaSeriesRef.current) {
        const ema = calculateEMA(baseData);
        emaSeriesRef.current.setData(ema.filter((d) => d.value !== null));
      }

      if (indicators.includes("SMA")) {
        [20, 60, 100].forEach((period) => {
          const sma = calculateSMA(baseData, period);
          const series = smaSeriesRefs.current[period];
          series?.setData(sma.filter((d) => d.value !== null));
        });
      }

      if (indicators.includes("RSI")) {
        setRsiData(calculateRSI(baseData, 14));
      }

      if (indicators.includes("STOCH_RSI")) {
        setStochRsiData(calculateStochRSI(baseData, 14, 14, 3, 3));
      }

      if (indicators.includes("VWBB")) {
        const vwbb = calculateVWBB(baseData, 20, 2.0);

        if (!vwbbUpperSeriesRef.current) {
          vwbbUpperSeriesRef.current = chart.addLineSeries({ color: "red" });
          vwbbLowerSeriesRef.current = chart.addLineSeries({ color: "blue" });
          vwbbBasisSeriesRef.current = chart.addLineSeries({ color: "orange" });
        }

        vwbbUpperSeriesRef.current.setData(vwbb.upper.filter(d => d.value !== null));
        vwbbLowerSeriesRef.current.setData(vwbb.lower.filter(d => d.value !== null));
        vwbbBasisSeriesRef.current.setData(vwbb.basis.filter(d => d.value !== null));
      }
    };

    const connectWebSocket = () => {
      const intervalStr = INTERVAL_MAP[currentInterval] || "1m";
      const wsUrl = `wss://stream.binance.com:9443/ws/btcusdt@kline_${intervalStr}`;
      socketRef.current = new WebSocket(wsUrl);

      socketRef.current.onmessage = (event) => {
        const data = JSON.parse(event.data);
        const k = data.k;

        const newCandle = {
          time: Math.floor(k.t / 1000),
          open: parseFloat(k.o),
          high: parseFloat(k.h),
          low: parseFloat(k.l),
          close: parseFloat(k.c),
          final: k.x,
        };

        const existing = candlesRef.current;
        const lastTime = existing[existing.length - 1]?.time;

        if (newCandle.time > lastTime) {
          candlesRef.current.push(newCandle);
        } else if (newCandle.time === lastTime) {
          candlesRef.current[existing.length - 1] = newCandle;
        }

        candleSeriesRef.current?.update(newCandle);

        if (k.x) {
          console.log("📩 실시간 마감된 캔들 시간:", new Date(newCandle.time * 1000).toLocaleString(), newCandle);
          updateIndicators();
        }
      };

      socketRef.current.onclose = () => {
        console.warn("⚠ 웹소켓 종료됨, 재연결 시도 중...");
        setTimeout(connectWebSocket, 3000);
      };
    };

    fetchInitialData();

    return () => {
      if (socketRef.current) socketRef.current.close();
      if (chartRef.current) chartRef.current.remove();
      chartRef.current = null;
    };
  }, [currentInterval, indicators]);

  return (
    <div className="relative p-4">
      <div className="absolute top-2 left-2 flex gap-2 z-10">
        {Object.values(Timeframe).map((item) => (
          <button
            key={item}
            onClick={() => setCurrentInterval(item)}
            className={`px-2 py-1 text-xs rounded shadow font-semibold ${
              currentInterval === item ? "bg-yellow-400 text-black" : "bg-gray-700 text-gray-300"
            }`}
          >
            {TIMEFRAME_LABELS[item]}
          </button>
        ))}
      </div>
      <div className="absolute top-2 right-2 flex gap-2 z-10">
        {["EMA", "SMA", "RSI", "STOCH_RSI", "VWBB"].map((name) => (
          <button
            key={name}
            onClick={() => toggleIndicator(name)}
            className={`px-2 py-1 text-xs rounded font-semibold shadow ${
              indicators.includes(name) ? "bg-green-500 text-white" : "bg-gray-600 text-gray-300"
            }`}
          >
            {name}
          </button>
        ))}
      </div>
      {isLoading && <div className="text-white">📡 로딩 중...</div>}
      <div ref={chartContainerRef} className="w-full h-[400px] border border-slate-600 rounded-md" />
      {(indicators.includes("RSI") || indicators.includes("STOCH_RSI")) && chartRef.current && (
        <SubChart
          rsiData={indicators.includes("RSI") ? rsiData : []}
          stochRsiData={indicators.includes("STOCH_RSI") ? stochRsiData : []}
          mainTimeScale={chartRef.current.timeScale()}
        />
      )}
    </div>
  );
};

export default BitcoinChart;
