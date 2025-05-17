// Sidebar.tsx

import React from "react";
import { useAuth } from "../hooks/useAuth";
const ADMIN_EMAIL = import.meta.env.VITE_ADMIN_EMAIL;

const Sidebar = ({
  openTab,
}: {
  openTab: (id: string, label: string) => void;
}) => {
  const { user } = useAuth();

  return (
    <aside className="w-64 bg-gray-800 text-white p-4 space-y-4">
      <h2 className="text-xl font-bold mb-6">📂 AutoTrader</h2>
      <ul className="space-y-2 text-sm">
        {user?.email === ADMIN_EMAIL && (
          <>
            <li
              onClick={() => openTab("admin-key-manager", "관리자 인증키 설정")}
              className="hover:text-yellow-400 cursor-pointer"
            >
              🔐 관리자 인증키 설정
            </li>
            <li
              onClick={() =>
                openTab("scheduler-toggle-manager", "스케줄러 제어")
              }
              className="hover:text-yellow-400 cursor-pointer"
            >
              ⚙️ 스케줄러 제어
            </li>
            <li
              onClick={() => openTab("indicator-comparison", "지표 오차 비교")}
              className="hover:text-yellow-400 cursor-pointer"
            >
              📊 지표 오차 비교
            </li>
          </>
        )}
        <li
          onClick={() => openTab("auth-key-manager", "인증키 관리")}
          className="hover:text-yellow-400 cursor-pointer"
        >
          🔐 인증키 관리
        </li>
        <li
          onClick={() => openTab("current-balance", "현재 잔고")}
          className="hover:text-yellow-400 cursor-pointer"
        >
          💰 현재 잔고
        </li>
        <li
          onClick={() => openTab("position-manager", "포지션 관리")}
          className="hover:text-yellow-400 cursor-pointer"
        >
          💰 포지션 관리
        </li>
        <li
          onClick={() => openTab("position-open", "포지션 오픈")}
          className="hover:text-yellow-400 cursor-pointer"
        >
          💰 포지션 오픈
        </li>
      </ul>
    </aside>
  );
};

export default Sidebar;
