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

// ✅ interceptor 통합
api.interceptors.response.use(
  response => response,
  error => {
    const status = error.response?.status;
    const path = window.location.pathname;

    if (status === 401) {
      // 로그인 페이지가 아닌 경우에만 이동
      if (!path.startsWith("/login") && !path.includes("/oauth2/authorization")) {
        window.location.href = "/login";
      }
    } else if (status === 400) {
      const message = error.response?.data?.error || "입력값이 올바르지 않습니다.";
      alert(message); // TODO: toast로 교체 가능
    }

    return Promise.reject(error);
  }
);

export default api;
