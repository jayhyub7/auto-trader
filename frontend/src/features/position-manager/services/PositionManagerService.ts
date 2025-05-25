import api from "@/shared/util/axios";
import { Exchange } from "@/constants/Exchange";
import { Timeframe } from "@/constants/TimeFrame";

// 런타임 상수 객체와 타입 동시 정의
export const Directions = {
  LONG: "LONG",
  SHORT: "SHORT",
} as const;
export type Direction = keyof typeof Directions;

export const ConditionPhases = {
  ENTRY: "ENTRY",
  EXIT: "EXIT",
} as const;
export type ConditionPhase = keyof typeof ConditionPhases;

export const IndicatorTypes = {
  RSI: "RSI",
  STOCH_RSI: "STOCH_RSI",
  VWBB: "VWBB",
  STOP_HUNTING: "스탑헌팅",
  FIVE_CANDLE: "5캔들",
  TEST: "테스트",
} as const;

export const IndicatorOptions: IndicatorType[] = ["RSI", "STOCH_RSI", "VWBB"];
export const StrategyOptions: IndicatorType[] = ["스탑헌팅", "5캔들", "TEST"];
export const VWBBOperators = {
  UPPER: "상단_돌파",
  LOWER: "하단_돌파",
} as const;
export type VWBBOperator = (typeof VWBBOperators)[keyof typeof VWBBOperators];

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

export interface Position {
  id: string;
  title: string;
  exchange: Exchange;
  direction: Direction;
  conditions: IndicatorCondition[];
  enabled: boolean;
}

export interface IdMapping {
  tempId: string;
  realId: number;
}
// ✅ 유효성 검사 결과 구조
interface ValidationResult {
  isValid: boolean;
  message?: string;
}

export const fetchPositions = async (): Promise<Position[]> => {
  const res = await api.get<Position[]>("/positions");
  return res.data;
};
// ✅ 포지션 유효성 검사 함수 (확장 가능)
const validatePosition = (position: Position): ValidationResult => {
  if (!position.title?.trim()) {
    return { isValid: false, message: "포지션 제목은 필수입니다." };
  }
  if (!position.conditions || position.conditions.length === 0) {
    return { isValid: false, message: "조건이 1개 이상 필요합니다." };
  }
  return { isValid: true };
};

export const savePositions = async (
  positions: Position[]
): Promise<IdMapping[]> => {
  for (const p of positions) {
    const result = validatePosition(p);
    if (!result.isValid) {
      throw new Error(result.message);
    }
  }

  const payload = positions.map((p) => ({
    id: isValidLong(p.id) ? Number(p.id) : null,
    tempId: p.id,
    title: p.title,
    exchange: p.exchange,
    direction: p.direction,
    enabled: p.enabled,
    conditions: p.conditions.map((c) => {
      const isIndicator = IndicatorOptions.includes(c.type);

      const condition: any = {
        type: c.type,
        conditionPhase: c.conditionPhase,
        operator: c.operator,
        value: c.value,
        k: c.k,
        d: c.d,
        enabled: c.enabled,
      };

      if (isIndicator) {
        condition.timeframe = c.timeframe;
      }

      return condition;
    }),
  }));

  const res = await api.post<IdMapping[]>("/positions", payload);
  return res.data;
};

const isValidLong = (id: string): boolean => {
  return /^\d+$/.test(id);
};

export const deletePosition = async (id: number) => {
  return await api.delete(`/position/${id}`);
};

export const deletePositions = async (ids: number[]) => {
  return await api.post("/positions/delete", ids);
};
