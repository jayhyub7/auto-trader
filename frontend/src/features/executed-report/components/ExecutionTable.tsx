import React from 'react';

const ExecutionTable = () => {
  return (
    <table className="min-w-full bg-white text-sm border">
      <thead>
        <tr className="bg-gray-100">
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
        <tr>
          <td className="border px-2 py-1">2025-05-30 09:12:23</td>
          <td className="border px-2 py-1">스탑헌팅롱</td>
          <td className="border px-2 py-1">LONG</td>
          <td className="border px-2 py-1">68,320</td>
          <td className="border px-2 py-1 text-green-600">+0.71%</td>
          <td className="border px-2 py-1">RSI > 70, VWBB < 하단</td>
          <td className="border px-2 py-1 whitespace-pre">조건 평가 통과
RSI 72 > 70
VWBB 26.1 < 27.0</td>
        </tr>
      </tbody>
    </table>
  );
};

export default ExecutionTable;
