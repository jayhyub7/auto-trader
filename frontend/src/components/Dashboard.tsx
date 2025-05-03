import { useState } from "react";

const Dashboard = () => {
  const [activeTab, setActiveTab] = useState("home");

  const renderContent = () => {
    switch (activeTab) {
      case "home":
        return <div className="text-white">🏠 홈 콘텐츠</div>;
      case "trade":
        return <div className="text-white">📊 매매 분석 콘텐츠</div>;
      case "history":
        return <div className="text-white">📁 거래 내역 콘텐츠</div>;
      default:
        return null;
    }
  };

  return (
    <div className="flex min-h-screen bg-gray-900">
      {/* Sidebar */}
      <aside className="w-64 bg-gray-800 text-white p-4 space-y-4">
        <h2 className="text-xl font-bold mb-4">AutoTrader</h2>
        <div>
          <p className="text-gray-400 text-sm mb-2">📁 메뉴</p>
          <ul className="space-y-2">
            <li className="hover:text-yellow-300 cursor-pointer">대시보드</li>
            <li className="hover:text-yellow-300 cursor-pointer">계좌 관리</li>
            <li className="hover:text-yellow-300 cursor-pointer">전략 설정</li>
            <li className="hover:text-yellow-300 cursor-pointer">리포트</li>
          </ul>
        </div>
      </aside>

      {/* Main Area */}
      <div className="flex-1 flex flex-col">
        {/* Header */}
        <header className="bg-gray-800 text-white px-6 py-3 border-b border-gray-700">
          <div className="text-right">👤 로그인한 사용자 정보</div>
        </header>

        {/* Tabs */}
        <nav className="bg-gray-800 px-6 py-3 border-b border-gray-700">
          <div className="flex space-x-4">
            {["home", "trade", "history"].map((tab) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={`px-4 py-2 rounded-md font-semibold ${
                  activeTab === tab
                    ? "bg-gray-700 text-yellow-300 border border-yellow-400 shadow-inner"
                    : "bg-gray-700 text-gray-300 hover:bg-gray-600"
                }`}
              >
                {tab === "home"
                  ? "홈"
                  : tab === "trade"
                  ? "매매 분석"
                  : "거래 내역"}
              </button>
            ))}
          </div>
        </nav>

        {/* Content */}
        <main className="flex-1 bg-gray-900 p-6">{renderContent()}</main>
      </div>
    </div>
  );
};

export default Dashboard;
