import React, { useState, useMemo } from "react";

interface AmountSliderProps {
  maxAmount: number; // 총 잔고 (예: 100 USDT)
  unit?: string;     // 단위 표시 (기본: USDT)
}

const AmountSlider: React.FC<AmountSliderProps> = ({ maxAmount, unit = "USDT" }) => {
  const [percentage, setPercentage] = useState(0);

  const calculatedAmount = useMemo(() => {
    return ((percentage / 100) * maxAmount).toFixed(2);
  }, [percentage, maxAmount]);

  const handleQuickSelect = (value: number) => {
    setPercentage(value);
  };

  return (
    <div className="w-full bg-gray-900 p-4 rounded-2xl shadow">
      <div className="flex justify-between items-center mb-2">
        <span className="text-sm text-white font-medium">주문 비율</span>
        <span className="text-sm text-yellow-400 font-semibold">{percentage}%</span>
      </div>

      <input
        type="range"
        min={0}
        max={100}
        step={1}
        value={percentage}
        onChange={(e) => setPercentage(Number(e.target.value))}
        className="w-full accent-yellow-400"
      />

      <div className="flex justify-between text-xs text-gray-400 mt-1">
        <span>0%</span>
        <span>100%</span>
      </div>

      <div className="flex justify-center gap-2 mt-3">
        {[25, 50, 75, 100].map((v) => (
          <button
            key={v}
            className={`px-3 py-1 text-sm rounded-full border border-yellow-500 text-yellow-400 hover:bg-yellow-600 hover:text-white transition ${
              percentage === v ? "bg-yellow-500 text-black" : ""
            }`}
            onClick={() => handleQuickSelect(v)}
          >
            {v}%
          </button>
        ))}
      </div>

      <div className="mt-4 text-right text-white text-sm">
        사용 금액: <span className="font-bold text-yellow-300">{calculatedAmount} {unit}</span>
      </div>
    </div>
  );
};

export default AmountSlider;
