import React, { useEffect } from "react";
import { Timeframe, TIMEFRAME_LABELS } from "@/constants/TimeFrame";
import { IndicatorCondition, Direction, ConditionPhase, IndicatorType, VWBBOperator } from "@/service/positionManager";


interface ConditionEditorProps {
  selectedDirection: Direction;
  selectedTimeframe: Timeframe;
  selectedIndicator: string;
  currentCondition: Partial<IndicatorCondition>;  
  setSelectedTimeframe: (tf: Timeframe) => void;
  setSelectedIndicator: (type: string) => void;
  setCurrentCondition: (cond: Partial<IndicatorCondition>) => void;
  selectedPhase: ConditionPhase;
  setSelectedPhase: (phase: ConditionPhase) => void;
  activePositionId: string | null;
  positions: Position[];
  setPositions: (positions: Position[]) => void;
  setShowConditionBox: (v: boolean) => void;
  handleAddCondition: (params: {
    selectedIndicator: string;
    activePositionId: string | null;
    positions: Position[];    
    selectedTimeframe: Timeframe;
    currentCondition: Partial<IndicatorCondition>;
    setPositions: (positions: Position[]) => void;
    setCurrentCondition: (cond: Partial<IndicatorCondition>) => void;
    setSelectedIndicator: (v: string) => void;
    setShowConditionBox: (v: boolean) => void;
    selectedPhase: ConditionPhase;
  }) => void;
}

const ConditionEditor: React.FC<ConditionEditorProps> = ({
  selectedDirection,
  selectedTimeframe,
  selectedIndicator,
  currentCondition,
  setSelectedDirection,
  setSelectedTimeframe,
  setSelectedIndicator,
  setCurrentCondition,
  setShowConditionBox,
  handleAddCondition,
  selectedPhase,
  setSelectedPhase,
  activePositionId,
  positions,
  setPositions,
}) => {

  return (
    <div className="mt-6 border-2 border-gray-700 p-4 bg-gray-800 rounded-md relative">
      <div className="absolute top-4 right-4">
        <button className="px-4 py-2 bg-yellow-500 text-white rounded hover:bg-yellow-600"
          onClick={() => {
            handleAddCondition({
              selectedIndicator,
              activePositionId,
              positions,
              selectedTimeframe,
              currentCondition,
              setPositions,
              setCurrentCondition,
              setSelectedIndicator,
              setShowConditionBox,
              selectedPhase,
            });
          }}
        >
          조건 저장
        </button>
      </div>      

      <div className="flex gap-2 items-center mb-4 text-white">
        <span>조건 유형</span>
        <button
          className={`px-2 py-1 rounded ${selectedPhase === "ENTRY" ? "bg-blue-600" : "bg-gray-600"}`}
          onClick={() => setSelectedPhase("ENTRY")}
        >
          진입조건
        </button>
        <button
          className={`px-2 py-1 rounded ${selectedPhase === "EXIT" ? "bg-blue-600" : "bg-gray-600"}`}
          onClick={() => setSelectedPhase("EXIT")}
        >
          종료조건
        </button>
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
        <option value="STOCH_RSI">STOCH_RSI</option>
        <option value="VWBB">VWBB</option>
      </select>

      {selectedIndicator === "RSI" && (
      <div className="flex items-center gap-3">
        <input
          type="number"
          placeholder="값"
          onChange={(e) =>
            setCurrentCondition((prev) => ({
              ...prev,
              type: "RSI",
              value: Number(e.target.value),
            }))
          }
          className="px-2 py-1 rounded bg-gray-700 text-gray-300"
        />
        <div className="flex gap-2 items-center">
          <label className="flex items-center gap-1">
            <input
              type="radio"
              name="rsi-operator"
              value="이하"
              checked={currentCondition.operator === "이하"}
              onChange={() =>
                setCurrentCondition((prev) => ({
                  ...prev,
                  operator: "이하",
                }))
              }
            />
            이하
          </label>
          <label className="flex items-center gap-1">
            <input
              type="radio"
              name="rsi-operator"
              value="이상"
              checked={currentCondition.operator === "이상"}
              onChange={() =>
                setCurrentCondition((prev) => ({
                  ...prev,
                  operator: "이상",
                }))
              }
            />
            이상
          </label>
        </div>
      </div>
    )}

    {selectedIndicator === "STOCH_RSI" && (
      <div className="flex flex-col gap-2 text-white">
        <div className="flex items-center gap-3">
          <label>K</label>
          <input
            type="number"
            placeholder="값"
            onChange={(e) =>
              setCurrentCondition((prev) => ({
                ...prev,
                type: "STOCH_RSI",
                k: Number(e.target.value),
              }))
            }
            className="px-2 py-1 rounded bg-gray-700 text-gray-300"
          />
          <label className="ml-4">D</label>
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
        </div>
        <div className="flex gap-4 items-center mt-2">
          <label className="flex items-center gap-1">
            <input
              type="radio"
              name="stoch-operator"
              value="이하"
              checked={currentCondition.operator === "이하"}
              onChange={() =>
                setCurrentCondition((prev) => ({
                  ...prev,
                  operator: "이하",
                }))
              }
            />
            이하
          </label>
          <label className="flex items-center gap-1">
            <input
              type="radio"
              name="stoch-operator"
              value="이상"
              checked={currentCondition.operator === "이상"}
              onChange={() =>
                setCurrentCondition((prev) => ({
                  ...prev,
                  operator: "이상",
                }))
              }
            />
            이상
          </label>
        </div>
      </div>
    )}

      {selectedIndicator === "VWBB" && (
        <div className="flex gap-4 text-white">
          <label className="flex items-center gap-2">
            <input
              type="radio"
              name="vwbb"
              value="상단 돌파"
              onChange={() =>
                setCurrentCondition((prev) => ({
                  ...prev,
                  type: "VWBB",
                  operator: "상단 돌파",
                }))
              }
            />
            상단 돌파
          </label>
          <label className="flex items-center gap-2">
            <input
              type="radio"
              name="vwbb"
              value="하단 돌파"
              onChange={() =>
                setCurrentCondition((prev) => ({
                  ...prev,
                  type: "VWBB",
                  operator: "하단 돌파",
                }))
              }
            />
            하단 돌파
          </label>
        </div>
      )}

    </div>
  );
};

export default ConditionEditor;
