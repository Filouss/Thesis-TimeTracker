import { useState } from "react"
import { http } from "../lib/http"

type GraphData = {
    dailyData: {weekDay: string, secondsTracked: number}[],
    rankedIssues: {title: string, number: number, timeTracked: number}[],
    timeByLabel: {name: string, color: string, secondsTracked: number}[]
}

type Stats = {
    totalTimeTracked: number,
    workingTimeRatio: number
}


export function useGetOverviewData() {
    const [graphData, setGraphData] = useState<GraphData | null>(null);
    const [statData, setStatData] = useState<Stats | null>(null);
    const [loading, setLoading] = useState(true);

    async function fetchGraphs() {
        setLoading(true);
        try {
            const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
            const res = await http.get<GraphData>(`/dashboard/overview/graphs?zoneId=${encodeURIComponent(timeZone)}`);
            setGraphData(res.data);
        } catch (error) {
            console.error("Failed to fetch graphs", error);
        } finally {
            setLoading(false);
        }
    }

    async function fetchStats(interval: string) {
        setLoading(true);
        try {
            const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
            const res = await http.get<Stats>(`/dashboard/overview/stats?zoneId=${encodeURIComponent(timeZone)}&interval=${interval}`);
            setStatData(res.data);
        } catch (error) {
            console.error("Failed to fetch stats", error);
        } finally {
            setLoading(false);
        }
    }

    return {
        graphData,
        statData,
        loading,
        fetchGraphs,
        fetchStats
    };
}



