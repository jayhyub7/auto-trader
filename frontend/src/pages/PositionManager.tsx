import React from "react";
import BitcoinChart from "@/components/BitcoinChart";

const PositionManager = () => {
  return (
    <div className="p-4">
      <h2 className="text-white mb-2 text-lg">📈 포지션 차트</h2>
      <BitcoinChart />
    </div>
  );
};

export default PositionManager;
