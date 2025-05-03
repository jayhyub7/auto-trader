import React, { useState } from "react";

const Layout = () => {
  const [tabs, setTabs] = useState<{ id: string; label: string; content: React.ReactNode }[]>([]);
  const [activeTab, setActiveTab] = useState<string | null>(null);
  const [openMenu, setOpenMenu] = useState<string | null>(null);

  const toggleMenu = (menu: string) => {
    setOpenMenu(openMenu === menu ? null : menu);
  };

  const openTab = (id: string, label: string, content: React.ReactNode) => {
    setTabs((prev) =>
      prev.some((tab) => tab.id === id) ? prev : [...prev, { id, label, content }]
    );
    setActiveTab(id);
  };

  const handleLogout = async () => {
    await fetch("/api/logout", {
      method: "POST",
      credentials: "include",
    });
    localStorage.clear();
    window.location.href = "/login";
  };

  const user = localStorage.getItem("user");
  const parsedUser = user ? JSON.parse(user) : null;
  const displayName = parsedUser?.nickName || parsedUser?.name || "사용자";

  return (
    <div className="flex h-screen bg-gray-900 text-gray-200">
      {/* Sidebar */}
      <aside className="w-64 bg-gray-800 p-4 flex flex-col">
        <div className="text-lg font-extrabold text-white mb-4 tracking-wide">
          📁 트리 구조
        </div>

        <nav className="flex-1 text-sm">
          {/* 계정 관리 */}
          <div className="mb-4">
            <button
              onClick={() => toggleMenu("account")}
              className="w-full text-left font-semibold text-gray-200 hover:text-yellow-400"
            >
              👤 계정정보 관리
            </button>
            {openMenu === "account" && (
              <ul className="pl-4 mt-2 space-y-2 text-gray-300">
                <li
                  className="hover:text-yellow-400 cursor-pointer"
                  onClick={() =>
                    openTab("auth-key", "인증키 관리", <div>🔐 인증키 관리 페이지</div>)
                  }
                >
                  🔐 인증키 관리
                </li>
              </ul>
            )}
          </div>

          {/* 대시보드 */}
          <div>
            <button
              onClick={() => toggleMenu("dashboard")}
              className="w-full text-left font-semibold text-gray-200 hover:text-yellow-400"
            >
              📊 대시보드
            </button>
            {openMenu === "dashboard" && (
              <ul className="pl-4 mt-2 space-y-2 text-gray-300">
                <li
                  className="hover:text-yellow-400 cursor-pointer"
                  onClick={() =>
                    openTab("sample-dashboard", "대시보드 탭", <div>📊 대시보드 내용</div>)
                  }
                >
                  📋 샘플 탭 열기
                </li>
              </ul>
            )}
          </div>
        </nav>

        <div className="text-xs text-gray-500 mt-auto">© AutoTrader</div>
      </aside>

      {/* Main Content */}
      <div className="flex-1 flex flex-col">
        {/* Topbar */}
        <header className="bg-gray-800 p-4 shadow flex justify-between items-center">
          <div className="text-lg font-bold text-yellow-400">AutoTrader</div>
          <div className="text-sm font-semibold flex items-center space-x-4">
            <span>
              로그인한 닉네임:{" "}
              <span className="text-yellow-300">{displayName}</span>
            </span>
            <button
              onClick={handleLogout}
              className="bg-red-600 hover:bg-red-700 text-white text-xs px-3 py-1 rounded"
            >
              로그아웃
            </button>
          </div>
        </header>

        {/* Dynamic Tabs */}
        <nav className="bg-gray-800 px-6 py-3 border-b border-gray-700">
          <div className="flex space-x-2">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`px-4 py-2 rounded-md font-semibold ${
                  activeTab === tab.id
                    ? "bg-gray-700 text-yellow-300 border border-yellow-400 shadow-inner"
                    : "bg-gray-700 text-gray-300 hover:bg-gray-600"
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </nav>

        <main className="flex-1 p-6 overflow-y-auto bg-gray-900">
          {tabs.find((tab) => tab.id === activeTab)?.content ?? (
            <div className="text-gray-500">왼쪽 메뉴를 클릭해 콘텐츠를 열어보세요.</div>
          )}
        </main>
      </div>
    </div>
  );
};

export default Layout;
