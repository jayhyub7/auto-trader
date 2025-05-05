import api from "@/lib/axios";
import { Exchange } from "@/constants/Exchange";
import { Timeframe } from "@/constants/TimeFrame";

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

export const fetchPositions = async (): Promise<Position[]> => {
  const res = await api.get<Position[]>("/positions");
  return res.data;
};

export const savePositions = async (positions: Position[]): Promise<void> => {
  await api.post("/positions", positions);
};

export const deletePosition = async (id: string): Promise<void> => {
  await api.delete(`/positions/${id}`);
};
