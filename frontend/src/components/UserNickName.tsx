import React, { useEffect, useState } from "react";

const UserNickName = () => {
  const [name, setName] = useState("");
  const [nickName, setNickName] = useState("");
  const [inputNick, setInputNick] = useState("");
  const [editing, setEditing] = useState(false);

  useEffect(() => {
    const fetchUser = async () => {
      const res = await fetch("/api/user/me", { credentials: "include" });
      if (res.ok) {
        const data = await res.json();
        setName(data.name || "사용자");
        setNickName(data.nickName || "");
        setInputNick(data.nickName || "");
      }
    };
    fetchUser();
  }, []);

  const handleSave = async () => {
    const res = await fetch("/api/user/me/nickname", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify({ nickName: inputNick }),
    });

    if (res.ok) {
      const data = await res.json();
      setNickName(data.nickName || "");
      setEditing(false);
    }
  };

  return (
    <div className="bg-gray-800 p-4 rounded shadow text-sm mb-4">
      <div className="mb-2 font-semibold text-yellow-300">📝 닉네임</div>
      {editing ? (
        <div className="space-x-2">
          <input
            type="text"
            value={inputNick}
            onChange={(e) => setInputNick(e.target.value)}
            className="px-2 py-1 rounded text-black"
            placeholder="닉네임 입력"
          />
          <button
            onClick={handleSave}
            className="bg-green-600 px-3 py-1 rounded text-white"
          >
            저장
          </button>
          <button
            onClick={() => setEditing(false)}
            className="bg-gray-600 px-2 py-1 rounded text-white"
          >
            취소
          </button>
        </div>
      ) : (
        <div className="flex justify-between items-center">
          <span className="text-gray-300">
            {nickName || name}
          </span>
          <button
            onClick={() => setEditing(true)}
            className="bg-blue-600 px-3 py-1 rounded text-white text-xs"
          >
            수정
          </button>
        </div>
      )}
    </div>
  );
};

export default UserNickName;
