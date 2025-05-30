import React from 'react';

const Dashboard = () => {
  return (
    <div className="grid grid-cols-4 gap-4 mb-6">
      <div className="bg-white p-4 shadow rounded">누적 수익률: +5.47%</div>
      <div className="bg-white p-4 shadow rounded">평균 수익률: +1.23%</div>
      <div className="bg-white p-4 shadow rounded">총 체결 수: 48건</div>
      <div className="bg-white p-4 shadow rounded">성공률: 72%</div>
    </div>
  );
};

export default Dashboard;
