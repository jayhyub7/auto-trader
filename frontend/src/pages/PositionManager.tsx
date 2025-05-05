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
import { fetchPositions, savePositions, deletePosition  } from "@/service/positionManager";
import { handleAddCondition } from "@/components/PositionManager/handleAddCondition";


export type Direction = "LONG" | "SHORT";
export type ConditionPhase = "ENTRY" | "EXIT";
export type IndicatorType = "RSI" | "StochRSI" | "VWBB";
export type VWBBOperator = "상단 돌파" | "하단 돌파";


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

export interface IdMapping {
  tempId: number;   // 프론트 임시 ID
  realId: number;   // 백엔드 실제 저장된 ID
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
  const [selectedPhase, setSelectedPhase] = useState<"ENTRY" | "EXIT">("ENTRY");

  useEffect(() => { 
    const loadPositions = async () => {
      const result = await fetchPositions();
      if (result) setPositions(result);
    };
    loadPositions();
  }, []);

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

  const toggleSelectPosition = (id: string, force?: boolean) => {
    setSelectedPositionIds((prev) => {
      const newSet = new Set(prev);
      if (force === undefined) {
        // 기존 토글 동작
        newSet.has(id) ? newSet.delete(id) : newSet.add(id);
      } else {
        // 전체 선택/해제에서 사용
        force ? newSet.add(id) : newSet.delete(id);
      }
      return newSet;
    });
  };

  const deleteSelectedPositions = async () => {
    const idsToDelete = Array.from(selectedPositionIds);
  
    // 각각 개별 삭제 요청
    await Promise.all(idsToDelete.map(id => deletePosition(id)));
  
    // 프론트 상태도 갱신
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

  const handleSave = async () => {
    try {
      const mappings = await savePositions(positions); // tempId → realId 목록
  
      setPositions((prev) =>
        prev.map((p) => {
          const match = mappings.find((m) => m.tempId === p.id);
          return match ? { ...p, id: match.realId } : p;
        })
      );
  
      toast.success("저장 완료");
    } catch (err) {
      toast.error("저장 중 오류 발생");
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

        <input
          type="text"
          placeholder="포지션 제목을 입력하세요"
          value={positionTitle}
          onChange={(e) => setPositionTitle(e.target.value)}
          className="px-4 py-2 rounded bg-gray-800 text-white border border-gray-600 w-1/2 mx-4"
        />
        {!showConditionBox && (
          <PositionControls
            onAdd={handleAddPosition}
            onDelete={deleteSelectedPositions}
            onSave={handleSave}
          />
        )}
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
          selectedPhase={selectedPhase}
          setSelectedPhase={setSelectedPhase}
          activePositionId={activePositionId}         // ✅ 추가
          positions={positions}                       // ✅ 추가
          setPositions={setPositions}                 // ✅ 추가
          setShowConditionBox={setShowConditionBox}   // ✅ 추가
          handleAddCondition={handleAddCondition}     // ✅ 위치 중요 X
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
        setPositions={setPositions}
      />

      <ToastContainer position="top-center" autoClose={2000} />
    </div>
  );
};

export default PositionManager;
