import { BrowserRouter, Routes, Route } from "react-router-dom";
import Login from "./pages/Login";
import Home from "./pages/Home"; // 메인 페이지
import OAuthRedirect from "./pages/OAuthRedirect";

//import PrivateRoute from "./components/PrivateRoute";

function App() { 
  
  return (
    <BrowserRouter>
      <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/oauth2/redirect" element={<OAuthRedirect />} />
      <Route path="/" element={<Home />} />
      </Routes>
    </BrowserRouter>
  );

}

export default App;