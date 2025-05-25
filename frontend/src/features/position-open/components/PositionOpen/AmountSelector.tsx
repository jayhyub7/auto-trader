import React, { useState, useEffect } from "react";
import { toast, ToastContainer } from "react-toastify"; // 반드시 import 필요
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
    setPercent(initialPercent ?? 0);
    setAmount(calculated);
    onChange(calculated);
    if (onPercentChange) {
      onPercentChange(percent); // ✅ 비율도 전달
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
    <div className="bg-gray-800 p-4 rounded-xl text-white">
      <label className="block mb-2 text-sm text-gray-300">비율 선택</label>
      <input
        type="range"
        min={0}
        max={100}
        step={1}
        value={percent}
        onChange={(e) => setPercent(Number(e.target.value))}
        className="w-full mb-2"
      />
      <div className="flex justify-between text-xs text-gray-400 mb-2">
        <span>0%</span>
        <span>100%</span>
      </div>

      <div className="flex gap-2 mb-2">
        {quickPercents.map((p) => (
          <button
            key={p}
            onClick={() => setPercent(p)}
            className={`px-2 py-1 rounded text-sm ${
              percent === p ? "bg-yellow-500 text-black" : "bg-gray-700"
            }`}
          >
            {p}%
          </button>
        ))}
      </div>

      <div className="mt-4">
        <label className="block mb-1 text-sm text-gray-300">
          비율 선택시 투입 금액은 사용가능 금액에 따라 변합니다.
        </label>
        <input
          type="text"
          value={`(${percent}%) ${amount}`}
          readOnly // ✅ readonly 속성은 boolean입니다
          className="w-full bg-gray-800 text-white p-2 rounded"
        />
      </div>

      <div className="text-right text-xs text-gray-400 mt-1">
        최대 가능: {maxAmount} USDT
      </div>
    </div>
  );
};

export default AmountSelector;
