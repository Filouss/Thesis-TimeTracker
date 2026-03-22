import { useCallback, useEffect, useState } from "react";
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
}


type HomeIssuesData = {
  assigned: ApiIssue[];
  pinned: ApiIssue[];
  tracking: ApiIssue | null;
  toSync: ApiSession[];
  trackingPaused: boolean;
};


export  function useGetIssues() {
    const [data, setData] = useState<HomeIssuesData | null>(null);

    const fetchData = useCallback(async () => {
    try {
      const res = await http.get("/home");
      setData(res.data);
    } catch (err) {
      console.error(err);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  return {
    data: data ?? {
      assigned: [],
      pinned: [],
      tracking: null,
      toSync: [],
      trackingPaused: false
  },
    refetch: fetchData
  }
}
