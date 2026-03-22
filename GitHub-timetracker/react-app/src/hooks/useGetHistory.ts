import { useEffect, useState } from "react";
import { http } from "../lib/http";

type ApiSession = {
  id:number;
  issue: {id: number, title: string, labels: {id:number, name: string, color: string}[], repoName: string, repoOwner: string, number: number};
  timeblocks: {start: string, end: string}[];
  paused: boolean;
  notes: string;
  trackedSeconds: number;
  synced: boolean
}


export function useGetHistory(issueId?: number) {
    const [sessions, setSessions] = useState<ApiSession[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
    const fetchHistory = async () => {
      setLoading(true);
      try {
        const endpoint = issueId ? `/session/issue/${issueId}` : "/session";
        const res = await http.get(endpoint);
        setSessions(res.data);
      } catch (error) {
        console.error("Failed to fetch sessions", error);
      } finally {
        setLoading(false);
      }
    };

    fetchHistory();
  }, [issueId]);

  return { sessions, loading };
}