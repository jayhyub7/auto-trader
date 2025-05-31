// 📁 src/service/ExecutedReportService.ts

import api from "@/shared/util/axios";

export interface ConditionDto {
  indicator: string;
  operator: string;
  value: number;
  timeframe: string;
  phase: string;
}

export interface ExecutedReportResponseDto {
  executedAt: string;
  positionName: string;
  direction: string;
  executedPrice: number;
  profitRate: number;
  observedPrice: number;
  slippage: number;
  tpSlRegistered: boolean;
  executionLog: string;
  conditions: ConditionDto[];
}

export async function getExecutedReports(): Promise<
  ExecutedReportResponseDto[]
> {
  const res = await api.get<ExecutedReportResponseDto[]>("/executed-reports");
  return res.data;
}
