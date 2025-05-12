// components/PositionManager/index.tsx
// 메인 엔트리: 상태 관리 및 레이아웃

import React, { useState, useEffect } from "react";
import BitcoinChart from "@/components/BitcoinChart";
import PositionControls from "@/components/PositionManager/PositionControls";
import ConditionEditor from "@/components/PositionManager/ConditionEditor";
import PositionTable from "@/components/PositionManager/PositionTable";
import { Exchange, EXCHANGE_LABELS } from "@/constants/Exchange";
import { Timeframe } from "@/constants/TimeFrame";
import { v4 as uuidv4 } from "uuid";
import { toast, ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import { fetchPositions, savePositions, deletePosition  } from "@/service/positionManagerService";
import { ConditionPhase, IndicatorType, VWBBOperator, IndicatorCondition, Position, IdMapping, Direction } from "@/service/positionManagerService";
import { handleAddCondition } from "@/components/PositionManager/handleAddCondition";

const PositionManager = () => {
  const [showChart, setShowChart] = useState(true);
  const [selectedExchange, setSelectedExchange] = useState<Exchange | "">("");
  const [positions, setPositions] = useState<Position[]>([]);
  const [positionTitle, setPositionTitle] = useState("");
  const [selectedPositionIds, setSelectedPositionIds] = useState<Set<string>>(new Set());
  const [selectedDirection, setSelectedDirection] = useState<Direction>("LONG");
  const [showConditionBox, setShowConditionBox] = useState(false);
  const [activePositionId, setActivePositionId] = useState<string | null>(null);  
  const [selectedIndicator, setSelectedIndicator] = useState<string>("RSI");
  const [selectedTimeframe, setSelectedTimeframe] = useState<Timeframe>(Timeframe.ONE_MINUTE);
  const [selectedPhase, setSelectedPhase] = useState<"ENTRY" | "EXIT">("ENTRY");
  const [currentCondition, setCurrentCondition] = useState<Partial<IndicatorCondition>>({
    operator: "이하"
  });
  

  useEffect(() => { 
    const loadPositions = async () => {
      const result = await fetchPositions();
      if (result) {
        const updated = result.map(p => ({
          ...p,
          direction: p.direction || "LONG"
        }));
        setPositions(updated);
      }
    };
    loadPositions();
  }, []);

  const handleAddPosition = (direction: "LONG" | "SHORT") => {
    if (!selectedExchange) {
      toast.warning("📛 거래소를 먼저 선택해주세요.");
      return;
    }
  
    const newPosition: Position = {
      id: uuidv4(),
      title: "",
      direction,
      exchange: selectedExchange,
      conditions: [],
      open: [],
      enabled: true,
    };
  
    setPositions((prev) => [...prev, newPosition]);
  };

  const toggleSelectPosition = (id: string, force?: boolean) => {
    setSelectedPositionIds((prev) => {
      const newSet = new Set(prev);
      if (force === undefined) {
        newSet.has(id) ? newSet.delete(id) : newSet.add(id);
      } else {
        force ? newSet.add(id) : newSet.delete(id);
      }
      return newSet;
    });
  };

  const deleteSelectedPositions = async () => {
    const idsToDelete = Array.from(selectedPositionIds);
    await Promise.all(idsToDelete.map(id => deletePosition(id)));
    setPositions(prev => prev.filter(p => !selectedPositionIds.has(p.id)));
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

  const toggleConditionEnabled = (positionId: string, conditionIndex: number) => {
    setPositions((prev) =>
      prev.map((p) =>
        p.id === positionId
          ? {
              ...p,
              conditions: p.conditions.map((cond, idx) =>
                idx === conditionIndex ? { ...cond, enabled: !cond.enabled } : cond
              ),
            }
          : p
      )
    );
  };

  const handleSave = async () => {
    try {
      const mappings = await savePositions(positions);
      setPositions((prev) =>
        prev.map((p) => {
          const match = mappings.find((m) => m.tempId === p.id);
          return match ? { ...p, id: match.realId } : p;
        })
      );
      toast.success("저장 완료");
    } catch (err) {     
      toast.error(err.message);
    }
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
        {/*
        <input
          type="text"
          placeholder="포지션 제목을 입력하세요"
          value={positionTitle}
          onChange={(e) => setPositionTitle(e.target.value)}
          className="px-4 py-2 rounded bg-gray-800 text-white border border-gray-600 w-1/2 mx-4"
        />
        */}

        {!showConditionBox && (
          <PositionControls
            onAddLong={() => handleAddPosition("LONG")}
            onAddShort={() => handleAddPosition("SHORT")}
            onDelete={deleteSelectedPositions}
            onSave={handleSave}
            showConditionBox={showConditionBox}
          />
        )}
      </div>

      {showConditionBox && (
        <ConditionEditor
          selectedTimeframe={selectedTimeframe}
          setSelectedTimeframe={setSelectedTimeframe}
          selectedIndicator={selectedIndicator}
          setSelectedIndicator={setSelectedIndicator}
          currentCondition={currentCondition}
          setCurrentCondition={setCurrentCondition}
          selectedPhase={selectedPhase}
          setSelectedPhase={setSelectedPhase}
          activePositionId={activePositionId}
          positions={positions}
          setPositions={setPositions}
          setShowConditionBox={setShowConditionBox}
          handleAddCondition={handleAddCondition}
        />
      )}

      <PositionTable
        positions={positions}
        selectedPositionIds={selectedPositionIds}
        toggleSelectPosition={toggleSelectPosition}
        toggleEnabled={toggleEnabled}
        deleteCondition={deleteCondition}
        toggleConditionEnabled={toggleConditionEnabled}
        setShowConditionBox={setShowConditionBox}
        setActivePositionId={setActivePositionId}
        setPositions={setPositions}
      />

      <div className="flex items-center justify-end mt-2 text-sm text-yellow-400">
        ⚠️ 실행 중 또는 시뮬레이션 중인 포지션은 수정하더라도 반영되지 않습니다.
      </div>

      <ToastContainer position="top-center" autoClose={2000} />
    </div>
  );
};

export default PositionManager;
