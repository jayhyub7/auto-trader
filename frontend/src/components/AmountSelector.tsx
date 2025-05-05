import React, { useState, useEffect } from "react";

interface AmountSelectorProps {
  maxAmount: number;
  onChange: (amount: number) => void;
}

const quickPercents = [25, 50, 75, 100];

const AmountSelector: React.FC<AmountSelectorProps> = ({ maxAmount, onChange }) => {
  const [percent, setPercent] = useState(0);
  const [amount, setAmount] = useState(0);

  useEffect(() => {
    const calculated = +(maxAmount * (percent / 100)).toFixed(4);
    setAmount(calculated);
    onChange(calculated);
  }, [percent, maxAmount, onChange]);

  const handleAmountChange = (value: string) => {
    const parsed = parseFloat(value);
    if (!isNaN(parsed)) {
      setAmount(parsed);
      setPercent(Math.round((parsed / maxAmount) * 100));
      onChange(parsed);
    } else {
      setAmount(0);
      onChange(0);
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
        <label className="block mb-1 text-sm text-gray-300">금액 직접 입력</label>
        <input
          type="number"
          value={amount}
          onChange={(e) => handleAmountChange(e.target.value)}
          className="w-full bg-gray-700 text-white p-2 rounded"
        />
      </div>

      <div className="text-right text-xs text-gray-400 mt-1">
        최대 가능: {maxAmount} USDT
      </div>
    </div>
  );
};

export default AmountSelector;
