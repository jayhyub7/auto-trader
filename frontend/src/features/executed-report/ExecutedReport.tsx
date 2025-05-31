// 📁 src/features/executed-report/ExecutedReport.tsx

import React from "react";
import Dashboard from "./components/Dashboard";
import ExecutionTable from "./components/ExecutionTable";

const ExecutedReport = () => {
  return (
    <div className="p-4">
      <h2 className="text-xl font-bold mb-4">체결 리포트</h2>
      <Dashboard />
      <ExecutionTable />
    </div>
  );
};

export default ExecutedReport;
