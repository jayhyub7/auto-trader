import React from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";

const PrivateRoute = ({ children }: { children: React.ReactNode }) => {
  const { user, loading } = useAuth();

  if (loading) return <div>로딩 중...</div>;
  if (!user) return <Navigate to="/login" />;

  return <>{children}</>;
};

export default PrivateRoute;
