import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";

const OAuthRedirect = () => {
  const navigate = useNavigate();

  useEffect(() => {
    axios
      .get("/api/user/me", { withCredentials: true })
      .then((res) => {
        console.log("로그인 성공!", res.data);
        // 사용자 정보 저장 후 메인 페이지로 이동
        localStorage.setItem("user", JSON.stringify(res.data));
        navigate("/"); // 또는 /dashboard 등
      })
      .catch((err) => {
        
        if (err.response && err.response.status === 401) {
          alert("로그인이 필요합니다");
        } else {
          alert("API 오류 발생");
        }
        navigate("/login");
      });
  }, []);

  return <div className="text-center mt-20">로그인 중입니다...</div>;
};

export default OAuthRedirect;
