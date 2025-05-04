import React, { useState } from "react";
import BitcoinChart from "@/components/BitcoinChart";
import { Exchange, EXCHANGE_LABELS } from "@/constants/Exchange";
import { Timeframe, TIMEFRAME_LABELS } from "@/constants/TimeFrame";
import { v4 as uuidv4 } from "uuid";
import { toast, ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";

interface IndicatorCondition {
  type: "RSI" | "StochRSI";
  value?: number;
  k?: number;
  d?: number;
  operator: "이상" | "이하";
  timeframe: Timeframe;
  direction: "LONG" | "SHORT";
}

interface Position {
  id: string;
  title: string;
  exchange: Exchange;
  conditions: IndicatorCondition[];
  enabled: boolean;
}

const PositionManager = () => {
  const [showChart, setShowChart] = useState(true);
  const [selectedExchange, setSelectedExchange] = useState<Exchange | "">("");
  const [selectedIndicator, setSelectedIndicator] = useState<string>("");
  const [positions, setPositions] = useState<Position[]>([]);
  const [positionTitle, setPositionTitle] = useState("");
  const [currentCondition, setCurrentCondition] = useState<Partial<IndicatorCondition>>({});
  const [showConditionBox, setShowConditionBox] = useState(false);
  const [activePositionId, setActivePositionId] = useState<string | null>(null);
  const [selectedTimeframe, setSelectedTimeframe] = useState<Timeframe>(Timeframe.ONE_MINUTE);
  const [selectedDirection, setSelectedDirection] = useState<"LONG" | "SHORT">("LONG");
  const [selectedPositionIds, setSelectedPositionIds] = useState<Set<string>>(new Set());


  const handleAddPosition = () => {
    if (!selectedExchange) {
      toast.error("거래소를 선택해주세요.");
      return;
    }
    if (!positionTitle.trim()) {
      toast.error("포지션 제목을 입력해주세요.");
      return;
    }
    const newPosition: Position = {
      id: uuidv4(),
      title: positionTitle,
      exchange: selectedExchange as Exchange,
      conditions: [],
      enabled: true,
    };
    setPositions([...positions, newPosition]);
    setPositionTitle("");
  };

  const handleAddCondition = () => {
    if (!selectedIndicator || !activePositionId) {
      toast.error("지표를 선택해주세요.");
      return;
    }

    const targetPosition = positions.find((p) => p.id === activePositionId);
    if (!targetPosition) return;

    // 방향 일관성 검사
    const existingDirection = targetPosition.conditions.find((c) => c.direction === "LONG")
      ? "LONG"
      : targetPosition.conditions.find((c) => c.direction === "SHORT")
      ? "SHORT"
      : null;
    if (existingDirection && selectedDirection !== existingDirection) {
      toast.error(`${existingDirection} 조건만 추가할 수 있습니다.`);
      return;
    }

    // 지표 중복 검사
    const isDuplicate = targetPosition.conditions.some(
      (c) => c.type === selectedIndicator && c.timeframe === selectedTimeframe
    );
    if (isDuplicate) {
      toast.error(`${TIMEFRAME_LABELS[selectedTimeframe]} 분봉의 ${selectedIndicator} 조건은 이미 존재합니다.`);
      return;
    }

    // 값 유효성 검사
    if (selectedIndicator === "RSI") {
      if (
        currentCondition.value === undefined ||
        currentCondition.value === null ||
        isNaN(currentCondition.value)
      ) {
        toast.error("RSI 값이 입력되지 않았습니다.");
        return;
      }
    }

    if (selectedIndicator === "StochRSI") {
      if (
        currentCondition.k === undefined ||
        currentCondition.d === undefined ||
        isNaN(currentCondition.k) ||
        isNaN(currentCondition.d)
      ) {
        toast.error("StochRSI의 K 또는 D 값이 입력되지 않았습니다.");
        return;
      }
    }

    const conditionWithTimeframe = {
      ...currentCondition,
      timeframe: selectedTimeframe,
      direction: selectedDirection,
    } as IndicatorCondition;

    setPositions((prev) =>
      prev.map((p) =>
        p.id === activePositionId
          ? {
              ...p,
              conditions: [...p.conditions, conditionWithTimeframe],
            }
          : p
      )
    );
    setCurrentCondition({});
    setSelectedIndicator("");
    setShowConditionBox(false);
  };

  const toggleEnabled = (id: string) => {
    setPositions((prev) =>
      prev.map((p) => (p.id === id ? { ...p, enabled: !p.enabled } : p))
    );
  };

  const deleteCondition = (positionId: string, conditionIndex: number) => {
    setPositions((prev) =>
      prev.map((p) =>
        p.id === positionId
          ? { ...p, conditions: p.conditions.filter((_, idx) => idx !== conditionIndex) }
          : p
      )
    );
  };

  const toggleSelectPosition = (id: string) => {
    setSelectedPositionIds((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(id)) {
        newSet.delete(id);
      } else {
        newSet.add(id);
      }
      return newSet;
    });
  }; 
  
  const deleteSelectedPositions = () => {
    setPositions((prev) => prev.filter((p) => !selectedPositionIds.has(p.id)));
    setSelectedPositionIds(new Set());
  };  
  const savePositions = () => {
    toast.success("변경사항이 저장되었습니다.");
    // 나중에 API 호출 등 추가 가능
  };
  return (
    <div className="p-4">
      <div className="flex items-center mb-2">
        <span
          className="text-white text-lg cursor-pointer hover:text-yellow-400 transition-colors"
          onClick={() => setShowChart((prev) => !prev)}
        >
          ?? 포지션 차트 ({showChart ? "클릭해서 숨기기" : "클릭해서 보이기"})
        </span>
      </div>

      {showChart && <BitcoinChart />}

      <div className="flex items-center justify-between mb-4">
        <select
          value={selectedExchange}
          onChange={(e) => setSelectedExchange(e.target.value as Exchange)}
          className="px-4 py-2 rounded bg-gray-800 text-white border border-gray-600"
        >
          <option value="">거래소 선택</option>
          {Object.values(Exchange).map((ex) => (
            <option key={ex} value={ex}>
              {EXCHANGE_LABELS[ex]}
            </option>
          ))}
        </select>

        <input
          type="text"
          placeholder="포지션 제목을 입력하세요"
          value={positionTitle}
          onChange={(e) => setPositionTitle(e.target.value)}
          className="px-4 py-2 rounded bg-gray-800 text-white border border-gray-600 w-1/2 mx-4"
        />
        <div className="flex justify-end gap-2 mb-4">
          <button
            onClick={deleteSelectedPositions}
            className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
          >
            선택 삭제
          </button>
          <button
            onClick={handleAddPosition}
            className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700"
          >
            포지션 추가
          </button>
          <button
            onClick={savePositions}
            className="px-4 py-2 bg-yellow-500 text-white rounded hover:bg-yellow-600"
          >
            저장
          </button>
        </div>
       
      </div>

      {showConditionBox && (
        <div className="mt-6 border-2 border-gray-700 p-4 bg-gray-800 rounded-md relative">
          <div className="absolute top-4 right-4">
            <button
              onClick={handleAddCondition}
              className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
            >
              조건 저장
            </button>
          </div>
          <div className="text-lg text-white mb-4 flex items-center gap-4">
            <span>지표 설정</span>
            <div className="flex gap-2">
              <button
                className={`px-2 py-1 rounded ${selectedDirection === "LONG" ? "bg-green-600" : "bg-gray-600"}`}
                onClick={() => setSelectedDirection("LONG")}
              >
                롱
              </button>
              <button
                className={`px-2 py-1 rounded ${selectedDirection === "SHORT" ? "bg-red-600" : "bg-gray-600"}`}
                onClick={() => setSelectedDirection("SHORT")}
              >
                숏
              </button>
            </div>
          </div>

          <div className="flex flex-wrap gap-4 text-white mb-4">
            {Object.values(Timeframe).map((tf) => (
              <label key={tf} className="flex items-center gap-1">
                <input
                  type="radio"
                  name="timeframe"
                  value={tf}
                  checked={selectedTimeframe === tf}
                  onChange={() => setSelectedTimeframe(tf)}
                />
                {TIMEFRAME_LABELS[tf]}
              </label>
            ))}
          </div>

          <select
            value={selectedIndicator}
            onChange={(e) => setSelectedIndicator(e.target.value)}
            className="mb-4 px-2 py-1 rounded bg-gray-700 text-gray-300"
          >
            <option value="">-- 지표 선택 --</option>
            <option value="RSI">RSI</option>
            <option value="StochRSI">StochRSI</option>
          </select>

          {selectedIndicator === "RSI" && (
            <div className="flex items-center gap-3">
              <input
                type="number"
                placeholder="값"
                onChange={(e) =>
                  setCurrentCondition({ type: "RSI", value: Number(e.target.value), operator: selectedDirection === "LONG" ? "이하" : "이상" })
                }
                className="px-2 py-1 rounded bg-gray-700 text-gray-300"
              />
              <span className="text-white text-sm">
                {selectedDirection === "LONG" ? "이하" : "이상"}
              </span>
            </div>
          )}

          {selectedIndicator === "StochRSI" && (
            <div className="flex items-center gap-3 flex-wrap">
              <label className="text-white">K</label>
              <input
                type="number"
                placeholder="값"
                onChange={(e) =>
                  setCurrentCondition((prev) => ({ ...prev, type: "StochRSI", k: Number(e.target.value), operator: selectedDirection === "LONG" ? "이하" : "이상" }))
                }
                className="px-2 py-1 rounded bg-gray-700 text-gray-300"
              />
              <span className="text-white text-sm">
                {selectedDirection === "LONG" ? "이하" : "이상"}
              </span>
              <label className="text-white ml-4">D</label>
              <input
                type="number"
                placeholder="값"
                onChange={(e) =>
                  setCurrentCondition((prev) => ({ ...prev, d: Number(e.target.value) }))
                }
                className="px-2 py-1 rounded bg-gray-700 text-gray-300"
              />
              <span className="text-white text-sm">
                {selectedDirection === "LONG" ? "이하" : "이상"}
              </span>
            </div>
          )}
        </div>
      )}

      <table className="w-full text-white mt-8 border border-gray-700">
      <thead className="bg-gray-700">
        <tr>
          <th className="border border-gray-600 p-2">선택</th> {/* ✅ 추가 */}
          <th className="border border-gray-600 p-2">포지션 제목</th>
          <th className="border border-gray-600 p-2">거래소</th>
          <th className="border border-gray-600 p-2">조건</th>
          <th className="border border-gray-600 p-2">내용</th>
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
                  [{cond.direction}] {cond.timeframe} - {cond.type} {cond.type === "RSI"
                    ? ` ${cond.value} ${cond.operator}`
                    : `K ${cond.k} ${cond.operator} D ${cond.d} ${cond.operator}`}
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
      <td className="border border-gray-600 p-2" colSpan={2}>조건 없음</td>
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

      <ToastContainer position="top-center" autoClose={2000} />
    </div>
  );
};

export default PositionManager;