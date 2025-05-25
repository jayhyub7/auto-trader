// 📄 ConditionEditor.tsx

import React, { useEffect, useState } from "react";
import { Timeframe, TIMEFRAME_LABELS } from "@/constants/TimeFrame";
import {
  IndicatorCondition,
  ConditionPhase,
  VWBBOperator,
  getIndicatorTypes,
  IndicatorTypeResponse,
  VWBBOperators,
  ConditionPhases,
} from "@/features/position-manager/services/PositionManagerService";

interface ConditionEditorProps {
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
  selectedTimeframe,
  selectedIndicator,
  currentCondition,
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
  const [options, setOptions] = useState<IndicatorTypeResponse[]>([]);

  useEffect(() => {
    getIndicatorTypes().then(setOptions);
  }, []);

  useEffect(() => {
    if (selectedIndicator === "VWBB") {
      const isVWBBOperator = Object.values(VWBBOperators).includes(
        currentCondition.operator as VWBBOperator
      );
      if (!isVWBBOperator) {
        setCurrentCondition((prev) => ({
          ...prev,
          type: "VWBB",
          operator: VWBBOperators.UPPER,
        }));
      }
    } else if (
      (selectedIndicator === "RSI" || selectedIndicator === "STOCH_RSI") &&
      !currentCondition.operator
    ) {
      setCurrentCondition((prev) => ({
        ...prev,
        operator: "이하",
        type: selectedIndicator as any,
      }));
    }
  }, [selectedIndicator]);

  const indicators = options.filter((o) => o.conditionType === "INDICATOR");
  const strategies = options.filter((o) => o.conditionType === "STRATEGY");
  const selectedMeta = options.find((o) => o.type === selectedIndicator);
  const isStrategy = selectedMeta?.conditionType === "STRATEGY";

  return (
    <div className="mt-6 border-2 border-gray-700 p-4 bg-gray-800 rounded-md relative">
      <div className="absolute top-4 right-4">
        <button
          className="px-4 py-2 bg-yellow-500 text-white rounded hover:bg-yellow-600"
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
          className={`px-2 py-1 rounded ${
            selectedPhase === ConditionPhases.ENTRY
              ? "bg-blue-600"
              : "bg-gray-600"
          }`}
          onClick={() => setSelectedPhase(ConditionPhases.ENTRY)}
        >
          진입조건
        </button>
        <button
          className={`px-2 py-1 rounded ${
            selectedPhase === ConditionPhases.EXIT
              ? "bg-blue-600"
              : "bg-gray-600"
          }`}
          onClick={() => setSelectedPhase(ConditionPhases.EXIT)}
        >
          종료조건
        </button>
      </div>

      {!isStrategy && selectedIndicator && (
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
      )}

      <select
        value={selectedIndicator}
        onChange={(e) => setSelectedIndicator(e.target.value)}
        className="mb-4 px-2 py-1 rounded bg-gray-700 text-gray-300"
      >
        <option value="">-- 지표/매매법 선택 --</option>
        <optgroup label="지표">
          {indicators.map((item) => (
            <option key={item.type} value={item.type}>
              {item.label}
            </option>
          ))}
        </optgroup>
        <optgroup label="매매법">
          {strategies.map((item) => (
            <option key={item.type} value={item.type}>
              {item.label}
            </option>
          ))}
        </optgroup>
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
            {["이하", "이상"].map((op) => (
              <label key={op} className="flex items-center gap-1">
                <input
                  type="radio"
                  name="rsi-operator"
                  value={op}
                  checked={currentCondition.operator === op}
                  onChange={() =>
                    setCurrentCondition((prev) => ({
                      ...prev,
                      operator: op,
                    }))
                  }
                />
                {op}
              </label>
            ))}
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
            {["이하", "이상"].map((op) => (
              <label key={op} className="flex items-center gap-1">
                <input
                  type="radio"
                  name="stoch-operator"
                  value={op}
                  checked={currentCondition.operator === op}
                  onChange={() =>
                    setCurrentCondition((prev) => ({
                      ...prev,
                      operator: op,
                    }))
                  }
                />
                {op}
              </label>
            ))}
          </div>
        </div>
      )}

      {selectedIndicator === "VWBB" && (
        <div className="flex gap-4 text-white">
          {Object.values(VWBBOperators).map((op) => (
            <label key={op} className="flex items-center gap-2">
              <input
                type="radio"
                name="vwbb"
                value={op}
                checked={currentCondition.operator === op}
                onChange={() =>
                  setCurrentCondition((prev) => ({
                    ...prev,
                    type: "VWBB",
                    operator: op,
                  }))
                }
              />
              {op}
            </label>
          ))}
        </div>
      )}
    </div>
  );
};

export default ConditionEditor;
