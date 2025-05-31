// 📁 src/features/executed-report/components/Dashboard.tsx

import React, { useEffect, useState } from "react";
import {
  getExecutedReports,
  ExecutedReportResponseDto,
} from "@/features/executed-report/services/ExecutedReportService";

const Dashboard = () => {
  const [data, setData] = useState<ExecutedReportResponseDto[]>([]);

  useEffect(() => {
    getExecutedReports()
      .then(setData)
      .catch((err) => {
        console.error("대시보드 데이터 로딩 실패:", err);
      });
  }, []);

  const total = data.length;
  const totalProfit = data.reduce(
    (acc, item) => acc + (item.profitRate ?? 0),
    0
  );
  const avgProfit = total > 0 ? totalProfit / total : 0;
  const successCount = data.filter((item) => item.profitRate > 0).length;
  const successRate = total > 0 ? (successCount / total) * 100 : 0;

  return (
    <div className="grid grid-cols-4 gap-4 mb-6">
      <div className="bg-gray-900 text-white p-4 shadow-md rounded-2xl border border-gray-700">
        누적 수익률:{" "}
        <span className={totalProfit >= 0 ? "text-green-400" : "text-red-400"}>
          {totalProfit.toFixed(2)}%
        </span>
      </div>
      <div className="bg-gray-900 text-white p-4 shadow-md rounded-2xl border border-gray-700">
        평균 수익률:{" "}
        <span className={avgProfit >= 0 ? "text-green-400" : "text-red-400"}>
          {avgProfit.toFixed(2)}%
        </span>
      </div>
      <div className="bg-gray-900 text-white p-4 shadow-md rounded-2xl border border-gray-700">
        총 체결 수: {total}건
      </div>
      <div className="bg-gray-900 text-white p-4 shadow-md rounded-2xl border border-gray-700">
        성공률: <span className="text-blue-400">{successRate.toFixed(1)}%</span>
      </div>
    </div>
  );
};

export default Dashboard;
