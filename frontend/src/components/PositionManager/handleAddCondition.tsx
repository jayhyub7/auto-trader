// 📄 handleAddCondition.tsx

import { IndicatorCondition, IndicatorTypes, ConditionPhases, Directions } from "@/service/positionManagerService";
import { TIMEFRAME_LABELS } from "@/constants/TimeFrame";
import { toast } from "react-toastify";

interface Params {
  selectedIndicator: string;
  activePositionId: string | null;
  positions: any[];
  selectedDirection: "LONG" | "SHORT";
  selectedTimeframe: string;
  currentCondition: Partial<IndicatorCondition>;
  setPositions: (positions: any[]) => void;
  setCurrentCondition: (cond: Partial<IndicatorCondition>) => void;
  setSelectedIndicator: (v: string) => void;
  setShowConditionBox: (v: boolean) => void;
  selectedPhase: "ENTRY" | "EXIT";
}

export const handleAddCondition = ({
  selectedIndicator,
  activePositionId,
  positions,
  selectedDirection,
  selectedTimeframe,
  currentCondition,
  setPositions,
  setCurrentCondition,
  setSelectedIndicator,
  setShowConditionBox,
  selectedPhase,
}: Params) => {
  if (!selectedIndicator || !activePositionId) {
    toast.error("지표를 선택해주세요.");
    return;
  }

  const targetPosition = positions.find((p) => p.id === activePositionId);
  if (!targetPosition) return;

  const existingDirection = targetPosition.conditions.find((c: any) => c.direction === Directions.LONG)
    ? Directions.LONG
    : targetPosition.conditions.find((c: any) => c.direction === Directions.SHORT)
    ? Directions.SHORT
    : null;

  if (existingDirection && selectedDirection !== existingDirection) {
    toast.error(`${existingDirection} 조건만 추가할 수 있습니다.`);
    return;
  }

  const isDuplicate = targetPosition.conditions.some(
    (c: any) =>
      c.type === selectedIndicator &&
      c.timeframe === selectedTimeframe &&
      c.conditionPhase === selectedPhase
  );

  if (isDuplicate) {
    const phaseLabel = selectedPhase === ConditionPhases.ENTRY ? "진입조건" : "종료조건";
    toast.error(`${TIMEFRAME_LABELS[selectedTimeframe]} 분봉의 ${selectedIndicator} (${phaseLabel})은 이미 존재합니다.`);
    return;
  }

  // 유효성 검사
  if (selectedIndicator === IndicatorTypes.RSI) {
    if (
      currentCondition.value === undefined ||
      currentCondition.value === null ||
      isNaN(currentCondition.value)
    ) {
      toast.error("RSI 값이 입력되지 않았습니다.");
      return;
    }
  }

  if (selectedIndicator === IndicatorTypes.STOCH_RSI) {
    if (
      currentCondition.k === undefined ||
      currentCondition.d === undefined ||
      isNaN(currentCondition.k) ||
      isNaN(currentCondition.d)
    ) {
      toast.error("STOCH_RSI의 K 또는 D 값이 입력되지 않았습니다.");
      return;
    }
  }

  if (selectedIndicator === IndicatorTypes.VWBB) {
    if (!currentCondition.operator) {
      toast.error("VWBB 조건 유형을 선택해주세요.");
      return;
    }
  }

  const conditionWithTimeframe = {
    ...currentCondition,
    timeframe: selectedTimeframe,
    direction: selectedDirection,
    conditionPhase: selectedPhase,
    enabled: true,
  } as IndicatorCondition;

  setPositions(
    positions.map((p) =>
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
