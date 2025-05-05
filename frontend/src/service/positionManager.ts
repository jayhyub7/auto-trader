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

export interface Position {
  id: string;
  title: string;
  exchange: Exchange;
  conditions: IndicatorCondition[];
  enabled: boolean;
}

export const fetchPositions = async (): Promise<Position[]> => {
  const res = await api.get<Position[]>("/positions");
  return res.data;
};

export const savePositions = async (positions: Position[]): Promise<IdMapping[]> => {
  const payload = positions.map(p => ({
    id: isValidLong(p.id) ? Number(p.id) : null, // Long만 전송
    tempId: p.id, // 프론트의 임시 ID
    title: p.title,
    exchange: p.exchange,
    enabled: p.enabled,
    conditions: p.conditions,
  }));

  const res = await api.post<IdMapping[]>("/positions", payload);
  return res.data;
};

const isValidLong = (id: string): boolean => {
  // 숫자로만 이루어진 경우 (e.g. "123", "45") → Long 가능
  return /^\d+$/.test(id);
};

export const deletePosition = async (id: string): Promise<void> => {
  await api.delete(`/positions/${id}`);
};
