import React from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../components/nav/AuthProvider";

export const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const { user, loading } = useAuth();

  if (loading) return <div className="loading-screen">Loading...</div>;

  if (!user) {
    return <Navigate to="/landing" replace />;
  }

  return children;
};