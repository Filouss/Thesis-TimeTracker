import { Navigate } from "react-router-dom";
import { useAuth } from "./AuthProvider";

export function RootRedirect() {
    const { user, loading } = useAuth();

    if (loading) return <div className="loading-screen">Loading...</div>;

    if (user) {
        return <Navigate to="/home" replace />;
    }
    
    return <Navigate to="/landing" replace />;
}