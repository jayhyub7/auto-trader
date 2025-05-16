import {
  PositionOpenStatuses,
  AmountTypes,
  PositionOpenStatus,
  PositionOpenPayload,
} from "@/features/position-open/services/PositionOpenService";
import { Position } from "@/features/position-manager/services/PositionManagerService";
import { ExchangeBalance } from "@/service/balanceService";
import { toast, ToastContainer } from "react-toastify";
import AmountSelector from "./AmountSelector";
import React, { useState, useEffect } from "react";

interface Props {
  position: Position;
  balance: number;
  available: number;
  status: PositionOpenStatus;
  openData?: any;
  onUpdateStatus: (
    id: string,
    status: PositionOpenStatus,
    payload: Omit<PositionOpenPayload, "status">
  ) => void;
  onDelete: (id: number) => void;
}

const PositionCard = ({
  position,
  balance,
  available,
  status,
  openData,
  onUpdateStatus,
  onDelete,
}: Props) => {
  const [takeProfit, setTakeProfit] = useState(openData?.takeProfit ?? 0);
  const [stopLoss, setStopLoss] = useState(openData?.stopLoss ?? 0);
  const [amountType, setAmountType] = useState(
    openData?.amountType ?? AmountTypes.FIXED
  );
  const [amount, setAmount] = useState(openData?.amount ?? 0);
  const [percent, setPercent] = useState(0);

  useEffect(() => {
    if (openData) {
      setTakeProfit(openData.takeProfit ?? 0);
      setStopLoss(openData.stopLoss);
      setAmount(openData.amount);
      setAmountType(openData.amountType ?? AmountTypes.FIXED);
      if (openData.amountType === AmountTypes.PERCENT) {
        setPercent(openData.amount);
      } else {
        setAmountType(AmountTypes.FIXED);
      }
    }
  }, [openData]);

  const openId = openData?.id ?? position.open?.id;
  const isStartable =
    status === PositionOpenStatuses.IDLE ||
    status === PositionOpenStatuses.CANCELLED;
  const isRunning = status === PositionOpenStatuses.RUNNING;
  const isSimulating = status === PositionOpenStatuses.SIMULATING;

  const handleClick = (nextStatus: PositionOpenStatus) => {
    const entryConditions = position.conditions.filter(
      (c) => c.conditionPhase === "ENTRY"
    );

    if (nextStatus !== PositionOpenStatuses.CANCELLED) {
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
    }

    const payload: Omit<PositionOpenPayload, "status"> = {
      id: openId,
      positionId: position.id,
      amount: amountType === AmountTypes.PERCENT ? percent : amount,
      amountType: amountType ?? AmountTypes.FIXED,
      stopLoss,
      takeProfit,
    };
    onUpdateStatus(openId, nextStatus, payload);
  };

  return (
    <div className="border p-4 rounded-xl shadow-sm space-y-2">
      <div className="flex justify-between items-center mb-2">
        <div className="text-lg font-semibold">
          {position.title}
          {position.direction === "LONG" && (
            <span className="ml-2 text-green-400 text-sm font-semibold">
              [롱]
            </span>
          )}
          {position.direction === "SHORT" && (
            <span className="ml-2 text-red-400 text-sm font-semibold">
              [숏]
            </span>
          )}
        </div>
      </div>

      {position.conditions.length > 0 && (
        <div className="text-sm text-gray-300">
          조건:
          <ul className="list-disc ml-6 mt-1 space-y-1">
            {position.conditions.map((cond, idx) => (
              <li
                key={idx}
                className={
                  cond.enabled ? "text-white" : "text-gray-500 line-through"
                }
              >
                [{position.direction?.toUpperCase()}][{cond.type.toUpperCase()}]{" "}
                {cond.timeframe} {cond.operator} (
                {cond.conditionPhase === "ENTRY" ? "진입" : "종료"})
              </li>
            ))}
          </ul>
        </div>
      )}

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
            value={amountType ?? AmountTypes.FIXED}
            onChange={(e) => setAmountType(e.target.value as AmountTypes)}
            className="w-full px-2 py-1 rounded bg-gray-700 border border-gray-600"
          >
            <option value={AmountTypes.FIXED}>고정 금액</option>
            <option value={AmountTypes.PERCENT}>비율</option>
          </select>
        </div>

        <div>
          {amountType === AmountTypes.FIXED ? (
            <div>
              <label className="block mb-1 text-sm">금액</label>
              <input
                type="number"
                value={amount}
                onChange={(e) => {
                  const value = Number(e.target.value);
                  if (value <= available) {
                    setAmount(value);
                  } else {
                    toast.error(
                      `사용 가능 금액(${available.toFixed(
                        2
                      )} USDT)을 초과할 수 없습니다.`
                    );
                    setAmount(available);
                  }
                }}
                className="w-full px-2 py-1 rounded bg-gray-700 border border-gray-600"
                placeholder="예: 100"
              />
            </div>
          ) : (
            <AmountSelector
              maxAmount={available}
              onChange={(val) => setAmount(val)}
              onPercentChange={(p) => setPercent(p)}
              initialPercent={percent}
            />
          )}
        </div>

        <div className="col-span-2 text-sm text-gray-400">
          잔고:{" "}
          {balance.toLocaleString(undefined, {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2,
          })}{" "}
          USDT
          <br />
          사용 가능:{" "}
          {available.toLocaleString(undefined, {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2,
          })}{" "}
          USDT
        </div>
      </div>

      <div className="flex gap-3 items-center">
        <span className="text-sm text-yellow-400">상태: {status}</span>

        <button
          disabled={!isStartable}
          onClick={() => handleClick(PositionOpenStatuses.RUNNING)}
          className="px-3 py-1 bg-green-600 hover:bg-green-700 disabled:opacity-50 rounded text-sm"
        >
          실행
        </button>

        <button
          disabled={!isStartable}
          onClick={() => handleClick(PositionOpenStatuses.SIMULATING)}
          className="px-3 py-1 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 rounded text-sm"
        >
          시뮬레이션
        </button>

        <button
          disabled={!(isRunning || isSimulating)}
          onClick={() => handleClick(PositionOpenStatuses.CANCELLED)}
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
