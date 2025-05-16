import api from "@/shared/util/axios";

export interface AdminKey {
  id?: number;
  exchange: "BINANCE";
  apiKey: string;
  secretKey: string;
  validated: boolean;
}

// 🔍 조회
export const fetchAdminKey = async (): Promise<AdminKey | null> => {
  try {
    const res = await api.get<AdminKey>("/admin-key");
    return res.data;
  } catch (err: any) {
    if (err?.response?.status === 204) return null; // No Content
    throw err;
  }
};

// 💾 저장 (create or update)
export const saveAdminKey = async (key: Pick<AdminKey, "apiKey" | "secretKey">): Promise<AdminKey> => {
  const res = await api.post<AdminKey>("/admin-key", {
    ...key,
    exchange: "BINANCE", // 고정값
  });
  return res.data;
};

// ❌ 삭제
export const deleteAdminKey = async (): Promise<void> => {
  await api.delete("/admin-key");
};
