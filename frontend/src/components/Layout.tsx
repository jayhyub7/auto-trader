// Layout.tsx

import React, { useState } from "react";
import Sidebar from "./Sidebar";
import AdminKeyManager from "@/pages/admin/AdminKeyManager";
import SchedulerToggleManager from "@/pages/admin/SchedulerToggleManager";
import AuthKeyManager from "@/pages/AuthKeyManager";
import CurrentBalance from "@/pages/CurrentBalance";
import PositionManager from "@/features/position-manager/PositionManager";
import PositionOpen from "@/features/position-open/PositionOpen";
import IndicatorComparison from "@/pages/admin/IndicatorComparison";

const ALL_COMPONENTS: Record<string, () => React.ReactNode> = {
  "admin-key-manager": () => <AdminKeyManager />,
  "scheduler-toggle-manager": () => <SchedulerToggleManager />,
  "indicator-comparison": () => <IndicatorComparison />,
  "auth-key-manager": () => <AuthKeyManager />,
  "current-balance": () => <CurrentBalance />,
  "position-manager": () => <PositionManager />,
  "position-open": () => <PositionOpen />,
};

const Layout = () => {
  const [tabs, setTabs] = useState<{ id: string; label: string }[]>([]);
  const [activeTab, setActiveTab] = useState<string | null>(null);
  const [mountedTabs, setMountedTabs] = useState<
    Record<string, React.ReactNode>
  >({});

  const openTab = (id: string, label: string) => {
    setTabs((prev) =>
      prev.some((tab) => tab.id === id) ? prev : [...prev, { id, label }]
    );
    setActiveTab(id);

    setMountedTabs((prev) => {
      if (prev[id]) return prev;
      const create = ALL_COMPONENTS[id];
      return create ? { ...prev, [id]: create() } : prev;
    });
  };

  const closeTab = (id: string) => {
    setTabs((prev) => prev.filter((tab) => tab.id !== id));
    setMountedTabs((prev) => {
      const copy = { ...prev };
      delete copy[id];
      return copy;
    });
    if (activeTab === id) {
      const remain = tabs.filter((tab) => tab.id !== id);
      setActiveTab(remain.length > 0 ? remain[remain.length - 1].id : null);
    }
  };

  return (
    <div className="flex h-screen bg-gray-900 text-white">
      <Sidebar openTab={openTab} />

      <div className="flex-1 flex flex-col">
        {/* 탭 영역 */}
        <nav className="flex bg-gray-800 px-4 py-2 space-x-2 border-b border-gray-700">
          {tabs.map((tab) => (
            <div key={tab.id} className="flex items-center bg-gray-700 rounded">
              <button
                onClick={() => setActiveTab(tab.id)}
                className={`px-4 py-1 font-semibold ${
                  activeTab === tab.id
                    ? "text-yellow-300 border-b-2 border-yellow-400"
                    : "text-gray-300 hover:bg-gray-600"
                }`}
              >
                {tab.label}
              </button>
              <button
                onClick={() => closeTab(tab.id)}
                className="ml-1 px-2 text-gray-400 hover:text-red-500"
              >
                ✕
              </button>
            </div>
          ))}
        </nav>

        {/* 콘텐츠 영역 */}
        <main className="flex-1 p-4 overflow-y-auto">
          {Object.entries(mountedTabs).map(([id, node]) => (
            <div
              key={id}
              style={{ display: id === activeTab ? "block" : "none" }}
            >
              {node}
            </div>
          ))}
          {!activeTab && (
            <div className="text-gray-500">
              왼쪽 메뉴를 클릭해 탭을 열어보세요.
            </div>
          )}
        </main>
      </div>
    </div>
  );
};

export default Layout;
