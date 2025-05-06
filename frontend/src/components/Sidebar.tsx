import React from "react";
import { useAuth } from "../hooks/useAuth";
const ADMIN_EMAIL = import.meta.env.VITE_ADMIN_EMAIL;
import AdminKeyManager from "../pages/AdminKeyManager";
import AuthKeyManager from "../pages/AuthKeyManager";
import CurrentBalance from "../pages/CurrentBalance"; 
import PositionManager from "../pages/PositionManager"; 
import PositionOpen from "../pages/PositionOpen"; 


const Sidebar = ({
  openTab,
}: {
  openTab: (id: string, label: string, content: React.ReactNode) => void;
}) => {
  const { user } = useAuth();

  return (
    <aside className="w-64 bg-gray-800 text-white p-4 space-y-4">
      <h2 className="text-xl font-bold mb-6">📂 AutoTrader</h2>

      <ul className="space-y-2 text-sm">
        {/* ✅ 관리자 전용 탭 */}
        {user?.email === ADMIN_EMAIL && (
          <li
            className="hover:text-yellow-400 cursor-pointer"
            onClick={() => openTab("admin-key-manager", "관리자 인증키 설정", <AdminKeyManager />)}
          >
            🔐 관리자 인증키 설정
          </li>
        )}      
        <li
          className="hover:text-yellow-400 cursor-pointer"
          onClick={() => openTab("auth-key-manager", "인증키 관리", <AuthKeyManager />)}
        >
          🔐 인증키 관리
        </li>
        <li
          className="hover:text-yellow-400 cursor-pointer"
          onClick={() => openTab("current-balance", "현재 잔고", <CurrentBalance />)}
        >
          💰 현재 잔고
        </li>
        <li
          className="hover:text-yellow-400 cursor-pointer"
          onClick={() => openTab("position-manager", "포지션 관리", <PositionManager />)}
        >
          💰 포지션 관리
        </li>    
        <li
          className="hover:text-yellow-400 cursor-pointer"
          onClick={() => openTab("position-open", "포지션 오픈", <PositionOpen />)}
        >
          💰 포지션 오픈
        </li>              
      </ul>
    </aside>
  );
};

export default Sidebar;
