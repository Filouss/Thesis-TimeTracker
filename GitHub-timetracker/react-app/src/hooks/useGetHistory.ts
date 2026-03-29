import { useCallback, useEffect, useState } from "react";
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

    const fetchHistory = useCallback(async (sortBy? : string, direction?: string) => {
      setLoading(true);
      if(!sortBy){sortBy = "createdAt"}
      if(!direction){direction = "desc"}
      console.log("refetch s temahle params " + sortBy + " " + direction)
      try {
        const endpoint = issueId ? `/session/issue/${issueId}` : "/session";
        const res = await http.get(endpoint, {params: {sortBy: sortBy, direction: direction}});
        setSessions(res.data);
      } catch (error) {
        console.error("Failed to fetch sessions", error);
      } finally {
        setLoading(false);
      }
    }, [issueId]);

    useEffect(() => {
    fetchHistory();
  }, [fetchHistory]);

  return { sessions, loading, refetch: fetchHistory };
}