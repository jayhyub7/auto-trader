import React, { useEffect, useState } from "react";
import { Exchange, EXCHANGE_LABELS } from "../constants/Exchange";
import api from "@/shared/util/axios";
import { ApiKeyDto } from "../types/apiKey";
import { toast } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";

// 타입 정의
type ApiKeyMap = Partial<Record<Exchange, ApiKeyDto>>;

const AuthKeyManager = () => {
  const exchanges = Object.values(Exchange);
  const [apiKeys, setApiKeys] = useState<ApiKeyMap>({});

  useEffect(() => {
    api.get<ApiKeyDto[]>("/keys").then((res) => {
      const data = res.data.reduce((map, item) => {
        map[item.exchange] = item;
        return map;
      }, {} as ApiKeyMap);
      setApiKeys(data);
    });
  }, []);

  const handleChange = (
    exchange: Exchange,
    field: keyof ApiKeyDto,
    value: string
  ) => {
    setApiKeys((prev) => ({
      ...prev,
      [exchange]: {
        ...prev[exchange],
        exchange,
        [field]: value.trim(),
      },
    }));
  };

  const handleSave = async (exchange: Exchange) => {
    const data = apiKeys[exchange];
    if (!data?.apiKey || !data?.secretKey) {
      toast.error("❗ 필수값이 비었습니다.");
      return;
    }

    try {
      const res = await api.post<{ validated: boolean }>("/keys", data);
      const isValid = res.data.validated;

      setApiKeys((prev) => ({
        ...prev,
        [exchange]: {
          ...prev[exchange],
          validated: isValid,
        },
      }));

      toast[isValid ? "success" : "warning"](
        isValid ? "✅ 인증 성공" : "❌ 인증 실패 - 키가 저장되었습니다."
      );
    } catch (e) {
      toast.error("🚨 오류가 발생했습니다.");
    }
  };

  const handleDelete = async (exchange: Exchange) => {
    await api.delete(`/keys/${exchange}`);
    setApiKeys((prev) => {
      const copy = { ...prev };
      delete copy[exchange];
      return copy;
    });
    toast.info("🗑️ 삭제되었습니다.");
  };

  return (
    <div className="p-6 max-w-3xl mx-auto text-gray-200">
      <h1 className="text-2xl font-bold mb-8">🔐 인증키 관리</h1>

      {exchanges.map((exchange) => {
        const data = apiKeys[exchange] || {};
        const validated = data.validated ?? false;
        const isSaved = !!(data.apiKey && data.secretKey);

        return (
          <div
            key={exchange}
            className="mb-6 bg-gray-800 rounded-xl p-4 shadow"
          >
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-xl font-semibold">
                {EXCHANGE_LABELS[exchange]} API 키
              </h2>
              {isSaved && (
                <span
                  className={`text-sm px-2 py-1 rounded-full ${
                    validated
                      ? "bg-green-600 text-white"
                      : "bg-red-600 text-white"
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
                value={data.apiKey || ""}
                onChange={(e) =>
                  handleChange(exchange, "apiKey", e.target.value)
                }
              />
              <input
                className="bg-gray-900 border border-gray-600 p-2 rounded w-full"
                placeholder="Secret Key"
                type="password"
                value={data.secretKey || ""}
                onChange={(e) =>
                  handleChange(exchange, "secretKey", e.target.value)
                }
              />
              {exchange === Exchange.BITGET && (
                <input
                  className="bg-gray-900 border border-gray-600 p-2 rounded w-full"
                  placeholder="Passphrase"
                  value={data.passphrase || ""}
                  onChange={(e) =>
                    handleChange(exchange, "passphrase", e.target.value)
                  }
                />
              )}
              <div className="flex gap-3">
                <button
                  className="bg-green-600 hover:bg-green-700 px-4 py-2 rounded"
                  onClick={() => handleSave(exchange)}
                >
                  저장
                </button>
                <button
                  className="bg-red-600 hover:bg-red-700 px-4 py-2 rounded"
                  onClick={() => handleDelete(exchange)}
                >
                  삭제
                </button>
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
};

export default AuthKeyManager;
