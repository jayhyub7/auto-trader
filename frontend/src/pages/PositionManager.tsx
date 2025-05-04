// components/PositionManager/index.tsx
// 메인 엔트리: 상태 관리 및 레이아웃
import React, { useState } from "react";
import BitcoinChart from "@/components/BitcoinChart";
import PositionControls from "@/components/PositionManager/PositionControls";
import ConditionEditor from "@/components/PositionManager/ConditionEditor";
import PositionTable from "@/components/PositionManager/PositionTable";
import { Exchange, EXCHANGE_LABELS } from "@/constants/Exchange";
import { Timeframe } from "@/constants/TimeFrame";
import { v4 as uuidv4 } from "uuid";
import { toast, ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";

export interface IndicatorCondition {
  type: "RSI" | "StochRSI";
  value?: number;
  k?: number;
  d?: number;
  operator: "이상" | "이하";
  timeframe: Timeframe;
  direction: "LONG" | "SHORT";
}

export interface Position {
  id: string;
  title: string;
  exchange: Exchange;
  conditions: IndicatorCondition[];
  enabled: boolean;
}

const PositionManager = () => {
  const [showChart, setShowChart] = useState(true);
  const [selectedExchange, setSelectedExchange] = useState<Exchange | "">("");
  const [positions, setPositions] = useState<Position[]>([]);
  const [positionTitle, setPositionTitle] = useState("");
  const [selectedPositionIds, setSelectedPositionIds] = useState<Set<string>>(new Set());
  const [selectedDirection, setSelectedDirection] = useState<"LONG" | "SHORT">("LONG");
  const [showConditionBox, setShowConditionBox] = useState(false);
  const [activePositionId, setActivePositionId] = useState<string | null>(null);
  const [selectedTimeframe, setSelectedTimeframe] = useState<Timeframe>(Timeframe.ONE_MINUTE);
  const [selectedIndicator, setSelectedIndicator] = useState<string>("");
  const [currentCondition, setCurrentCondition] = useState<Partial<IndicatorCondition>>({});

  const handleAddPosition = () => {
    if (!selectedExchange) return toast.error("거래소를 선택해주세요.");
    if (!positionTitle.trim()) return toast.error("포지션 제목을 입력해주세요.");
    const newPosition: Position = {
      id: uuidv4(),
      title: positionTitle,
      exchange: selectedExchange as Exchange,
      conditions: [],
      enabled: true,
    };
    setPositions((prev) => [...prev, newPosition]);
    setPositionTitle("");
  };

  const toggleSelectPosition = (id: string) => {
    setSelectedPositionIds((prev) => {
      const newSet = new Set(prev);
      newSet.has(id) ? newSet.delete(id) : newSet.add(id);
      return newSet;
    });
  };

  const deleteSelectedPositions = () => {
    setPositions((prev) => prev.filter((p) => !selectedPositionIds.has(p.id)));
    setSelectedPositionIds(new Set());
  };

  const toggleEnabled = (id: string) => {
    setPositions((prev) =>
      prev.map((p) => (p.id === id ? { ...p, enabled: !p.enabled } : p))
    );
  };

  const deleteCondition = (positionId: string, conditionIndex?: number) => {
    setPositions((prev) =>
      prev.map((p) =>
        p.id === positionId
          ? {
              ...p,
              conditions: conditionIndex === undefined
                ? []
                : p.conditions.filter((_, idx) => idx !== conditionIndex),
            }
          : p
      )
    );
  };

  const savePositions = () => {
    toast.success("변경사항이 저장되었습니다.");
    // API 저장 로직 추가 가능
  };

  const handleAddCondition = () => {
    if (!selectedIndicator || !activePositionId) {
      toast.error("지표를 선택해주세요.");
      return;
    }

    const targetPosition = positions.find((p) => p.id === activePositionId);
    if (!targetPosition) return;

    const existingDirection = targetPosition.conditions.find((c) => c.direction === "LONG")
      ? "LONG"
      : targetPosition.conditions.find((c) => c.direction === "SHORT")
      ? "SHORT"
      : null;

    if (existingDirection && selectedDirection !== existingDirection) {
      toast.error(`${existingDirection} 조건만 추가할 수 있습니다.`);
      return;
    }

    const isDuplicate = targetPosition.conditions.some(
      (c) => c.type === selectedIndicator && c.timeframe === selectedTimeframe
    );

    if (isDuplicate) {
      toast.error(`${selectedTimeframe} 분봉의 ${selectedIndicator} 조건은 이미 존재합니다.`);
      return;
    }

    if (selectedIndicator === "RSI") {
      if (
        currentCondition.value === undefined ||
        currentCondition.value === null ||
        isNaN(currentCondition.value)
      ) {
        toast.error("RSI 값이 입력되지 않았습니다.");
        return;
      }
    }

    if (selectedIndicator === "StochRSI") {
      if (
        currentCondition.k === undefined ||
        currentCondition.d === undefined ||
        isNaN(currentCondition.k) ||
        isNaN(currentCondition.d)
      ) {
        toast.error("StochRSI의 K 또는 D 값이 입력되지 않았습니다.");
        return;
      }
    }

    const conditionWithTimeframe = {
      ...currentCondition,
      timeframe: selectedTimeframe,
      direction: selectedDirection,
    } as IndicatorCondition;

    setPositions((prev) =>
      prev.map((p) =>
        p.id === activePositionId
          ? {
              ...p,
              conditions: [...p.conditions, conditionWithTimeframe],
            }
          : p
      )
    );

    setCurrentCondition({});
    setSelectedIndicator("");
    setShowConditionBox(false);
  };

  return (
    <div className="p-4">
      <div className="flex items-center mb-2">
        <span
          className="text-white text-lg cursor-pointer hover:text-yellow-400"
          onClick={() => setShowChart((prev) => !prev)}
        >
          📈 포지션 차트 ({showChart ? "클릭해서 숨기기" : "클릭해서 보이기"})
        </span>
      </div>

      {showChart && <BitcoinChart />}

      <div className="flex items-center justify-between mb-2">
        <select
          value={selectedExchange}
          onChange={(e) => setSelectedExchange(e.target.value as Exchange)}
          className="px-4 py-2 rounded bg-gray-800 text-white border border-gray-600"
        >
          <option value="">거래소 선택</option>
          {Object.values(Exchange).map((ex) => (
            <option key={ex} value={ex}>
              {EXCHANGE_LABELS[ex]}
            </option>
          ))}
        </select>

        <input
          type="text"
          placeholder="포지션 제목을 입력하세요"
          value={positionTitle}
          onChange={(e) => setPositionTitle(e.target.value)}
          className="px-4 py-2 rounded bg-gray-800 text-white border border-gray-600 w-1/2 mx-4"
        />

        <PositionControls
          onAdd={handleAddPosition}
          onDelete={deleteSelectedPositions}
          onSave={savePositions}
        />
      </div>

      {showConditionBox && (
        <ConditionEditor
          selectedDirection={selectedDirection}
          setSelectedDirection={setSelectedDirection}
          selectedTimeframe={selectedTimeframe}
          setSelectedTimeframe={setSelectedTimeframe}
          selectedIndicator={selectedIndicator}
          setSelectedIndicator={setSelectedIndicator}
          currentCondition={currentCondition}
          setCurrentCondition={setCurrentCondition}
          handleAddCondition={handleAddCondition}
        />
      )}

      <PositionTable
        positions={positions}
        selectedPositionIds={selectedPositionIds}
        toggleSelectPosition={toggleSelectPosition}
        toggleEnabled={toggleEnabled}
        deleteCondition={deleteCondition}
        setShowConditionBox={setShowConditionBox}
        setActivePositionId={setActivePositionId}
      />

      <ToastContainer position="top-center" autoClose={2000} />
    </div>
  );
};

export default PositionManager;