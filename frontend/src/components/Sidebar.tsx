import { useState } from "react";

const Sidebar = () => {
  const [openMenu, setOpenMenu] = useState<string | null>(null);

  const toggleMenu = (menu: string) => {
    setOpenMenu(openMenu === menu ? null : menu);
  };

  return (
    <aside className="w-64 bg-gray-800 text-white p-4 space-y-4">
      <h2 className="text-xl font-bold mb-6">📂 AutoTrader</h2>

      {/* 트리 메뉴 */}
      <div>
        <p className="text-sm text-gray-400 mb-2">📁 메뉴</p>
        <ul className="space-y-2 text-sm">
          {/* 메뉴 1 */}
          <li>
            <button
              onClick={() => toggleMenu("dashboard")}
              className="w-full text-left hover:text-yellow-300"
            >
              📊 대시보드
            </button>
            {openMenu === "dashboard" && (
              <ul className="pl-4 mt-1 space-y-1 text-gray-300">
                <li className="hover:text-yellow-400 cursor-pointer">일일 리포트</li>
                <li className="hover:text-yellow-400 cursor-pointer">월간 성과</li>
              </ul>
            )}
          </li>

          {/* 메뉴 2 */}
          <li>
            <button
              onClick={() => toggleMenu("account")}
              className="w-full text-left hover:text-yellow-300"
            >
              💳 계좌 관리
            </button>
            {openMenu === "account" && (
              <ul className="pl-4 mt-1 space-y-1 text-gray-300">
                <li className="hover:text-yellow-400 cursor-pointer">계좌 등록</li>
                <li className="hover:text-yellow-400 cursor-pointer">연결 상태</li>
              </ul>
            )}
          </li>

          {/* 메뉴 3 */}
          <li>
            <button
              onClick={() => toggleMenu("settings")}
              className="w-full text-left hover:text-yellow-300"
            >
              ⚙️ 설정
            </button>
            {openMenu === "settings" && (
              <ul className="pl-4 mt-1 space-y-1 text-gray-300">
                <li className="hover:text-yellow-400 cursor-pointer">전략 설정</li>
                <li className="hover:text-yellow-400 cursor-pointer">알림 설정</li>
              </ul>
            )}
          </li>
        </ul>
      </div>
    </aside>
  );
};

export default Sidebar;
