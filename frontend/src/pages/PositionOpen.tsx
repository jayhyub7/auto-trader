import React, { useEffect, useState } from "react";
import { PositionOpenDto, fetchMyOpenPositions } from "@/service/positionOpenService";
import { ExchangeBalance, fetchBalances } from "@/service/balanceService";
import PositionCard from "@/components/PositionCard";
import {
  Position,
  saveOrUpdatePositionOpen,
  PositionOpenPayload,
  PositionOpenStatus,
} from "@/service/positionOpenService";

const PositionOpen = () => {
  const [positions, setPositions] = useState<Position[]>([]);
  const [balances, setBalances] = useState<Record<string, ExchangeBalance>>({});
  const [statusMap, setStatusMap] = useState<Record<number, PositionOpenStatus>>({});
  const [loadingBalances, setLoadingBalances] = useState(true);
  const [openDataMap, setOpenDataMap] = useState<Record<number, PositionOpenDto>>({}); // ✅ 수정
  const openDataInitMap: Record<number, PositionOpenDto> = {};

  useEffect(() => {
    fetchMyOpenPositions().then((data) => {
      const enabledPositions = data.filter((p) => p.enabled);
      setPositions(enabledPositions);
  
      const initialStatus: Record<number, PositionOpenStatus> = {};
      const openDataInitMap: Record<number, PositionOpenDto> = {};
  
      enabledPositions.forEach((p) => {
        initialStatus[p.id] = "idle";
        if (p.open) {
          openDataInitMap[p.id] = p.open;
        }
      });
  
      setStatusMap(initialStatus);
      setOpenDataMap(openDataInitMap);
    });
  
    fetchBalances()
    .then((data) => {
      const converted: Record<string, number> = {};
      Object.values(data).forEach((entry) => {
        const usdt = entry.balances.find((b) => b.asset === "USDT");
        console.log('entry.balances : ', entry.balances);
        console.log('usdt : ', usdt);
        converted[entry.exchange] = usdt?.total ?? 0;
      });
      console.log('converted : ', converted)
      setBalances(converted);
    })
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
      const result = await saveOrUpdatePositionOpen(fullPayload);
      setStatusMap((prev) => ({ ...prev, [payload.positionId]: status }));

      if (result?.id) {
        setOpenDataMap((prev) => ({
          ...prev,
          [result.positionId]: result, // ✅ 숫자 key로 저장
        }));
      }
    } catch (err) {
      console.error("상태 업데이트 실패", err);
      alert("상태 업데이트에 실패했습니다.");
    }
  };

  return (
    <div className="p-4 space-y-6">
      {positions.length === 0 ? (
        <div className="flex items-center justify-center h-64 text-gray-400 text-sm">
          📭 포지션 관리에서 포지션을 등록하세요.
        </div>
      ) : (
        positions.map((position) => (
          <PositionCard
            key={position.id}
            position={position}
            balance={balances[position.exchange] ?? 0}
            status={statusMap[position.id] ?? "idle"}
            onUpdateStatus={updateStatus}
            openData={openDataMap[position.id]}
          />
        ))
      )}
    </div>
  );
  
};

export default PositionOpen;
