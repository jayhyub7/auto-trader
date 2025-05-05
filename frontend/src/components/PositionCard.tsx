import React, { useState } from "react";
import AmountSelector from "@/components/AmountSelector";
import {
  Position,
  PositionOpenPayload,
  PositionOpenStatus,
} from "@/service/positionOpenService"; // ✅ 타입 통합 import

interface PositionCardProps {
  position: Position;
  balance: number;
  status: PositionOpenStatus;
  onUpdateStatus: (
    id: string,
    status: PositionOpenStatus,
    payload: Omit<PositionOpenPayload, "status">
  ) => void;
}

const PositionCard: React.FC<PositionCardProps> = ({
  position,
  balance,
  status,
  onUpdateStatus,
}) => {
  const [takeProfit, setTakeProfit] = useState(0);
  const [stopLoss, setStopLoss] = useState(0);
  const [amountType, setAmountType] = useState<"fixed" | "percent">("fixed");
  const [amount, setAmount] = useState(0);

  return (
    <div className="border border-gray-600 bg-gray-800 p-4 rounded-lg text-white">
      <div className="font-semibold text-lg mb-2">
        {position.title} ({position.exchange})
      </div>

      <div className="text-sm mb-2">
        조건:
        <ul className="list-disc ml-5">
          {position.conditions.map((cond, idx) => (
            <li key={idx}>
              [{cond.direction}] {cond.timeframe} - {cond.operator} (
              {cond.conditionPhase === "ENTRY" ? "진입" : "종료"})
            </li>
          ))}
        </ul>
      </div>

      <div className="grid grid-cols-2 gap-4 items-start mb-4">
        <div>
          <label className="block mb-1 text-sm">Take Profit (%)</label>
          <input
            type="number"
            value={takeProfit}
            onChange={(e) => setTakeProfit(Number(e.target.value))}
            className="w-full px-2 py-1 rounded bg-gray-700 border border-gray-600"
            placeholder="예: 15"
          />
        </div>

        <div>
          <label className="block mb-1 text-sm text-red-400">
            Stop Loss (%) <span className="text-yellow-400">*</span>
          </label>
          <input
            type="number"
            value={stopLoss}
            onChange={(e) => setStopLoss(Number(e.target.value))}
            className="w-full px-2 py-1 rounded bg-gray-700 border border-gray-600"
            placeholder="예: 5"
            required
          />
        </div>

        <div>
          <label className="block mb-1 text-sm">금액 유형</label>
          <select
            value={amountType}
            onChange={(e) => setAmountType(e.target.value as "fixed" | "percent")}
            className="w-full px-2 py-1 rounded bg-gray-700 border border-gray-600"
          >
            <option value="fixed">고정 금액</option>
            <option value="percent">비율</option>
          </select>
        </div>

        <div>
          {amountType === "fixed" ? (
            <div>
              <label className="block mb-1 text-sm">금액</label>
              <input
                type="number"
                value={amount}
                onChange={(e) => setAmount(Number(e.target.value))}
                className="w-full px-2 py-1 rounded bg-gray-700 border border-gray-600"
                placeholder="예: 100"
              />
            </div>
          ) : (
            <AmountSelector
              maxAmount={balance}
              onChange={(val) => setAmount(val)}
            />
          )}
        </div>

        <div className="col-span-2 text-sm text-gray-400">
          잔고: {balance.toLocaleString()} USDT
        </div>
      </div>

      <div className="flex gap-3 items-center">
        <span className="text-sm text-yellow-400">상태: {status}</span>

        <button
          disabled={status !== "idle"}
          onClick={() =>
            onUpdateStatus(position.id, "running", {
              id: undefined,
              positionId: position.id,
              amount,
              amountType,
              stopLoss,
              takeProfit,
            })
          }
          className="px-3 py-1 bg-green-600 hover:bg-green-700 disabled:opacity-50 rounded text-sm"
        >
          실행
        </button>

        <button
          disabled={status !== "idle"}
          onClick={() =>
            onUpdateStatus(position.id, "simulating", {
              id: undefined,
              positionId: position.id,
              amount,
              amountType,
              stopLoss,
              takeProfit,
            })
          }
          className="px-3 py-1 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 rounded text-sm"
        >
          시뮬레이션
        </button>

        <button
          disabled={!(status === "running" || status === "simulating")}
          onClick={() =>
            onUpdateStatus(position.id, "cancelled", {
              id: undefined,
              positionId: position.id,
              amount,
              amountType,
              stopLoss,
              takeProfit,
            })
          }
          className="px-3 py-1 bg-red-600 hover:bg-red-700 disabled:opacity-50 rounded text-sm"
        >
          취소
        </button>
      </div>
    </div>
  );
};

export default PositionCard;
