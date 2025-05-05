import React from "react";

interface PositionControlsProps {
  onAdd: () => void;
  onDelete: () => void;
  onSave: () => void;
  showConditionBox: boolean; // 🔥 추가
}

const PositionControls: React.FC<PositionControlsProps> = ({
  onAdd,
  onDelete,
  onSave,
  showConditionBox, // 🔥 추가
}) => {
  if (showConditionBox) return null; // 🔥 조건 박스 보이면 숨김

  return (
    <div className="flex justify-end gap-2 mb-4">
      <button
        onClick={onDelete}
        className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
      >
        선택 삭제
      </button>
      <button
        onClick={onAdd}
        className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700"
      >
        포지션 추가
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
