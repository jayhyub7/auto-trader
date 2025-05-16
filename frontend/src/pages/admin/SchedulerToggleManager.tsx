import React, { useEffect, useState } from "react";
import {
  SchedulerStatusDto,
  SchedulerType,
  SchedulerTypes,
  getSchedulerStatus,
  updateSchedulerStatus,
} from "@/service/schedulerToggleManagerService";

const schedulerLabels: Record<SchedulerType, string> = {
  ENTRY: "진입 스케줄러",
  EXIT: "종료 스케줄러",
  BALANCE: "잔고 스케줄러",
  INDICATOR: "지표 스케줄러",
  FX: "환율 스케줄러",
};

const SchedulerToggleManager: React.FC = () => {
  const [statusList, setStatusList] = useState<SchedulerStatusDto[]>([]);

  useEffect(() => {
    getSchedulerStatus()
      .then(setStatusList)
      .catch((err) => console.error("현재 상태 조회 실패", err));
  }, []);

  const toggleField = (index: number, field: keyof SchedulerStatusDto) => {
    const updated = [...statusList];
    updated[index] = {
      ...updated[index],
      [field]: !updated[index][field],
    };
    setStatusList(updated);
    updateSchedulerStatus(updated[index]).catch((err) =>
      console.error("시전 조정 실패", err)
    );
  };

  return (
    <div className="p-4">
      <h2 className="text-xl font-bold mb-2">스케줄러 제어</h2>
      <table className="w-full text-left">
        <thead>
          <tr>
            <th className="p-2">스케줄러</th>
            <th className="p-2">실행</th>
            <th className="p-2">로그</th>
          </tr>
        </thead>
        <tbody>
          {statusList.map((status, idx) => (
            <tr key={status.type}>
              <td className="p-2">{schedulerLabels[status.type]}</td>
              <td className="p-2">
                <input
                  type="checkbox"
                  checked={status.enabled}
                  onChange={() => toggleField(idx, "enabled")}
                />
              </td>
              <td className="p-2">
                <input
                  type="checkbox"
                  checked={status.log}
                  onChange={() => toggleField(idx, "log")}
                />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default SchedulerToggleManager;
