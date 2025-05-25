// AmountSelector.tsx

import React, { useEffect, useState } from "react";
import { toast, ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";

interface AmountSelectorProps {
  maxAmount: number;
  onChange: (amount: number) => void;
  onPercentChange?: (percent: number) => void;
  initialPercent?: number;
}

const quickPercents = [25, 50, 75, 100];

const AmountSelector: React.FC<AmountSelectorProps> = ({
  maxAmount,
  onChange,
  onPercentChange,
  initialPercent,
}) => {
  const [percent, setPercent] = useState(initialPercent ?? 0);
  const [amount, setAmount] = useState(0);

  useEffect(() => {
    const calculated = +(maxAmount * (percent / 100)).toFixed(4);
    setAmount(calculated);
    onChange(calculated);
    if (onPercentChange) {
      onPercentChange(percent);
    }
  }, [percent, maxAmount, onChange, onPercentChange]);

  const handleAmountChange = (value: string) => {
    const parsed = parseFloat(value);
    if (!isNaN(parsed)) {
      if (parsed > maxAmount) {
        toast.error(
          `사용 가능 금액(${maxAmount.toFixed(2)} USDT)을 초과할 수 없습니다.`
        );
        const capped = +maxAmount.toFixed(4);
        setAmount(capped);
        setPercent(100);
        onChange(capped);
        if (onPercentChange) onPercentChange(100);
      } else {
        setAmount(parsed);
        const newPercent = Math.round((parsed / maxAmount) * 100);
        setPercent(newPercent);
        onChange(parsed);
        if (onPercentChange) onPercentChange(newPercent);
      }
    } else {
      setAmount(0);
      setPercent(0);
      onChange(0);
      if (onPercentChange) onPercentChange(0);
    }
  };

  return (
    <div className="bg-gray-800 p-1 rounded-md text-white text-xs">
      <label className="block mb-1 text-gray-300">비율 선택</label>
      <input
        type="range"
        min={0}
        max={100}
        step={1}
        value={percent}
        onChange={(e) => setPercent(Number(e.target.value))}
        className="w-full mb-1"
      />
      <div className="flex justify-between text-[10px] text-gray-400 mb-1">
        <span>0%</span>
        <span>100%</span>
      </div>

      <div className="flex gap-1 mb-1">
        {quickPercents.map((p) => (
          <button
            key={p}
            onClick={() => setPercent(p)}
            className={`px-2 py-[2px] rounded text-xs ${
              percent === p ? "bg-yellow-500 text-black" : "bg-gray-700"
            }`}
          >
            {p}%
          </button>
        ))}
      </div>

      <div>
        <input
          type="text"
          value={`(${percent}%) ${amount}`}
          readOnly
          className="w-full bg-gray-800 text-white text-xs px-2 py-1 rounded border border-gray-600"
        />
      </div>

      <div className="text-right text-[10px] text-gray-400 mt-1">
        최대 가능: {maxAmount} USDT
      </div>
    </div>
  );
};

export default AmountSelector;
