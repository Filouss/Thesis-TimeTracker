import { useEffect } from 'react';
import DailyTimeGraph from '../components/overview/DailyTimeGraph';
import ByLabelGraph from '../components/overview/ByLabelGraph';
import TrackedTimeData from '../components/overview/TrackedTimeData';
import TimeRankedIssues from '../components/overview/TimeRankedIssues';
import { useGetOverviewData } from '../hooks/useGetOverviewData';

export default function OverViewPage() {
    const{statData, graphData, fetchGraphs, fetchStats} = useGetOverviewData();

    useEffect(() => {
        fetchStats();
        fetchGraphs();
    }, []);

    console.log("stats", statData);
    console.log("graphs", graphData);
    return (
        <div className="overview-page">
            <div className="overview-top">
                <DailyTimeGraph />
                <TrackedTimeData />
            </div>
            <div className="overview-bottom">
                <ByLabelGraph />
                <TimeRankedIssues />
            </div>
        </div>
    )
}