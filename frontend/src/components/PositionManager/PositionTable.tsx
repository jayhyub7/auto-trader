// 📄 PositionTable.tsx

import React from "react";
import { Exchange, EXCHANGE_LABELS } from "@/constants/Exchange";
import { Position } from "@/service/positionManagerService";
import { Directions, ConditionPhases } from "@/service/positionManagerService";

interface Props {
  positions: Position[];
  selectedPositionIds: Set<string>;
  toggleSelectPosition: (id: string) => void;
  toggleEnabled: (id: string) => void;
  deleteCondition: (positionId: string, conditionIndex?: number) => void;
  toggleConditionEnabled: (positionId: string, conditionIndex: number) => void;
  setShowConditionBox: (show: boolean) => void;
  setActivePositionId: (id: string | null) => void;
  setPositions: React.Dispatch<React.SetStateAction<Position[]>>;
}

const PositionTable: React.FC<Props> = ({
  positions,
  selectedPositionIds,
  toggleSelectPosition,
  toggleEnabled,
  deleteCondition,
  toggleConditionEnabled,
  setShowConditionBox,
  setActivePositionId,
  setPositions,
}) => {
  return (
    <table className="w-full text-sm text-white border border-gray-600 mt-4">
      <thead className="bg-gray-800 text-center">
        <tr>
          <th className="border border-gray-600 p-2">선택</th>
          <th className="border border-gray-600 p-2">포지션 제목</th>
          <th className="border border-gray-600 p-2">📊 방향</th>
          <th className="border border-gray-600 p-2">거래소</th>
          <th className="border border-gray-600 p-2">조건</th>
          <th className="border border-gray-600 p-2">내용</th>
          <th className="border border-gray-600 p-2">조건유형</th>
          <th className="border border-gray-600 p-2">조건 사용여부</th>
          <th className="border border-gray-600 p-2">조건추가</th>
          <th className="border border-gray-600 p-2">포지션 사용여부</th>
          <th className="border border-gray-600 p-2">조건삭제</th>
        </tr>
      </thead>
      <tbody>
        {positions.map((pos) =>
          pos.conditions.length === 0 ? (
            <tr key={pos.id}>
              <td className="border border-gray-600 p-2 text-center">
                <input
                  type="checkbox"
                  checked={selectedPositionIds.has(pos.id)}
                  onChange={() => toggleSelectPosition(pos.id)}
                />
              </td>
              <td className="border border-gray-600 p-2">
                <input
                  type="text"
                  value={pos.title}
                  onChange={(e) => {
                    const newPositions = [...positions];
                    newPositions.find((p) => p.id === pos.id)!.title = e.target.value;
                    setPositions(newPositions);
                  }}
                  className="w-full bg-transparent text-white"
                />
              </td>
              <td className="border border-gray-600 p-2 text-center text-sm">
                <span className={pos.direction === Directions.LONG ? "text-green-400" : "text-red-400"}>
                  {pos.direction === Directions.LONG ? "📈 롱" : "📉 숏"}
                </span>
              </td>
              <td className="border border-gray-600 p-2 text-center">
                {EXCHANGE_LABELS[pos.exchange]}
              </td>
              <td className="border border-gray-600 p-2 text-center">-</td>
              <td className="border border-gray-600 p-2 text-center text-gray-400">조건 없음</td>
              <td className="border border-gray-600 p-2 text-center">-</td>
              <td className="border border-gray-600 p-2 text-center">-</td>
              <td className="border border-gray-600 p-2 text-center">
                <button
                  className="text-xs bg-blue-600 px-2 py-1 rounded"
                  onClick={() => {
                    setActivePositionId(pos.id);
                    setShowConditionBox(true);
                  }}
                >
                  조건추가
                </button>
              </td>
              <td className="border border-gray-600 p-2 text-center">
                <input
                  type="checkbox"
                  checked={pos.enabled}
                  onChange={() => toggleEnabled(pos.id)}
                />
              </td>
              <td className="border border-gray-600 p-2 text-center">-</td>
            </tr>
          ) : (
            pos.conditions.map((cond, idx) => (
              <tr key={idx}>
                {idx === 0 && (
                  <>
                    <td rowSpan={pos.conditions.length} className="border border-gray-600 p-2 text-center">
                      <input
                        type="checkbox"
                        checked={selectedPositionIds.has(pos.id)}
                        onChange={() => toggleSelectPosition(pos.id)}
                      />
                    </td>
                    <td rowSpan={pos.conditions.length} className="border border-gray-600 p-2">
                      <input
                        type="text"
                        value={pos.title}
                        onChange={(e) => {
                          const newPositions = [...positions];
                          newPositions.find((p) => p.id === pos.id)!.title = e.target.value;
                          setPositions(newPositions);
                        }}
                        className="w-full bg-transparent text-white"
                      />
                    </td>
                    <td rowSpan={pos.conditions.length} className="border border-gray-600 p-2 text-center text-sm">
                      <span className={pos.direction === Directions.LONG ? "text-green-400" : "text-red-400"}>
                       {pos.direction === Directions.LONG ? "📈 롱" : "📉 숏"}
                      </span>
                    </td>
                    <td rowSpan={pos.conditions.length} className="border border-gray-600 p-2 text-center">
                      {EXCHANGE_LABELS[pos.exchange]}
                    </td>
                  </>
                )}
                <td className="border border-gray-600 p-2 text-center">조건 {idx + 1}</td>
                <td className="border border-gray-600 p-2">
                  [{cond.type}] {cond.timeframe}{" "}
                  {cond.type === "RSI" && cond.value !== undefined
                    ? `${cond.operator} ${cond.value}`
                    : cond.type === "STOCH_RSI"
                    ? `${cond.operator} K ${cond.k} D ${cond.d}`
                    : cond.operator}
                </td>
                <td className="border border-gray-600 p-2 text-center">
                  {cond.conditionPhase === ConditionPhases.ENTRY ? "진입" : "종료"}
                </td>
                <td className="border border-gray-600 p-2 text-center">
                  <input
                    type="checkbox"
                    checked={cond.enabled}
                    onChange={() => toggleConditionEnabled(pos.id, idx)}
                  />
                </td>
                {idx === 0 && (
                  <td rowSpan={pos.conditions.length} className="border border-gray-600 p-2 text-center">
                    <button
                      className="text-xs bg-blue-600 px-2 py-1 rounded"
                      onClick={() => {
                        setActivePositionId(pos.id);
                        setShowConditionBox(true);
                      }}
                    >
                      조건추가
                    </button>
                  </td>
                )}
                {idx === 0 && (
                  <td rowSpan={pos.conditions.length} className="border border-gray-600 p-2 text-center">
                    <input
                      type="checkbox"
                      checked={pos.enabled}
                      onChange={() => toggleEnabled(pos.id)}
                    />
                  </td>
                )}
                <td className="border border-gray-600 p-2 text-center">
                  <button
                    onClick={() => deleteCondition(pos.id, idx)}
                    className="text-xs bg-red-600 px-2 py-1 rounded"
                  >
                    삭제
                  </button>
                </td>
              </tr>
            ))
          )
        )}
      </tbody>
    </table>
  );
};

export default PositionTable;
