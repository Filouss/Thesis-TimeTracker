import {createContext ,useCallback,useContext, useEffect, useState, type ReactNode } from "react";
import { http } from "../lib/http";

type ApiIssue = {
  id: number;
  repository_url: string;
  title: string;
  state: string;
  labels: {id:number, name: string, color: string}[];
  timeTracked?: number;
  html_url: string;
  user: {id: number, login: string};
  body: string;
  created_at: string;
  number: number;
  allSynced: boolean;
};

type ApiSession = {
  id:number;
  issue: {id: number, title: string, labels: {id:number, name: string, color: string}[], repoName: string, repoOwner: string, number: number};
  timeblocks: {start: string, end: string}[];
  paused: boolean;
  notes: string;
  trackedSeconds: number;
  synced: boolean;
}


type HomeIssuesData = {
  assigned: ApiIssue[];
  pinned: ApiIssue[];
  tracking: ApiIssue | null;
  toSync: ApiSession[];
  trackingPaused: boolean;
};

type IssueContextType = {
    data: HomeIssuesData | null;
    // setData: (data: HomeIssuesData) => void;
    loading: boolean;
    refetch: () => void;
}

const IssueContext = createContext<IssueContextType | null>(null);

export function IssueProvider({ children }: { children: ReactNode }) {
    const [data, setData] = useState<HomeIssuesData | null>(null);
    const [loading, setLoading] = useState(true);

    const fetchIssues = useCallback(async () => {
    setLoading(true);
    try {
      console.log("DATA FETCH - tenhle saha na gh");
      const res = await http.get("/dashboard/home");
      setData(res.data);
    } catch (err) {
      console.error("Failed to fetch issues", err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchIssues();
  }, [fetchIssues]);

    return (
        <IssueContext.Provider value={{ data, loading, refetch: fetchIssues }}>
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