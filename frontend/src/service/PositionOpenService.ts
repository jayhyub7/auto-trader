import api from "@/lib/axios";
import { Exchange } from "@/constants/Exchange";
import { Timeframe } from "@/constants/TimeFrame";

export type Direction = "LONG" | "SHORT";
export type ConditionPhase = "ENTRY" | "EXIT";
export type IndicatorType = "RSI" | "StochRSI" | "VWBB";
export type VWBBOperator = "상단_돌파" | "하단_돌파";

export interface IndicatorCondition {
  type: IndicatorType;
  value?: number;
  k?: number;
  d?: number;
  operator: "이상" | "이하" | VWBBOperator;
  timeframe: Timeframe;
  direction: Direction;
  conditionPhase: ConditionPhase;
}

export type PositionOpenStatus = "IDLE" | "RUNNING" | "SIMULATING" | "CANCELLED";

export interface PositionOpenDto {
  id: number;
  positionId: number;
  status: PositionOpenStatus;
  amountType: "fixed" | "percent";
  amount: number;
  stopLoss: number;
  takeProfit?: number;
}

export interface Position {
  id: number;
  title: string;
  exchange: Exchange;
  conditions: IndicatorCondition[];
  enabled: boolean;
  open?: PositionOpenDto | null; // ✅ open 정보 포함됨
}

export interface PositionOpenPayload {
  id?: number;
  positionId: number;
  amount: number;
  amountType: "fixed" | "percent";
  stopLoss: number;
  takeProfit?: number;
  status: PositionOpenStatus;
}

export const fetchMyOpenPositions = async (): Promise<Position[]> => {
  const res = await api.get<Position[]>("/position-opens"); // ✅ 컨트롤러 대응
  return res.data;
};

export const saveOrUpdatePositionOpen = async (
  payload: PositionOpenPayload
): Promise<PositionOpenDto> => {
  if (payload.id) {
    const res = await api.patch<PositionOpenDto>(`/position-open/${payload.id}`, payload);
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
      throw new Error("실행 중이거나 시뮬레이션 상태에서는 삭제할 수 없습니다.");
    }
    throw error;
  }
};