import {createContext ,useCallback,useContext, useEffect, useState, type ReactNode } from "react";
import { http } from "../lib/http";
import type { HomeIssuesData } from "../types";

type IssueContextType = {
    data: HomeIssuesData | null;
    loading: boolean;
  error: string | null;
    refetch: () => void;
}

const IssueContext = createContext<IssueContextType | null>(null);

export function IssueProvider({ children }: { children: ReactNode }) {
    const [data, setData] = useState<HomeIssuesData | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const fetchIssues = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await http.get("/dashboard/home");
      setData(res.data);
    } catch (err: unknown) {
      if (typeof err === "object" && err !== null && "message" in err) {
        setError(String((err as { message?: string }).message ?? "Failed to load issues"));
      } else {
        setError("Failed to load issues");
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchIssues();
  }, [fetchIssues]);

    return (
        <IssueContext.Provider value={{ data, loading, error, refetch: fetchIssues }}>
        {children}
        </IssueContext.Provider>
    );
    }

export function useIssues() {
  const context = useContext(IssueContext);

  if (!context) {
    throw new Error("useIssues must be used inside IssueProvider");
  }

  return context;
}