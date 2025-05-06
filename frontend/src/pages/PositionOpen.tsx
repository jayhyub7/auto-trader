import React, { useEffect, useState } from "react";
import { PositionOpenDto, fetchMyOpenPositions, deletePositionOpen } from "@/service/positionOpenService";
import { ExchangeBalance, fetchBalances } from "@/service/balanceService";
import PositionCard from "@/components/PositionCard";
import {
  Position,
  saveOrUpdatePositionOpen,
  PositionOpenPayload,
  PositionOpenStatus,
} from "@/service/positionOpenService";
import { toast } from "react-toastify";
import { ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";

const PositionOpen = () => {
  const [positions, setPositions] = useState<Position[]>([]);
  const [balances, setBalances] = useState<Record<string, ExchangeBalance>>({});
  const [statusMap, setStatusMap] = useState<Record<number, PositionOpenStatus>>({});
  const [loadingBalances, setLoadingBalances] = useState(true);
  const [openDataMap, setOpenDataMap] = useState<Record<number, PositionOpenDto>>({}); // ✅ 수정
  const openDataInitMap: Record<number, PositionOpenDto> = {};
  const [availableMap, setAvailableMap] = useState<Record<string, number>>({});

  useEffect(() => {
    fetchMyOpenPositions().then((data) => {
      const enabledPositions = data.filter((p) => p.enabled);
      setPositions(enabledPositions);
  
      const initialStatus: Record<number, PositionOpenStatus> = {};
      const openDataInitMap: Record<number, PositionOpenDto> = {};
      
      enabledPositions.forEach((p) => {
        
        initialStatus[p.id] = "IDLE";
        if (p.open) {
          openDataInitMap[p.id] = p.open;
          initialStatus[p.id] = p.open.status;
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
        converted[entry.exchange] = usdt?.total ?? 0;
        availableMap[entry.exchange] = usdt?.available ?? 0;
        
      });
      //console.log('availableMap : ',availableMap)
      setBalances(converted);
      setAvailableMap(availableMap);
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

  const handleDelete = async (positionId: number) => {
    try {
      const open = openDataMap[positionId];
      if (open?.id) {
        await deletePositionOpen(open.id); // ✅ 서버에서 삭제
      }
  
      // ✅ 전체 포지션 재조회
      const updatedPositions = await fetchMyOpenPositions();
      setPositions(updatedPositions);
  
      // ✅ 상태 및 오픈 정보 재설정
      const newStatusMap = Object.fromEntries(
        updatedPositions.map((p) => [p.id, p.open?.status || "idle"])
      );
      const newOpenDataMap = Object.fromEntries(
        updatedPositions.map((p) => [p.id, p.open || null])
      );
      setStatusMap(newStatusMap);
      setOpenDataMap(newOpenDataMap);
  
      toast.success("포지션이 삭제되었습니다.");
    } catch (error: any) {
      console.error("❌ 서버 삭제 실패:", error);
      const message = error?.response?.data?.message || "삭제 중 오류가 발생했습니다.";
      toast.error(message); // ✅ 사용자에게 메시지 표시
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
            available={availableMap[position.exchange.toUpperCase()] ?? 0}
            status={statusMap[position.id] ?? "IDLE"}
            onUpdateStatus={updateStatus}
            openData={openDataMap[position.id]}
            onDelete={handleDelete}
          />
        ))
      )}
      <ToastContainer
        position="top-center" // 👈 여기 위치 조정
        autoClose={3000}
        hideProgressBar={false}
        newestOnTop={false}
        closeOnClick
        rtl={false}
        pauseOnFocusLoss
        draggable
        pauseOnHover
      />

    </div>
  );
  
};

export default PositionOpen;
