import React from "react";

const Login = () => {
  const handleLogin = (provider: string) => {
    const baseUrl = "http://localhost:8080/oauth2/authorization/";
    window.location.href = `${baseUrl}${provider}`;
  };

  return (
    
    <div className="fixed inset-0 flex items-center justify-center bg-gray-100">
      <div className="w-full max-w-sm bg-white p-8 rounded-xl shadow-lg text-center mx-auto">
        <h2 className="text-lg font-semibold text-gray-800 mb-8 leading-relaxed">
          당신의 비즈니스를 성장시킬<br />전문가를 만나보세요.
        </h2>

        <div className="space-y-3 mb-6">
          <button
            onClick={() => handleLogin("facebook")}
            className="w-full bg-blue-600 text-white py-2 rounded-md font-semibold hover:opacity-90 transition"
          >
            Facebook으로 시작하기
          </button>
          <button
            onClick={() => handleLogin("kakao")}
            className="w-full bg-yellow-300 text-gray-800 py-2 rounded-md font-semibold hover:opacity-90 transition"
          >
            KakaoTalk으로 시작하기
          </button>
          <button
            onClick={() => handleLogin("naver")}
            className="w-full bg-green-500 text-white py-2 rounded-md font-semibold hover:opacity-90 transition"
          >
            Naver로 시작하기
          </button>
          <button
            onClick={() => handleLogin("google")}
            className="w-full bg-white border text-gray-800 py-2 rounded-md font-semibold hover:shadow transition"
          >
            Google로 시작하기
          </button>
        </div>

        <button className="w-full bg-yellow-400 text-white font-semibold py-3 rounded-md hover:bg-yellow-500 transition">
          이메일로 시작하기
        </button>
      </div>
    </div>
  );
};

export default Login;
