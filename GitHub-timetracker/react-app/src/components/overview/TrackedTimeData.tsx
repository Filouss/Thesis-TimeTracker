import { useEffect, useState } from "react";
import { formatTrackedTime } from "../../lib/utils";

type trackedDataProps = {
    statData: Data | null,
    onIntervalChange: (intervalOption: string) => void;
}

type Data = {
    totalTimeTracked: number,
    workingTimeRatio: number
}

export default function TrackedTimeData({ statData, onIntervalChange }: trackedDataProps) {
    const [interval, setInterval] = useState("ThisWeek");

    useEffect(() => {
        onIntervalChange(interval)
    }, [interval]);


    return (
        <div className="time-data-wrapper">
            <div className="data-select-wrapper">
                <select name="interval" value={interval} onChange={e => setInterval(e.target.value)} >
                    <option value="Today">Today</option>
                    <option value="Yesterday">Yesterday</option>
                    <option value="ThisWeek">This week</option>
                    <option value="LastWeek">Last week</option>
                    <option value="ThisMonth">This month</option>
                    <option value="LastMonth">Last month</option>
                    <option value="year">This year</option>
                </select>
            </div>
            <div className="total-wrapper">
                <div className="data-title">Total time tracked</div>
                <div>{formatTrackedTime(statData?.totalTimeTracked || 0)}</div>
            </div>
            <div className="work-ratio-wrapper">
                <div className="data-title">Time working ratio during session beginning and end</div>
                <div>{statData?.workingTimeRatio || "No data available"}</div>
            </div>
        </div>
    )
}