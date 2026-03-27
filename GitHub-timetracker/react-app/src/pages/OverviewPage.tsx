import { useEffect } from 'react';
import DailyTimeGraph from '../components/overview/DailyTimeGraph';
import ByLabelGraph from '../components/overview/ByLabelGraph';
import TrackedTimeData from '../components/overview/TrackedTimeData';
import TimeRankedIssues from '../components/overview/TimeRankedIssues';
import { useGetOverviewData } from '../hooks/useGetOverviewData';
import "../styles/OverviewPage.css";

export default function OverViewPage() {
    const{statData, graphData, fetchGraphs, fetchStats} = useGetOverviewData();

    useEffect(() => {
        fetchStats("ThisWeek");
        fetchGraphs();
    }, []);

    console.log("stats", statData);
    console.log("graphs", graphData);
    return (
        <div className="overview-page">
            <div className="overview-top">
                <DailyTimeGraph data={graphData?.dailyData}/>
                <TrackedTimeData statData={statData} onIntervalChange={(val) => fetchStats(val)}/>
            </div>
            <div className="overview-bottom">
                <ByLabelGraph data={graphData?.timeByLabel}/>
                <TimeRankedIssues data={graphData?.rankedIssues}/>
            </div>
        </div>
    )
}