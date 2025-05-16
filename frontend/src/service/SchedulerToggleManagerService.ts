import api from "@/shared/util/axios";

export const SchedulerTypes = {
  ENTRY: "ENTRY",
  EXIT: "EXIT",
  BALANCE: "BALANCE",
  INDICATOR: "INDICATOR",
  FX: "FX",
} as const;
export type SchedulerType = keyof typeof SchedulerTypes;

export interface SchedulerStatusDto {
  type: SchedulerType;
  enabled: boolean;
  log: boolean;
}

export const getSchedulerStatus = async (): Promise<SchedulerStatusDto[]> => {
  const res = await api.get<SchedulerStatusDto[]>("/schedulers"); // ✅ /api 생략
  return res.data;
};

export const updateSchedulerStatus = async (
  dto: SchedulerStatusDto
): Promise<void> => {
  await api.post(`/schedulers/${dto.type}`, dto); // ✅ /api 생략
};
