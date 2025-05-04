import React from "react";
import { Timeframe, TIMEFRAME_LABELS } from "@/constants/TimeFrame";

type Direction = "LONG" | "SHORT";

type IndicatorType = "RSI" | "StochRSI";

interface IndicatorCondition {
  type: IndicatorType;
  value?: number;
  k?: number;
  d?: number;
  operator: "이상" | "이하";
  timeframe: Timeframe;
  direction: Direction;
}

interface ConditionEditorProps {
  selectedDirection: Direction;
  selectedTimeframe: Timeframe;
  selectedIndicator: string;
  currentCondition: Partial<IndicatorCondition>;
  setSelectedDirection: (dir: Direction) => void;
  setSelectedTimeframe: (tf: Timeframe) => void;
  setSelectedIndicator: (type: string) => void;
  setCurrentCondition: (cond: Partial<IndicatorCondition>) => void;
  handleAddCondition: () => void;
}

const ConditionEditor: React.FC<ConditionEditorProps> = ({
  selectedDirection,
  selectedTimeframe,
  selectedIndicator,  
  setSelectedDirection,
  setSelectedTimeframe,
  setSelectedIndicator,
  setCurrentCondition,
  handleAddCondition,
}) => {
  return (
    <div className="mt-6 border-2 border-gray-700 p-4 bg-gray-800 rounded-md relative">
      <div className="absolute top-4 right-4">
        <button
          onClick={handleAddCondition}
          className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
        >
          조건 저장
        </button>
      </div>

      <div className="text-lg text-white mb-4 flex items-center gap-4">
        <span>지표 설정</span>
        <div className="flex gap-2">
          <button
            className={`px-2 py-1 rounded ${selectedDirection === "LONG" ? "bg-green-600" : "bg-gray-600"}`}
            onClick={() => setSelectedDirection("LONG")}
          >
            롱
          </button>
          <button
            className={`px-2 py-1 rounded ${selectedDirection === "SHORT" ? "bg-red-600" : "bg-gray-600"}`}
            onClick={() => setSelectedDirection("SHORT")}
          >
            숏
          </button>
        </div>
      </div>

      <div className="flex flex-wrap gap-4 text-white mb-4">
        {Object.values(Timeframe).map((tf) => (
          <label key={tf} className="flex items-center gap-1">
            <input
              type="radio"
              name="timeframe"
              value={tf}
              checked={selectedTimeframe === tf}
              onChange={() => setSelectedTimeframe(tf)}
            />
            {TIMEFRAME_LABELS[tf]}
          </label>
        ))}
      </div>

      <select
        value={selectedIndicator}
        onChange={(e) => setSelectedIndicator(e.target.value)}
        className="mb-4 px-2 py-1 rounded bg-gray-700 text-gray-300"
      >
        <option value="">-- 지표 선택 --</option>
        <option value="RSI">RSI</option>
        <option value="StochRSI">StochRSI</option>
      </select>

      {selectedIndicator === "RSI" && (
        <div className="flex items-center gap-3">
          <input
            type="number"
            placeholder="값"
            onChange={(e) =>
              setCurrentCondition({
                type: "RSI",
                value: Number(e.target.value),
                operator: selectedDirection === "LONG" ? "이하" : "이상",
              })
            }
            className="px-2 py-1 rounded bg-gray-700 text-gray-300"
          />
          <span className="text-white text-sm">
            {selectedDirection === "LONG" ? "이하" : "이상"}
          </span>
        </div>
      )}

      {selectedIndicator === "StochRSI" && (
        <div className="flex items-center gap-3 flex-wrap">
          <label className="text-white">K</label>
          <input
            type="number"
            placeholder="값"
            onChange={(e) =>
              setCurrentCondition((prev) => ({
                ...prev,
                type: "StochRSI",
                k: Number(e.target.value),
                operator: selectedDirection === "LONG" ? "이하" : "이상",
              }))
            }
            className="px-2 py-1 rounded bg-gray-700 text-gray-300"
          />
          <span className="text-white text-sm">
            {selectedDirection === "LONG" ? "이하" : "이상"}
          </span>
          <label className="text-white ml-4">D</label>
          <input
            type="number"
            placeholder="값"
            onChange={(e) =>
              setCurrentCondition((prev) => ({
                ...prev,
                d: Number(e.target.value),
              }))
            }
            className="px-2 py-1 rounded bg-gray-700 text-gray-300"
          />
          <span className="text-white text-sm">
            {selectedDirection === "LONG" ? "이하" : "이상"}
          </span>
        </div>
      )}
    </div>
  );
};

export default ConditionEditor;