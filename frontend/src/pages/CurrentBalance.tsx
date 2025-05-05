import React, { useEffect, useState } from "react";
import { Exchange, EXCHANGE_LABELS } from "../constants/Exchange";
import { fetchBalances, ExchangeBalance } from "@/service/balanceService";

type BalanceItem = {
  asset: string;
  available: number;
  locked: number;
  total: number;
  usdValue: number;
  krwValue: number;
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

  useEffect(() => {
    const load = async () => {
      try {
        const result = await fetchBalances();
        setBalances(result);
      } catch (e) {
        console.error("잔고 요청 실패:", e);
      }
    };
  
    load(); // ✅ 초기 1회 호출만 수행
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

          // ✅ 0.5 USD 이상만 표시
          const usdtBalances = data.balances.filter(
            (b) => b.asset === "USDT" && b.usdValue >= 0.5
          );

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

              {usdtBalances.length > 0 ? (
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
                    {usdtBalances.map((b) => (
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
                <p className="text-gray-500 text-sm">0.5달러 이상 USDT 잔고 없음</p>
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
