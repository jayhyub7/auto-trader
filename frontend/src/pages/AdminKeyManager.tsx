import React, { useEffect, useState } from "react";
import { toast, ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";

import {
  fetchAdminKey,
  saveAdminKey,
  deleteAdminKey,
  AdminKey,
} from "@/service/AdminKeyService";

const AdminKeyManager = () => {
  const [adminKey, setAdminKey] = useState<AdminKey>({
    apiKey: "",
    secretKey: "",
    exchange: "BINANCE",
    validated: false,
  });

  useEffect(() => {
    fetchAdminKey()
      .then((res) => {
        if (res) setAdminKey(res);
      })
      .catch(() => console.warn("관리자 키 없음"));
  }, []);

  const handleChange = (field: keyof AdminKey, value: string) => {
    setAdminKey((prev) => ({
      ...prev,
      [field]: value.trim(),
    }));
  };

  const handleSave = async () => {
    if (!adminKey.apiKey || !adminKey.secretKey) {
      toast.error("❗ 필수값이 비었습니다.");
      return;
    }

    try {
      const saved = await saveAdminKey({
        apiKey: adminKey.apiKey,
        secretKey: adminKey.secretKey,
      });

      setAdminKey(saved);

      toast[saved.validated ? "success" : "warning"](
        saved.validated ? "✅ 인증 성공" : "❌ 인증 실패 - 키가 저장되었습니다."
      );
    } catch {
      toast.error("🚨 오류가 발생했습니다.");
    }
  };

  const handleDelete = async () => {
    await deleteAdminKey();
    setAdminKey({
      apiKey: "",
      secretKey: "",
      exchange: "BINANCE",
      validated: false,
    });
    toast.info("🗑️ 삭제되었습니다.");
  };

  const { apiKey, secretKey, validated } = adminKey;
  const isSaved = !!(apiKey && secretKey);

  return (
    <div className="p-6 max-w-3xl mx-auto text-gray-200">
      <ToastContainer position="top-center" />
      <h1 className="text-2xl font-bold mb-8">🔐 관리자 인증키</h1>

      <div className="mb-6 bg-gray-800 rounded-xl p-4 shadow">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-xl font-semibold">Binance API 키</h2>
          {isSaved && (
            <span
              className={`text-sm px-2 py-1 rounded-full ${
                validated ? "bg-green-600 text-white" : "bg-red-600 text-white"
              }`}
            >
              {validated ? "인증됨" : "인증 안됨"}
            </span>
          )}
        </div>

        <div className="space-y-3">
          <input
            className="bg-gray-900 border border-gray-600 p-2 rounded w-full"
            placeholder="API Key"
            value={apiKey}
            onChange={(e) => handleChange("apiKey", e.target.value)}
          />
          <input
            className="bg-gray-900 border border-gray-600 p-2 rounded w-full"
            placeholder="Secret Key"
            type="password"
            value={secretKey}
            onChange={(e) => handleChange("secretKey", e.target.value)}
          />
          <div className="flex gap-3">
            <button
              className="bg-green-600 hover:bg-green-700 px-4 py-2 rounded"
              onClick={handleSave}
            >
              저장
            </button>
            <button
              className="bg-red-600 hover:bg-red-700 px-4 py-2 rounded"
              onClick={handleDelete}
            >
              삭제
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AdminKeyManager;
