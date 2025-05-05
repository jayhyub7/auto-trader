import api from "@/lib/axios";

export type PositionOpenStatus = "idle" | "running" | "simulating" | "cancelled";

export interface Position {
  id: string;
  title: string;
  exchange: string;
  conditions: any[];
  enabled: boolean;
}

export interface PositionOpenPayload {
  id?: string; // positionOpenId - optional for create, required for update
  positionId: string;
  takeProfit?: number;   // optional
  stopLoss: number;      // required
  amount: number;        // fixed or calculated from percent
  amountType: "fixed" | "percent";
  status: PositionOpenStatus;
}

export const saveOrUpdatePositionOpen = async (payload: PositionOpenPayload): Promise<void> => {
  if (payload.id) {
    // 기존 ID가 있으면 PATCH (수정)
    await api.patch(`/position-open/${payload.id}`, payload);
  } else {
    // ID가 없으면 POST (신규 생성)
    await api.post("/position-open", payload);
  }
};
