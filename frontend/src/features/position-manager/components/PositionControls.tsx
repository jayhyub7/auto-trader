// 📄 PositionControls.tsx - 약 10~30라인

import React from "react";

interface PositionControlsProps {
  onAddLong: () => void;
  onAddShort: () => void;
  onDelete: () => void;
  onSave: () => void;
}

const PositionControls: React.FC<PositionControlsProps> = ({ onAddLong, onAddShort, onDelete, onSave }) => {
  return (
    <div className="flex justify-end gap-2 mb-4">
      <button
        onClick={onDelete}
        className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
      >
        선택 삭제
      </button>
      <button
        onClick={onAddLong}
        className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700"
      >
        📈 롱 포지션 추가
      </button>
      <button
        onClick={onAddShort}
        className="px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600"
      >
        📉 숏 포지션 추가
      </button>
      <button
        onClick={onSave}
        className="px-4 py-2 bg-yellow-500 text-white rounded hover:bg-yellow-600"
      >
        저장
      </button>
    </div>
  );
};

export default PositionControls;
