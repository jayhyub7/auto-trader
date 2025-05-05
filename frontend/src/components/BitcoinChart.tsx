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
  interval?: Timeframe; // 타입을 string 대신 Timeframe으로 변경
}

const API_URL = "https://api.binance.com/api/v3/klines";
const DEFAULT_INTERVAL = Timeframe.ONE_MINUTE; // 기본값을 Timeframe에서 가져온 값으로 설정
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
  const smaSeriesRefs = useRef<Record<number, any>>({}); // period별 ref 저장용
  const candlesRef = useRef<any[]>([]); 
  const intervalRef = useRef<NodeJS.Timeout | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [indicators, setIndicators] = useState<string[]>([]);
  const [rsiData, setRsiData] = useState<any[]>([]);
  const [stochRsiData, setStochRsiData] = useState<any[]>([]);
  //VWBB
  const vwbbUpperSeriesRef = useRef<any>(null);
  const vwbbLowerSeriesRef = useRef<any>(null);
  const vwbbBasisSeriesRef = useRef<any>(null);

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
      smaSeriesRef.current = null;
      if (intervalRef.current) clearInterval(intervalRef.current);
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
        const mappedInterval = INTERVAL_MAP[currentInterval] || "1m"; // Timeframe을 사용하여 interval 값 가져오기
        const res = await fetch(`${API_URL}?symbol=BTCUSDT&interval=${mappedInterval}&limit=500`);
        const raw = await res.json();
        const formatted = raw.map((d: any) => ({
          time: d[0] / 1000,
          open: parseFloat(d[1]),
          high: parseFloat(d[2]),
          low: parseFloat(d[3]),
          close: parseFloat(d[4]),
        }));

        candlesRef.current = formatted;
        series.setData(formatted);
        chart.timeScale().fitContent();

        if (indicators.includes("EMA")) {
          const ema = calculateEMA(formatted);
          emaSeriesRef.current = chart.addLineSeries();
          emaSeriesRef.current.setData(ema.filter((d) => d.value !== null));
        }

        if (indicators.includes("SMA")) {
          [20, 60, 100].forEach((period) => {
            const sma = calculateSMA(formatted, period);
            const series = chart.addLineSeries({
              color: period === 20 ? "orange" : period === 60 ? "aqua" : "violet",
            });
            series.setData(sma.filter((d) => d.value !== null));
            smaSeriesRefs.current[period] = series;
          });
        }

        if (indicators.includes("RSI")) {
          const rsi = calculateRSI(formatted);
          setRsiData(rsi);
        }

        if (indicators.includes("StochRSI")) {
          const stoch = calculateStochRSI(formatted);
          setStochRsiData(stoch);
        }
        if (indicators.includes("VWBB")) {
          const vwbb = calculateVWBB(formatted);
          vwbbUpperSeriesRef.current = chart.addLineSeries({ color: "red" });
          vwbbLowerSeriesRef.current = chart.addLineSeries({ color: "blue" });
          vwbbBasisSeriesRef.current = chart.addLineSeries({ color: "orange" });
        
          vwbbUpperSeriesRef.current.setData(vwbb.upper.filter(d => d.value !== null));
          vwbbLowerSeriesRef.current.setData(vwbb.lower.filter(d => d.value !== null));
          vwbbBasisSeriesRef.current.setData(vwbb.basis.filter(d => d.value !== null));
        }        
      } catch (e) {
        console.error("데이터 로딩 실패", e);
      }
      setIsLoading(false);
    };

    const startRealtimeUpdates = () => {
      intervalRef.current = setInterval(async () => {
        try {
          const mappedInterval = INTERVAL_MAP[currentInterval] || "1m";
          const res = await fetch(`${API_URL}?symbol=BTCUSDT&interval=${mappedInterval}&limit=2`);
          const raw = await res.json();
          const last = raw[raw.length - 1];
          const newCandle = {
            time: last[0] / 1000,
            open: parseFloat(last[1]),
            high: parseFloat(last[2]),
            low: parseFloat(last[3]),
            close: parseFloat(last[4]),
          };

          const existing = candlesRef.current;
          const lastTime = existing[existing.length - 1]?.time;

          if (newCandle.time > lastTime) {
            candlesRef.current.push(newCandle);
            candleSeriesRef.current?.update(newCandle);
          } else if (newCandle.time === lastTime) {
            candlesRef.current[existing.length - 1] = newCandle;
            candleSeriesRef.current?.update(newCandle);
          }

          if (indicators.includes("EMA") && emaSeriesRef.current) {
            const ema = calculateEMA(candlesRef.current);
            emaSeriesRef.current.setData(ema.filter((d) => d.value !== null));
          }

          if (indicators.includes("SMA")) {
            [20, 60, 100].forEach((period) => {
              const sma = calculateSMA(candlesRef.current, period);
              const series = smaSeriesRefs.current[period];
              series?.setData(sma.filter((d) => d.value !== null));
            });
          }

          if (indicators.includes("RSI")) {
            const rsi = calculateRSI(candlesRef.current);
            setRsiData(rsi);
          }

          if (indicators.includes("StochRSI")) {
            const stoch = calculateStochRSI(candlesRef.current);
            setStochRsiData(stoch);
          }
          if (indicators.includes("VWBB")) {
            const vwbb = calculateVWBB(candlesRef.current);
            vwbbUpperSeriesRef.current?.setData(vwbb.upper.filter(d => d.value !== null));
            vwbbLowerSeriesRef.current?.setData(vwbb.lower.filter(d => d.value !== null));
            vwbbBasisSeriesRef.current?.setData(vwbb.basis.filter(d => d.value !== null));
          }          
        } catch (e) {
          console.error("실시간 캔들 갱신 실패", e);
        }
      }, 2000);
    };

    fetchInitialData();
    startRealtimeUpdates();

    return () => {
      chart.remove();
      chartRef.current = null;
      if (intervalRef.current) clearInterval(intervalRef.current);
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
        {["EMA", "SMA", "RSI", "StochRSI", "VWBB"].map((name) => (
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
      {isLoading && <div className="text-white">? 로딩 중...</div>}
      <div ref={chartContainerRef} className="w-full h-[400px] border border-slate-600 rounded-md" />
      {(indicators.includes("RSI") || indicators.includes("StochRSI")) && chartRef.current && (
        <SubChart
          rsiData={indicators.includes("RSI") ? rsiData : []}
          stochRsiData={indicators.includes("StochRSI") ? stochRsiData : []}
          mainTimeScale={chartRef.current.timeScale()}
        />
      )}
    </div>
  );
};

export default BitcoinChart;
