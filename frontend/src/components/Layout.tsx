import React, { useState } from "react";
import Sidebar from "./Sidebar";

const Layout = () => {
  const [tabs, setTabs] = useState<{
    id: string;
    label: string;
    content: React.ReactNode;
  }[]>([]);
  const [activeTab, setActiveTab] = useState<string | null>(null);

  const openTab = (id: string, label: string, content: React.ReactNode) => {
    setTabs((prev) =>
      prev.some((tab) => tab.id === id) ? prev : [...prev, { id, label, content }]
    );
    setActiveTab(id);
  };

  const closeTab = (id: string) => {
    setTabs((prev) => prev.filter((tab) => tab.id !== id));
    if (activeTab === id) {
      const remainingTabs = tabs.filter((tab) => tab.id !== id);
      setActiveTab(remainingTabs.length > 0 ? remainingTabs[remainingTabs.length - 1].id : null);
    }
  };

  const handleLogout = async () => {
    await fetch("/logout", {
      method: "POST",
      credentials: "include",
    });
    localStorage.clear();
    window.location.href = "/login?force=true";
  };
  

  const user = localStorage.getItem("user");
  const parsedUser = user ? JSON.parse(user) : null;
  const displayName = parsedUser?.nickName || parsedUser?.name || "사용자";

  return (
    <div className="flex h-screen bg-gray-900 text-gray-200">
      {/* Sidebar */}
      <Sidebar openTab={openTab} />

      {/* Main Content */}
      <div className="flex-1 flex flex-col">
        {/* Topbar */}
        <header className="bg-gray-800 p-4 shadow flex justify-between items-center">
          <div className="text-lg font-bold text-yellow-400">AutoTrader</div>
          <div className="text-sm font-semibold flex items-center space-x-4">
            <span>
              로그인한 닉네임: <span className="text-yellow-300">{displayName}</span>
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
              <div key={tab.id} className="flex items-center bg-gray-700 rounded-md">
                <button
                  onClick={() => setActiveTab(tab.id)}
                  className={`px-4 py-2 font-semibold focus:outline-none ${
                    activeTab === tab.id
                      ? "text-yellow-300 border-b-2 border-yellow-400"
                      : "text-gray-300 hover:bg-gray-600"
                  }`}
                >
                  {tab.label}
                </button>
                <button
                  onClick={() => closeTab(tab.id)}
                  className="text-gray-400 hover:text-red-500 px-2"
                >
                  ✕
                </button>
              </div>
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
