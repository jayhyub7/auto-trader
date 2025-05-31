// ✅ BitcoinChart.tsx (EMA 입력창 디자인 수정: 버튼 아래에 absolute 위치로 띄움)
import React, { useEffect, useRef, useState } from "react";
import { createChart } from "lightweight-charts";
import {
  calculateRSI,
  calculateStochRSI,
  calculateVWBB,
  calculateVWAP,
  calculateEMA,
  calculateSMAFromCandles,
} from "@/shared/util/indicatorUtil";
import SubChart from "./SubChart";
import { Timeframe, TIMEFRAME_LABELS } from "@/constants/timeframe";
import Cookies from "js-cookie";

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
  [Timeframe.ONE_DAY]: "1d",
  [Timeframe.ONE_WEEK]: "1w",
  [Timeframe.ONE_MONTH]: "1M",
};

const EMA_COLORS = ["#facc15", "#ec4899", "#8b5cf6"];

const BitcoinChart = () => {
  const [currentInterval, setCurrentInterval] = useState(DEFAULT_INTERVAL);
  const [indicators, setIndicators] = useState<string[]>([]);
  const indicatorsRef = useRef<string[]>([]);
  const [rsiData, setRsiData] = useState<any[]>([]);
  const [stochRsiData, setStochRsiData] = useState<any[]>([]);
  const [mainTimeScale, setMainTimeScale] = useState<any>(null);
  const [emaPeriods, setEmaPeriods] = useState<number[]>([]);
  const [tempEmaValue, setTempEmaValue] = useState("7, 25, 99");
  const [showEmaInput, setShowEmaInput] = useState(false);
  const chartRef = useRef<any>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const candleSeriesRef = useRef<any>(null);
  const candlesRef = useRef<any[]>([]);
  const socketRef = useRef<WebSocket | null>(null);

  const vwbbUpperRef = useRef<any>(null);
  const vwbbLowerRef = useRef<any>(null);
  const vwbbBasisRef = useRef<any>(null);
  const vwapSeriesRef = useRef<any>(null);
  const emaRefs = useRef<any[]>([]);

  const [smaPeriods, setSmaPeriods] = useState<number[]>([]);
  const [tempSmaValue, setTempSmaValue] = useState("50, 100, 200");
  const [showSmaInput, setShowSmaInput] = useState(false);
  const smaRefs = useRef<any[]>([]);

  useEffect(() => {
    const cookie = Cookies.get("emaPeriods");
    if (cookie) {
      try {
        const parsed = JSON.parse(cookie);
        setEmaPeriods(parsed);
        setTempEmaValue(parsed.join(", ")); // ✅ 추가됨
      } catch {}
    } else {
      setEmaPeriods([7, 25, 99]);
      setTempEmaValue("7, 25, 99"); // ✅ 추가됨
    }

    const smaCookie = Cookies.get("smaPeriods");
    if (smaCookie) {
      try {
        const parsed = JSON.parse(smaCookie);
        setSmaPeriods(parsed);
        setTempSmaValue(parsed.join(", "));
      } catch {}
    } else {
      setSmaPeriods([50, 100, 200]);
      setTempSmaValue("50, 100, 200");
    }
  }, []);

  const toggleIndicator = (name: string) => {
    setIndicators((prev) => {
      const next = prev.includes(name)
        ? prev.filter((i) => i !== name)
        : [...prev, name];
      indicatorsRef.current = next;
      return next;
    });
  };

  const updateIndicators = () => {
    if (
      !chartRef.current ||
      typeof chartRef.current.addLineSeries !== "function"
    )
      return;
    const base = candlesRef.current.slice();
    const currentIndicators = indicatorsRef.current;

    if (currentIndicators.includes("VWBB")) {
      if (
        !vwbbUpperRef.current ||
        !vwbbLowerRef.current ||
        !vwbbBasisRef.current
      ) {
        vwbbUpperRef.current = chartRef.current.addLineSeries({ color: "red" });
        vwbbLowerRef.current = chartRef.current.addLineSeries({
          color: "blue",
        });
        vwbbBasisRef.current = chartRef.current.addLineSeries({
          color: "orange",
        });
      }
      const vwbb = calculateVWBB(base);
      vwbbUpperRef.current.setData(
        vwbb.upper.filter((d) => typeof d.value === "number")
      );
      vwbbLowerRef.current.setData(
        vwbb.lower.filter((d) => typeof d.value === "number")
      );
      vwbbBasisRef.current.setData(
        vwbb.basis.filter((d) => typeof d.value === "number")
      );
    }

    if (currentIndicators.includes("VWAP")) {
      if (!vwapSeriesRef.current) {
        vwapSeriesRef.current = chartRef.current.addLineSeries({
          color: "#facc15",
          lineWidth: 2,
        });
      }
      const vwap = calculateVWAP(base).filter(
        (d) => typeof d.value === "number"
      );
      vwapSeriesRef.current.setData(vwap);
    }

    if (currentIndicators.includes("EMA")) {
      if (emaRefs.current.length == 0) {
        // 새 시리즈 생성
        emaRefs.current = emaPeriods.map((period, idx) => {
          const series = chartRef.current!.addLineSeries({
            color: EMA_COLORS[idx] || "gray",
            lineWidth: 1,
          });
          return series;
        });
      }
      emaRefs.current.forEach((series, idx) => {
        const period = emaPeriods[idx];
        if (!series || !period) return;

        const ema = calculateEMA(base, period).filter(
          (d) => typeof d.value === "number"
        );
        series.setData(ema);
      });
    } else {
      // ✅ 꺼진 경우에만 제거
      emaRefs.current.forEach((ref) => {
        try {
          if (ref && chartRef.current) {
            chartRef.current.removeSeries(ref);
          }
        } catch {}
      });
      emaRefs.current = [];
    }

    if (currentIndicators.includes("SMA")) {
      if (smaRefs.current.length === 0) {
        smaRefs.current = smaPeriods.map((period, idx) => {
          const series = chartRef.current!.addLineSeries({
            color: EMA_COLORS[idx] || "gray",
            lineWidth: 1,
          });
          return series;
        });
      }
      smaRefs.current.forEach((series, idx) => {
        const period = smaPeriods[idx];
        if (!series || !period) return;

        const sma = calculateSMAFromCandles(base, period).filter(
          (d) => typeof d.value === "number"
        );
        series.setData(sma);
      });
    } else {
      smaRefs.current.forEach((ref) => {
        try {
          if (ref && chartRef.current) {
            chartRef.current.removeSeries(ref);
          }
        } catch {}
      });
      smaRefs.current = [];
    }

    setRsiData(currentIndicators.includes("RSI") ? calculateRSI(base) : []);
    setStochRsiData(
      currentIndicators.includes("STOCH_RSI") ? calculateStochRSI(base) : []
    );
  };

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    chartRef.current?.remove();
    const chart = createChart(container, {
      width: container.clientWidth,
      height: 400,
      layout: { background: { color: "#0f172a" }, textColor: "#e2e8f0" },
      grid: {
        vertLines: { color: "#1e293b" },
        horzLines: { color: "#1e293b" },
      },
      rightPriceScale: { borderColor: "#334155" },
    });

    chart
      .timeScale()
      .applyOptions({ borderColor: "#334155", timeVisible: true });
    const candleSeries = chart.addCandlestickSeries();
    chartRef.current = chart;
    candleSeriesRef.current = candleSeries;
    setMainTimeScale(chart.timeScale());

    const interval = INTERVAL_MAP[currentInterval];
    const intervalMs = {
      "1m": 60000,
      "3m": 180000,
      "5m": 300000,
      "15m": 900000,
      "1h": 3600000,
      "4h": 14400000,
      "1d": 86400000,
      "1w": 604800000,
      "1M": 2592000000,
    }[interval];

    const fetchInitialCandles = async () => {
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
        volume: +d[5],
        final: true,
      }));
      candlesRef.current = candles;
      candleSeries.setData(candles);
      chart.timeScale().fitContent();
      updateIndicators();
    };

    const connectSocket = () => {
      const ws = new WebSocket(`${SOCKET_URL}${interval}`);
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
          volume: +k.v,
          final: k.x,
        };
        const last = candlesRef.current.at(-1);
        if (!last) return;
        if (newCandle.time > last.time && last.final) {
          candlesRef.current.push(newCandle);
        } else if (newCandle.time === last.time) {
          candlesRef.current[candlesRef.current.length - 1] = newCandle;
        }
        candleSeriesRef.current?.update(newCandle);
        updateIndicators();
      };
      ws.onclose = () => setTimeout(connectSocket, 3000);
    };

    fetchInitialCandles();
    connectSocket();

    return () => {
      socketRef.current?.close();
      socketRef.current = null;
      chartRef.current?.remove();
      chartRef.current = null;
      candleSeriesRef.current = null;
      vwbbUpperRef.current = null;
      vwbbLowerRef.current = null;
      vwbbBasisRef.current = null;
      vwapSeriesRef.current = null;
      emaRefs.current = [];
    };
  }, [currentInterval]);

  useEffect(() => {
    updateIndicators();
  }, [indicators, emaPeriods]);

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
        {["RSI", "STOCH_RSI", "VWBB", "VWAP"].map((name) => (
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
        <button
          key={"EMA"}
          onClick={() => {
            toggleIndicator("EMA");
            setShowEmaInput((prev) => !prev);
          }}
          className={`px-2 py-1 text-xs rounded font-semibold shadow ${
            indicators.includes("EMA")
              ? "bg-green-500 text-white"
              : "bg-gray-600 text-gray-300"
          }`}
        >
          EMA
        </button>
        <button
          key={"SMA"}
          onClick={() => {
            toggleIndicator("SMA");
            setShowSmaInput((prev) => !prev);
          }}
          className={`px-2 py-1 text-xs rounded font-semibold shadow ${
            indicators.includes("SMA")
              ? "bg-green-500 text-white"
              : "bg-gray-600 text-gray-300"
          }`}
        >
          SMA
        </button>
      </div>
      {showEmaInput && indicators.includes("EMA") && (
        <div className="absolute top-12 right-2 bg-slate-800 px-3 py-2 rounded shadow z-10 text-sm w-48">
          <label className="block text-slate-300 mb-1">EMA Periods:</label>
          <input
            className="bg-slate-700 text-white px-2 py-1 rounded w-full mb-2"
            value={tempEmaValue}
            onChange={(e) => setTempEmaValue(e.target.value)}
          />
          <button
            className="w-full bg-amber-500 text-black rounded py-1 font-semibold"
            onClick={() => {
              const parts = tempEmaValue
                .split(",")
                .map((v) => parseInt(v.trim(), 10))
                .filter((v) => !isNaN(v));

              setEmaPeriods(parts);
              Cookies.set("emaPeriods", JSON.stringify(parts), { expires: 30 });

              // EMA가 비활성 상태였다면 강제 활성화
              if (!indicators.includes("EMA")) {
                const next = [...indicators, "EMA"];
                setIndicators(next);
                indicatorsRef.current = next;

                // ✅ indicators 비동기 기다리지 않고 직접 반영
                setTimeout(() => {
                  updateIndicators(); // ⬅ 강제 호출
                }, 0);
              } else {
                // ✅ 이미 켜져 있던 경우에도 반영 필요
                setTimeout(() => {
                  updateIndicators();
                }, 0);
              }

              // 입력창 닫기
              setShowEmaInput(false);
            }}
          >
            적용
          </button>
        </div>
      )}
      {showSmaInput && indicators.includes("SMA") && (
        <div className="absolute top-[88px] right-2 bg-slate-800 px-3 py-2 rounded shadow z-10 text-sm w-48">
          <label className="block text-slate-300 mb-1">SMA Periods:</label>
          <input
            className="bg-slate-700 text-white px-2 py-1 rounded w-full mb-2"
            value={tempSmaValue}
            onChange={(e) => setTempSmaValue(e.target.value)}
          />
          <button
            className="w-full bg-amber-500 text-black rounded py-1 font-semibold"
            onClick={() => {
              const parts = tempSmaValue
                .split(",")
                .map((v) => parseInt(v.trim(), 10))
                .filter((v) => !isNaN(v));

              setSmaPeriods(parts);
              Cookies.set("smaPeriods", JSON.stringify(parts), { expires: 30 });

              if (!indicators.includes("SMA")) {
                const next = [...indicators, "SMA"];
                setIndicators(next);
                indicatorsRef.current = next;
                setTimeout(() => updateIndicators(), 0);
              } else {
                setTimeout(() => updateIndicators(), 0);
              }

              setShowSmaInput(false);
            }}
          >
            적용
          </button>
        </div>
      )}

      <div
        ref={containerRef}
        className="w-full h-[400px] border border-slate-600 rounded-md"
      />
      {(indicators.includes("RSI") || indicators.includes("STOCH_RSI")) &&
        mainTimeScale && (
          <SubChart
            rsiData={indicators.includes("RSI") ? rsiData : []}
            stochRsiData={indicators.includes("STOCH_RSI") ? stochRsiData : []}
            mainTimeScale={mainTimeScale}
          />
        )}
    </div>
  );
};

export default BitcoinChart;
