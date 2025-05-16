import { useEffect, useState } from "react";
import BitcoinChart from "@/components/BitcoinChart";
import PositionControls from "@/features/position-manager/components/PositionControls";
import ConditionEditor from "@/features/position-manager/components/ConditionEditor";
import PositionTable from "@/features/position-manager/components/PositionTable";
import {
  Position,
  fetchPositions,
  savePositions,
  deletePosition,
  deletePositions,
  Directions,
  ConditionPhases,
  IndicatorTypes,
} from "@/features/position-manager/services/PositionManagerService";
import { handleAddCondition } from "@/features/position-manager/components/handleAddCondition";
import { v4 as uuidv4 } from "uuid";
import { toast } from "react-toastify";

const PositionManager = () => {
  const [positions, setPositions] = useState<Position[]>([]);
  const [selectedPositionIds, setSelectedPositionIds] = useState<Set<string>>(
    new Set()
  );
  const [showConditionBox, setShowConditionBox] = useState(false);
  const [activePositionId, setActivePositionId] = useState<string | null>(null);

  const [selectedTimeframe, setSelectedTimeframe] = useState("1m");
  const [selectedIndicator, setSelectedIndicator] = useState("");
  const [selectedPhase, setSelectedPhase] = useState(ConditionPhases.ENTRY);
  const [currentCondition, setCurrentCondition] = useState<any>({});

  const load = async () => {
    const result = await fetchPositions();
    setPositions(result);
  };

  useEffect(() => {
    load();
  }, []);

  const toggleSelectPosition = (id: string) => {
    setSelectedPositionIds((prev) => {
      const copy = new Set(prev);
      if (copy.has(id)) copy.delete(id);
      else copy.add(id);
      return copy;
    });
  };

  const toggleEnabled = (id: string) => {
    const updated = positions.map((p) =>
      p.id === id ? { ...p, enabled: !p.enabled } : p
    );
    setPositions(updated);
  };

  const toggleConditionEnabled = (positionId: string, index: number) => {
    const updated = positions.map((p) =>
      p.id === positionId
        ? {
            ...p,
            conditions: p.conditions.map((c, i) =>
              i === index ? { ...c, enabled: !c.enabled } : c
            ),
          }
        : p
    );
    setPositions(updated);
  };

  const onAddLong = () => {
    const newPosition: Position = {
      id: uuidv4(),
      title: "📈 롱 포지션",
      direction: Directions.LONG,
      exchange: "BINANCE",
      enabled: true,
      conditions: [],
    };
    setPositions((prev) => [...prev, newPosition]);
  };

  const onAddShort = () => {
    const newPosition: Position = {
      id: uuidv4(),
      title: "📉 숏 포지션",
      direction: Directions.SHORT,
      exchange: "BINANCE",
      enabled: true,
      conditions: [],
    };
    setPositions((prev) => [...prev, newPosition]);
  };

  const handleSaveAll = async () => {
    try {
      await savePositions(positions);
      await load();
      toast.success("저장 완료");
    } catch (e: any) {
      toast.error(e.message);
    }
  };

  const deleteSelectedPositions = async () => {
    const tempIds = Array.from(selectedPositionIds);

    const savedIds: number[] = [];
    const newIds: string[] = [];

    for (const id of tempIds) {
      if (!isNaN(Number(id))) {
        savedIds.push(Number(id)); // 서버에 보낼 Long ID
      } else {
        newIds.push(id); // 프론트에서만 제거할 가짜 ID
      }
    }

    // 저장되지 않은 포지션은 프론트에서 바로 제거
    setPositions((prev) => prev.filter((p) => !newIds.includes(p.id)));

    // 저장된 포지션은 서버로 삭제 요청
    if (savedIds.length > 0) {
      await deletePositions(savedIds);
      toast.success("삭제 완료");
      await load();
    }

    setSelectedPositionIds(new Set());
  };

  const deleteCondition = (positionId: string, index?: number) => {
    const updated = positions.map((p) =>
      p.id === positionId
        ? {
            ...p,
            conditions:
              index !== undefined
                ? p.conditions.filter((_, i) => i !== index)
                : [],
          }
        : p
    );
    setPositions(updated);
  };

  return (
    <div className="p-6 space-y-4">
      <BitcoinChart />
      <PositionControls
        positions={positions}
        setPositions={setPositions}
        onDelete={deleteSelectedPositions}
        onAddLong={onAddLong}
        onAddShort={onAddShort}
        onSave={handleSaveAll}
      />
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
      {showConditionBox && activePositionId && (
        <ConditionEditor
          selectedTimeframe={selectedTimeframe}
          selectedIndicator={selectedIndicator}
          currentCondition={currentCondition}
          setSelectedTimeframe={setSelectedTimeframe}
          setSelectedIndicator={setSelectedIndicator}
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
    </div>
  );
};

export default PositionManager;
