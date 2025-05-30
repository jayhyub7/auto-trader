// 추후 API 연동 시 사용될 타입 정의
export interface ExecutedReportItem {
  id: string;
  positionTitle: string;
  direction: string;
  executedPrice: number;
  profitRate: number;
  executedAt: string;
  conditions: string[];
  executionLog: string;
}
