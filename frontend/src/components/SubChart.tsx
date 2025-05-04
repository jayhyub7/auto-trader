import React, { useEffect, useRef } from "react";
import { createChart } from "lightweight-charts";

interface LineData {
  time: number;
  value: number;
}

interface SubChartProps {
  rsiData?: LineData[];
  stochRsiData?: LineData[];
  mainTimeScale: ReturnType<typeof createChart>["timeScale"];
}

const SubChart: React.FC<SubChartProps> = ({ rsiData = [], stochRsiData = [], mainTimeScale }) => {
  const rsiChartRef = useRef<HTMLDivElement>(null);
  const stochChartRef = useRef<HTMLDivElement>(null);
  const rsiChartInstance = useRef<ReturnType<typeof createChart>>();
  const stochChartInstance = useRef<ReturnType<typeof createChart>>();

  useEffect(() => {
    if (rsiChartRef.current) {
      const chart = createChart(rsiChartRef.current, {
        height: 120,
        layout: {
          background: { color: "#0f172a" },
          textColor: "#e2e8f0",
        },
        grid: {
          vertLines: { color: "#1e293b" },
          horzLines: { color: "#1e293b" },
        },
        timeScale: {
          borderColor: "#334155",
          timeVisible: true,
          lockVisibleTimeRangeOnResize: true,
          tickMarkFormatter: (time) => {
            const date = new Date((time as number) * 1000);
            const hours = date.getHours().toString().padStart(2, "0");
            const minutes = date.getMinutes().toString().padStart(2, "0");
            return `${hours}:${minutes}`;
          },
        },
        rightPriceScale: { borderColor: "#334155" },
      });

      rsiChartInstance.current = chart;

      const logicalRange = mainTimeScale?.getVisibleLogicalRange?.();
      if (logicalRange) {
        chart.timeScale().setVisibleLogicalRange(logicalRange);
      }

      const series = chart.addLineSeries({ color: "yellow", lineWidth: 1.5 });
      series.setData(rsiData.filter(d => typeof d.value === "number"));
    }

    if (stochChartRef.current) {
      const chart = createChart(stochChartRef.current, {
        height: 120,
        layout: {
          background: { color: "#0f172a" },
          textColor: "#e2e8f0",
        },
        grid: {
          vertLines: { color: "#1e293b" },
          horzLines: { color: "#1e293b" },
        },
        timeScale: {
          borderColor: "#334155",
          timeVisible: true,
          lockVisibleTimeRangeOnResize: true,
          tickMarkFormatter: (time) => {
            const date = new Date((time as number) * 1000);
            const hours = date.getHours().toString().padStart(2, "0");
            const minutes = date.getMinutes().toString().padStart(2, "0");
            return `${hours}:${minutes}`;
          },
        },
        rightPriceScale: { borderColor: "#334155" },
      });

      stochChartInstance.current = chart;

      const logicalRange = mainTimeScale?.getVisibleLogicalRange?.();
      if (logicalRange) {
        chart.timeScale().setVisibleLogicalRange(logicalRange);
      }

      const series = chart.addLineSeries({ color: "violet", lineWidth: 1.5 });
      series.setData(stochRsiData.filter(d => typeof d.value === "number"));
    }

    return () => {
      rsiChartInstance.current?.remove();
      stochChartInstance.current?.remove();
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
