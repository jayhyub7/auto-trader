// src/lib/axios.ts
import axios from "axios";
import { Exchange } from "../constants/Exchange";

export interface ApiKeyDto {
  exchange: Exchange;
  apiKey: string;
  secretKey: string;
  passphrase?: string;
}

const api = axios.create({
  baseURL: "/api",
  withCredentials: true, // 쿠키 기반 세션 유지
});

// ✅ 강화된 interceptor
api.interceptors.response.use(
  response => response,
  error => {
    const status = error?.response?.status;
    const path = window.location.pathname;
    console.error(error);
    // 401: 로그인 필요
    if (status === 401) {
      // 로그인 페이지가 아닌 경우에만 이동
      if (!path.startsWith("/login") && !path.includes("/oauth2/authorization")) {
        console.warn("🔒 인증되지 않음. 로그인 페이지로 이동합니다.");
        window.location.href = "/login";
      }
    }
    // 400: 입력값 오류
    else if (status === 400) {
      const message = error.response?.data?.error || "입력값이 올바르지 않습니다.";
      alert(message); // TODO: toast로 교체 가능
    }
    // 500 등 기타 서버 오류 처리도 원한다면 여기에 추가 가능

    return Promise.reject(error);
  }
);

export default api;
