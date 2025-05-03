import React from "react";
import AuthKeyManager from "../pages/AuthKeyManager";
import CurrentBalance from "../pages/CurrentBalance"; // ⬅️ 현재 잔고 페이지 추가

const Sidebar = ({
  openTab,
}: {
  openTab: (id: string, label: string, content: React.ReactNode) => void;
}) => {
  return (
    <aside className="w-64 bg-gray-800 text-white p-4 space-y-4">
      <h2 className="text-xl font-bold mb-6">📂 AutoTrader</h2>

      <ul className="space-y-2 text-sm">
        <li
          className="hover:text-yellow-400 cursor-pointer"
          onClick={() => openTab("auth-key", "인증키 관리", <AuthKeyManager />)}
        >
          🔐 인증키 관리
        </li>
        <li
          className="hover:text-yellow-400 cursor-pointer"
          onClick={() => openTab("current-balance", "현재 잔고", <CurrentBalance />)}
        >
          💰 현재 잔고
        </li>
      </ul>
    </aside>
  );
};

export default Sidebar;
