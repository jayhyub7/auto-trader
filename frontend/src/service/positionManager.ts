import api from "@/lib/axios";
import { Exchange } from "@/constants/Exchange";
import { Timeframe } from "@/constants/TimeFrame";

export type Direction = "LONG" | "SHORT";
export type ConditionPhase = "ENTRY" | "EXIT";
export type IndicatorType = "RSI" | "STOCH_RSI" | "VWBB";
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

export interface IdMapping {
  tempId: number;   // 프론트 임시 ID
  realId: number;   // 백엔드 실제 저장된 ID
}

export const fetchPositions = async (): Promise<Position[]> => {
  const res = await api.get<Position[]>("/positions");
  return res.data;
};

export const savePositions = async (positions: Position[]): Promise<IdMapping[]> => {
  const payload = positions.map(p => ({
    id: isValidLong(p.id) ? Number(p.id) : null,
    tempId: p.id,
    title: p.title,
    exchange: p.exchange,
    enabled: p.enabled,
    conditions: p.conditions.map((c) => ({
      ...c,
      type: c.type,                    
      direction: c.direction,         
      conditionPhase: c.conditionPhase 
      // operator는 한글이므로 그대로 전송
    }))
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
