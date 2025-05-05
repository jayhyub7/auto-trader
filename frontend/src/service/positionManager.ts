import api from "@/lib/axios";
import { Exchange } from "@/constants/Exchange";
import { Timeframe } from "@/constants/TimeFrame";

export type Direction = "LONG" | "SHORT";
export type ConditionPhase = "ENTRY" | "EXIT";
export type IndicatorType = "RSI" | "StochRSI" | "VWBB";
export type VWBBOperator = "상단 돌파" | "하단 돌파";

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
  const res = await api.post<IdMapping[]>("/positions", positions);
  return res.data;
};

export const deletePosition = async (id: string): Promise<void> => {
  await api.delete(`/positions/${id}`);
};
