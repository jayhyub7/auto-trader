// 📁 src/service/positionOpenService.ts

import api from "@/shared/util/axios";
import { Exchange } from "@/constants/Exchange";
import { Timeframe } from "@/constants/TimeFrame";
import {
  Direction,
  ConditionPhase,
  IndicatorType,
  VWBBOperator,
} from "@/features/position-manager/services/PositionManagerService";

// 💰 금액 유형 상수 및 타입 정의
export const AmountTypes = {
  FIXED: "FIXED",
  PERCENT: "PERCENT",
} as const;
export type AmountType = keyof typeof AmountTypes;

export const PositionOpenStatuses = {
  IDLE: "IDLE",
  RUNNING: "RUNNING",
  SIMULATING: "SIMULATING",
  CANCELLED: "CANCELLED",
} as const;
export type PositionOpenStatus = keyof typeof PositionOpenStatuses;

export interface IndicatorCondition {
  type: IndicatorType;
  value?: number;
  k?: number;
  d?: number;
  operator: "이상" | "이하" | VWBBOperator;
  timeframe: Timeframe;
  conditionPhase: ConditionPhase;
  enabled: boolean;
}

export interface PositionOpenDto {
  id: number;
  positionId: number;
  status: PositionOpenStatus;
  amountType: AmountType;
  amount: number;
  stopLoss: number;
  takeProfit?: number;
  leverage: number; // ✅ 추가됨
}

export interface Position {
  id: number;
  title: string;
  exchange: Exchange;
  conditions: IndicatorCondition[];
  enabled: boolean;
  open?: PositionOpenDto | null;
}

export interface PositionOpenPayload {
  id?: number;
  positionId: number;
  amount: number;
  amountType: AmountType;
  stopLoss: number;
  takeProfit?: number;
  leverage: number; // ✅ 추가됨
  status: PositionOpenStatus;
}

export const fetchMyOpenPositions = async (): Promise<Position[]> => {
  const res = await api.get<Position[]>("/position-opens");
  return res.data;
};

export const saveOrUpdatePositionOpen = async (
  payload: PositionOpenPayload
): Promise<PositionOpenDto> => {
  if (payload.id) {
    const res = await api.patch<PositionOpenDto>(
      `/position-open/${payload.id}`,
      payload
    );
    return res.data;
  } else {
    const res = await api.post<PositionOpenDto>("/position-open", payload);
    return res.data;
  }
};

export const deletePositionOpen = async (positionOpenId: number) => {
  try {
    await api.delete(`/position-open/${positionOpenId}`);
  } catch (error: any) {
    if (error?.response?.status === 400) {
      throw new Error(
        "실행 중이거나 시뮬레이션 상태에서는 삭제할 수 없습니다."
      );
    }
    throw error;
  }
};
