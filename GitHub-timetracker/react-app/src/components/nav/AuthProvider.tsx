import React, { createContext, useContext, useEffect, useState } from "react";
import { http } from "../../lib/http";

type ApiUser = {
  id: number;
  username: string;
  [key: string]: unknown;
};

interface AuthContextType {
  user: ApiUser | null;
  loading: boolean;
  setUser: (user: ApiUser | null) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

// User provider for protected route
export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<ApiUser | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    http.get<ApiUser>('/users/me')
      .then(res => {
        setUser(res.data);
        setLoading(false);
      })
      .catch((error: any) => {
        if(error.response && error.response.status === 401) {
          setUser(null);
        } else {
          console.error("Failed to fetch user data:", error);
        }
      }).finally(() => {
        setLoading(false);
      });
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, setUser }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
};