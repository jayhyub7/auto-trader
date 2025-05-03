import React, { useEffect, useState } from "react";
import { Exchange, EXCHANGE_LABELS } from "../constants/Exchange";
import api from "../lib/axios";

type BalanceItem = {
  asset: string;
  available: number;
  locked: number;
  total: number;
};

type ExchangeBalance = {
  exchange: string;
  balances: BalanceItem[];
  totalUsdValue: number;
  validated: boolean;
};

const CurrentBalance = () => {
  const exchanges = Object.values(Exchange);
  const [balances, setBalances] = useState<Record<string, ExchangeBalance>>({});

  const fetchBalances = async () => {
    try {
      const res = await api.get<ExchangeBalance[]>("/current-balance");
      const mapped = res.data.reduce((acc, cur) => {
        acc[cur.exchange] = cur;
        return acc;
      }, {} as Record<string, ExchangeBalance>);
      setBalances(mapped);
    } catch (e) {
      console.error("잔고 요청 실패:", e);
    }
  };

  useEffect(() => {
    fetchBalances(); // 초기 로딩
    const interval = setInterval(fetchBalances, 10000); // 10초마다 갱신
    return () => clearInterval(interval); // 언마운트 시 정리
  }, []);

  return (
    <div className="p-6 text-white">
      <h1 className="text-2xl font-bold mb-6">💰 현재 잔고</h1>

      <div className="space-y-6">
        {exchanges.map((exchange) => {
          const data = balances[exchange] || {
            exchange,
            balances: [],
            totalUsdValue: 0,
            validated: false,
          };

          return (
            <div
              key={exchange}
              className="bg-gray-800 p-4 rounded-xl shadow-md"
            >
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-xl font-semibold">
                  {EXCHANGE_LABELS[exchange] || exchange}
                </h2>
                <span
                  className={`text-sm px-2 py-1 rounded-full ${
                    data.validated ? "bg-green-600" : "bg-red-600"
                  }`}
                >
                  {data.validated ? "인증됨" : "인증 안됨"}
                </span>
              </div>

              {data.balances.length > 0 ? (
                <table className="w-full text-sm text-left">
                  <thead>
                    <tr className="text-gray-400 border-b border-gray-600">
                      <th className="py-1">자산</th>
                      <th className="py-1 text-right">사용 가능</th>
                      <th className="py-1 text-right">잠김</th>
                      <th className="py-1 text-right">총계</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.balances.map((b) => (
                      <tr key={b.asset} className="border-b border-gray-700">
                        <td className="py-1">{b.asset}</td>
                        <td className="py-1 text-right">{b.available}</td>
                        <td className="py-1 text-right">{b.locked}</td>
                        <td className="py-1 text-right font-semibold">
                          {b.total}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              ) : (
                <p className="text-gray-500 text-sm">잔고 없음</p>
              )}

              <div className="mt-4 text-right text-lg font-bold text-yellow-400">
                총 USD 자산: ${data.totalUsdValue.toLocaleString()}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default CurrentBalance;
