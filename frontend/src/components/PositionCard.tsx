import React, { useState, useEffect } from "react";
import AmountSelector from "@/components/AmountSelector";
import {
  Position,
  PositionOpenPayload,
  PositionOpenStatus,
  PositionOpenDto,
} from "@/service/positionOpenService";
import { toast, ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import { Exchange } from "@/constants/Exchange"; // ⬅️ 꼭 import 필요


interface PositionCardProps {
  position: Position;
  balance: number;
  status: PositionOpenStatus;
  onUpdateStatus: (
    id: number | undefined,
    status: PositionOpenStatus,
    payload: Omit<PositionOpenPayload, "status">
  ) => void;
  openData?: PositionOpenDto;
}

const PositionCard: React.FC<PositionCardProps> = ({
  position,
  balance,
  status,
  onUpdateStatus,
  openData,
}) => {
  const [takeProfit, setTakeProfit] = useState(openData?.takeProfit ?? 0);
  const [stopLoss, setStopLoss] = useState(openData?.stopLoss ?? 0);
  const [amountType, setAmountType] = useState<"fixed" | "percent">(openData?.amountType ?? "fixed");
  const [amount, setAmount] = useState(openData?.amount ?? 0);
  console.log('balance : ', balance)
  console.log("balance keys:", Object.keys(balance));
console.log("📌 position.exchange =", position.exchange);
console.log("💰 total balance =", balance[position.exchange]?.total); // ✅ 이렇게 직접 접근
  const exchangeEnumKey = Exchange[position.exchange as keyof typeof Exchange];
  
  const openId = openData?.id ?? position.open?.id;

  useEffect(() => {
    if (openData) {
      setTakeProfit(openData.takeProfit ?? 0);
      setStopLoss(openData.stopLoss);
      setAmount(openData.amount);
      setAmountType(openData.amountType);
    }
  }, [openData]);

  const handleClick = (status: PositionOpenStatus) => {
  // ✅ 진입 조건 필터링
  const entryConditions = position.conditions.filter(c => c.conditionPhase === "ENTRY");

  if (entryConditions.length === 0) {
    toast.error("진입 조건이 없습니다. 조건을 먼저 설정해주세요.");
    return;
  }

  if (!stopLoss || stopLoss <= 0) {
    toast.error("Stop Loss는 필수입니다. 0보다 커야 합니다.");
    return;
  }

  if (amount < 10) {
    toast.error("금액은 최소 10 이상이어야 합니다.");
    return;
  }    
    const payload: Omit<PositionOpenPayload, "status"> = {
      id: openId,
      positionId: position.id,
      amount,
      amountType,
      stopLoss,
      takeProfit,
    };
    console.log("🔥 [handleClick] 전송 payload:", payload);
    onUpdateStatus(openId, status, payload);
  };

  const isStartable = status === "idle" || status === "cancelled";
  const isRunning = status === "running";
  const isSimulating = status === "simulating";

  const statusColor =
    status === "running"
      ? "text-green-400"
      : status === "simulating"
      ? "text-blue-400"
      : status === "cancelled"
      ? "text-gray-400"
      : "text-yellow-400";

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
        잔고: {balance.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} USDT
        </div>

        
      </div>

      <div className="flex gap-3 items-center">
        <span className={`text-sm ${statusColor}`}>상태: {status}</span>

        <button
          disabled={!isStartable}
          onClick={() => handleClick("running")}
          className="px-3 py-1 bg-green-600 hover:bg-green-700 disabled:opacity-50 rounded text-sm"
        >
          실행
        </button>

        <button
          disabled={!isStartable}
          onClick={() => handleClick("simulating")}
          className="px-3 py-1 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 rounded text-sm"
        >
          시뮬레이션
        </button>

        <button
          disabled={!(isRunning || isSimulating)}
          onClick={() => handleClick("cancelled")}
          className="px-3 py-1 bg-red-600 hover:bg-red-700 disabled:opacity-50 rounded text-sm"
        >
          취소
        </button>
      </div>
      <ToastContainer position="top-center" autoClose={3000} />
    </div>
  );
};

export default PositionCard;
