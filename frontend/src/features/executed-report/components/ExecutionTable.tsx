import React, { useEffect, useState } from "react";
import {
  getExecutedReports,
  ExecutedReportResponseDto,
} from "@/features/executed-report/services/ExecutedReportService";

const ExecutionTable = () => {
  const [data, setData] = useState<ExecutedReportResponseDto[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchData = () => {
    setLoading(true);
    getExecutedReports()
      .then(setData)
      .catch((err) => {
        console.error("실행 리포트 로딩 실패:", err);
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchData();
  }, []);

  return (
    <div>
      <div className="flex justify-end mb-2">
        <button
          onClick={fetchData}
          className="bg-blue-600 text-white px-4 py-1 rounded hover:bg-blue-500 text-sm"
        >
          조회
        </button>
      </div>

      {loading ? (
        <div className="text-white">불러오는 중...</div>
      ) : (
        <table className="min-w-full text-sm border text-white bg-gray-800">
          <thead className="bg-gray-700">
            <tr>
              <th className="border px-2 py-1">#</th>
              <th className="border px-2 py-1">체결일시</th>
              <th className="border px-2 py-1">포지션명</th>
              <th className="border px-2 py-1">방향</th>
              <th className="border px-2 py-1">체결가</th>
              <th className="border px-2 py-1">수익률</th>
              <th className="border px-2 py-1">조건</th>
              <th className="border px-2 py-1">로그</th>
            </tr>
          </thead>
          <tbody>
            {data.map((item, i) => (
              <tr key={i}>
                <td className="border px-2 py-1">{i + 1}</td>
                <td className="border px-2 py-1">{item.executedAt}</td>
                <td className="border px-2 py-1">{item.positionName}</td>
                <td className="border px-2 py-1">{item.direction}</td>
                <td className="border px-2 py-1">
                  {item.executedPrice.toLocaleString()}
                </td>
                <td
                  className={`border px-2 py-1 ${
                    item.profitRate >= 0 ? "text-green-400" : "text-red-400"
                  }`}
                >
                  {item.profitRate.toFixed(2)}%
                </td>
                <td className="border px-2 py-1">
                  {item.conditions
                    .map((c) => `${c.indicator} ${c.operator} ${c.value}`)
                    .join(", ")}
                </td>
                <td className="border px-2 py-1 whitespace-pre-line">
                  {item.executionLog}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
};

export default ExecutionTable;
