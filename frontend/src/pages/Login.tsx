import React, { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import api from "../lib/axios";

const Login = () => {
  const navigate = useNavigate();

  useEffect(() => {
    const url = new URL(window.location.href);
    const force = url.searchParams.get("force");

    if (force !== "true") {
      api.get("/keys")
        .then(() => {
          navigate("/");
        })
        .catch((err) => {
          console.debug("자동 로그인 실패:", err?.response?.status); // 디버깅용
        });
    }
  }, [navigate]);

  const handleLogin = () => {
    const baseUrl = "http://localhost:8080/oauth2/authorization/";
    window.location.href = `${baseUrl}google`;
  };

  return (
    <div className="fixed inset-0 flex items-center justify-center bg-gray-900">
      <div className="w-full max-w-sm bg-gray-800 p-8 rounded-xl shadow-lg text-center mx-auto text-gray-200">
        <h2 className="text-lg font-semibold mb-8 leading-relaxed">
          AutoTrader에 오신 걸 환영합니다.<br />
          Google 계정으로 로그인해주세요.
        </h2>

        <button
          onClick={handleLogin}
          className="w-full bg-white text-gray-800 py-2 rounded-md font-semibold hover:shadow-md transition"
        >
          Google로 시작하기
        </button>
      </div>
    </div>
  );
};

export default Login;
