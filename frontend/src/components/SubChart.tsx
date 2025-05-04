import React, { useEffect, useRef } from "react";
import { createChart } from "lightweight-charts";

interface LineData {
  time: number;
  value: number;
}

interface DualLinePoint {
  time: number;
  k: number | null;
  d: number | null;
}

interface SubChartProps {
  rsiData?: LineData[];
  stochRsiData?: DualLinePoint[];
  mainTimeScale: ReturnType<typeof createChart>["timeScale"];
}

const SubChart: React.FC<SubChartProps> = ({
  rsiData = [],
  stochRsiData = [],
  mainTimeScale,
}) => {
  const rsiChartRef = useRef<HTMLDivElement>(null);
  const stochChartRef = useRef<HTMLDivElement>(null);
  const rsiChartInstance = useRef<ReturnType<typeof createChart>>();
  const stochChartInstance = useRef<ReturnType<typeof createChart>>();

  // ✅ RSI 차트: 메인 차트와 x축 동기화
  useEffect(() => {
    if (mainTimeScale && rsiChartInstance.current) {
      const unsubscribe = mainTimeScale.subscribeVisibleLogicalRangeChange((range) => {
        rsiChartInstance.current?.timeScale().setVisibleLogicalRange(range);
      });
      return () => unsubscribe?.();
    }
  }, [mainTimeScale]);

  // ✅ StochRSI 차트: 메인 차트와 x축 동기화
  useEffect(() => {
    if (mainTimeScale && stochChartInstance.current) {
      const unsubscribe = mainTimeScale.subscribeVisibleLogicalRangeChange((range) => {
        stochChartInstance.current?.timeScale().setVisibleLogicalRange(range);
      });
      return () => unsubscribe?.();
    }
  }, [mainTimeScale]);

  // ✅ RSI 및 StochRSI 차트 생성 및 데이터 바인딩
  useEffect(() => {
    // 이전 차트 제거 (오류 방지)
    rsiChartInstance.current?.remove();
    rsiChartInstance.current = undefined;
    stochChartInstance.current?.remove();
    stochChartInstance.current = undefined;

    // RSI 차트 생성
    if (rsiChartRef.current) {
      const chart = createChart(rsiChartRef.current, {
        height: 120,
        layout: { background: { color: "#0f172a" }, textColor: "#e2e8f0" },
        grid: { vertLines: { color: "#1e293b" }, horzLines: { color: "#1e293b" } },
        timeScale: {
          borderColor: "#334155",
          timeVisible: true,
          lockVisibleTimeRangeOnResize: true,
          tickMarkFormatter: (time) => {
            const date = new Date(time * 1000);
            return `${date.getHours().toString().padStart(2, "0")}:${date
              .getMinutes()
              .toString()
              .padStart(2, "0")}`;
          },
        },
        rightPriceScale: { borderColor: "#334155" },
      });

      rsiChartInstance.current = chart;
      const series = chart.addLineSeries({ color: "#facc15", lineWidth: 1.5 });
      series.setData(rsiData.filter((d) => typeof d.value === "number"));
    }

    // StochRSI 차트 생성
    if (stochChartRef.current) {
      const chart = createChart(stochChartRef.current, {
        height: 120,
        layout: { background: { color: "#0f172a" }, textColor: "#e2e8f0" },
        grid: { vertLines: { color: "#1e293b" }, horzLines: { color: "#1e293b" } },
        timeScale: {
          borderColor: "#334155",
          timeVisible: true,
          lockVisibleTimeRangeOnResize: true,
          tickMarkFormatter: (time) => {
            const date = new Date(time * 1000);
            return `${date.getHours().toString().padStart(2, "0")}:${date
              .getMinutes()
              .toString()
              .padStart(2, "0")}`;
          },
        },
        rightPriceScale: { borderColor: "#334155" },
      });

      stochChartInstance.current = chart;

      const kSeries = chart.addLineSeries({ color: "#eab308", lineWidth: 1.5 });
      const dSeries = chart.addLineSeries({ color: "#8b5cf6", lineWidth: 1.5 });

      kSeries.setData(
        stochRsiData
          .filter((d) => typeof d.k === "number")
          .map((d) => ({ time: d.time, value: d.k! }))
      );

      dSeries.setData(
        stochRsiData
          .filter((d) => typeof d.d === "number")
          .map((d) => ({ time: d.time, value: d.d! }))
      );
    }

    // 언마운트 시 정리
    return () => {
      rsiChartInstance.current?.remove();
      rsiChartInstance.current = undefined;
      stochChartInstance.current?.remove();
      stochChartInstance.current = undefined;
    };
  }, [rsiData, stochRsiData, mainTimeScale]);

  return (
    <div className="mt-4">
      {rsiData.length > 0 && (
        <div className="h-[120px] border border-slate-600 rounded mb-4">
          <div ref={rsiChartRef} className="h-full w-full" />
        </div>
      )}
      {stochRsiData.length > 0 && (
        <div className="h-[120px] border border-slate-600 rounded">
          <div ref={stochChartRef} className="h-full w-full" />
        </div>
      )}
    </div>
  );
};

export default SubChart;
