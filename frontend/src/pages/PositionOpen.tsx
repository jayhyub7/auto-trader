import React, { useEffect, useState } from "react";
import { fetchPositions } from "@/service/positionManager";
import { ExchangeBalance, fetchBalances } from "@/service/balanceService";
import PositionCard from "@/components/PositionCard";
import {
  Position,
  saveOrUpdatePositionOpen,
  PositionOpenPayload,
  PositionOpenStatus, // ✅ 기존 type PositionStatus 제거 후 import
} from "@/service/positionOpenService";

const PositionOpen = () => {
  const [positions, setPositions] = useState<Position[]>([]);
  const [balances, setBalances] = useState<Record<string, ExchangeBalance>>({});
  const [statusMap, setStatusMap] = useState<Record<string, PositionOpenStatus>>({});
  const [loadingBalances, setLoadingBalances] = useState(true);

  useEffect(() => {
    fetchPositions().then((data) => {
      const enabledPositions = data.filter((p) => p.enabled);
      setPositions(enabledPositions);
      const initialStatus: Record<string, PositionOpenStatus> = {};
      enabledPositions.forEach((p) => {
        initialStatus[p.id] = "idle";
      });
      setStatusMap(initialStatus);
    });

    fetchBalances()
      .then(setBalances)
      .finally(() => setLoadingBalances(false));
  }, []);

  const updateStatus = async (
    id: string,
    status: PositionOpenStatus,
    payload: Omit<PositionOpenPayload, "status">
  ) => {
    const fullPayload: PositionOpenPayload = {
      ...payload,
      id,
      status,
    };

    try {
      await saveOrUpdatePositionOpen(fullPayload);
      setStatusMap((prev) => ({ ...prev, [id]: status }));
    } catch (err) {
      console.error("상태 업데이트 실패", err);
      alert("상태 업데이트에 실패했습니다.");
    }
  };

  return (
    <div className="p-4 space-y-6">
      {positions.map((pos) => (
        <PositionCard
          key={pos.id}
          position={pos}
          balance={balances[pos.exchange]?.totalUsdValue || 0}
          status={statusMap[pos.id]}
          onUpdateStatus={updateStatus}
        />
      ))}
    </div>
  );
};

export default PositionOpen;
