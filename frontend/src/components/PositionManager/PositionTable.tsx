import React from "react";
import { Exchange, EXCHANGE_LABELS } from "@/constants/Exchange";
import { Timeframe, TIMEFRAME_LABELS } from "@/constants/TimeFrame";
import { Position } from "@/service/positionManager";


interface Props {
  positions: Position[];
  selectedPositionIds: Set<string>;
  toggleSelectPosition: (id: string) => void;
  toggleEnabled: (id: string) => void;
  deleteCondition: (positionId: string, conditionIndex?: number) => void;
  setShowConditionBox: (show: boolean) => void;
  setActivePositionId: (id: string | null) => void;
}

const PositionTable: React.FC<Props> = ({
  positions,
  selectedPositionIds,
  toggleSelectPosition,
  toggleEnabled,
  deleteCondition,
  setShowConditionBox,
  setActivePositionId,
}) => {
  return (
    <table className="w-full text-white mt-8 border border-gray-700">
      <thead className="bg-gray-700">
        <tr>
          <th className="border border-gray-600 p-2">선택</th>
          <th className="border border-gray-600 p-2">포지션 제목</th>
          <th className="border border-gray-600 p-2">거래소</th>
          <th className="border border-gray-600 p-2">조건</th>           
          <th className="border border-gray-600 p-2">내용</th>
          <th className="border border-gray-600 p-2 text-center">조건유형</th>
          <th className="border border-gray-600 p-2 text-center">조건추가</th>
          <th className="border border-gray-600 p-2 text-center">사용여부</th>
          <th className="border border-gray-600 p-2 text-center">조건삭제</th>
        </tr>
      </thead>
      <tbody>
        {positions.map((pos) =>
          pos.conditions.length > 0 ? (
            pos.conditions.map((cond, idx) => (
              <tr key={`${pos.id}-${idx}`} className="border-t border-gray-700">
                {idx === 0 && (
                  <>
                    <td rowSpan={pos.conditions.length} className="border border-gray-600 p-2 text-center">
                      <input
                        type="checkbox"
                        checked={selectedPositionIds.has(pos.id)}
                        onChange={() => toggleSelectPosition(pos.id)}
                      />
                    </td>
                    <td rowSpan={pos.conditions.length} className="border border-gray-600 p-2">{pos.title}</td>
                    <td rowSpan={pos.conditions.length} className="border border-gray-600 p-2">{EXCHANGE_LABELS[pos.exchange]}</td>
                  </>
                )}
                <td className="border border-gray-600 p-2">조건 {idx + 1}</td>
                <td className="border border-gray-600 p-2">
                  [{cond.direction}] {cond.timeframe} -{
                    cond.type === "RSI"
                      ? ` ${cond.value} ${cond.operator}`
                      : cond.type === "StochRSI"
                      ? `K ${cond.k} ${cond.operator} D ${cond.d} ${cond.operator}`
                      : cond.type === "VWBB"
                      ? `${cond.operator}`
                      : ""
                  }
                </td>
                <td className="border border-gray-600 p-2 text-center">
                  {cond.conditionPhase === "ENTRY" ? "진입" : "종료"}
                </td>
                {idx === 0 && (
                  <>
                    <td rowSpan={pos.conditions.length} className="border border-gray-600 p-2 text-center">
                      <button
                        onClick={() => {
                          setShowConditionBox(true);
                          setActivePositionId(pos.id);
                        }}
                        className="px-2 py-1 text-xs bg-blue-600 rounded"
                      >
                        조건추가
                      </button>
                    </td>
                    <td rowSpan={pos.conditions.length} className="border border-gray-600 p-2 text-center">
                      <input
                        type="checkbox"
                        checked={pos.enabled}
                        onChange={() => toggleEnabled(pos.id)}
                      />
                    </td>
                  </>
                )}
                <td className="border border-gray-600 p-2 text-center">
                  <button
                    onClick={() => deleteCondition(pos.id, idx)}
                    className="px-2 py-1 text-xs bg-red-600 rounded"
                  >
                    삭제
                  </button>
                </td>
              </tr>
            ))
          ) : (
            <tr key={pos.id} className="border-t border-gray-700">
              <td className="border border-gray-600 p-2 text-center">
                <input
                  type="checkbox"
                  checked={selectedPositionIds.has(pos.id)}
                  onChange={() => toggleSelectPosition(pos.id)}
                />
              </td>
              <td className="border border-gray-600 p-2">{pos.title}</td>
              <td className="border border-gray-600 p-2">{EXCHANGE_LABELS[pos.exchange]}</td>
              <td className="border border-gray-600 p-2" colSpan={3}>조건 없음</td>
              <td className="border border-gray-600 p-2 text-center">
                <button
                  onClick={() => {
                    setShowConditionBox(true);
                    setActivePositionId(pos.id);
                  }}
                  className="px-2 py-1 text-xs bg-blue-600 rounded"
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
              <td className="border border-gray-600 p-2 text-center">
                <button
                  onClick={() => deleteCondition(pos.id)}
                  className="px-2 py-1 text-xs bg-red-600 rounded"
                >
                  삭제
                </button>
              </td>
            </tr>
          )
        )}
      </tbody>
    </table>
  );
};

export default PositionTable;