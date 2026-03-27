import { Bar, BarChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { formatTrackedTime } from "../../lib/utils";

type Data = {
    data?: {weekDay: string, secondsTracked: number}[],
}


export default function DailyTimeGraph({data} : Data) {
    return (
        <div className="daily-time-graph-wrapper">
            <h3>Daily Time Tracked</h3>
            <div className="widget">
                <ResponsiveContainer width="100%" height="100%" className={"weeklyData"}>
                    <BarChart data={data}>
                    <XAxis dataKey="weekDay" stroke="rgb(247, 247, 247)"/>
                    <YAxis stroke="rgb(247, 247, 247)" tickFormatter={formatTrackedTime} />
                    <Tooltip formatter={(value: any) => [formatTrackedTime(value || 0), "Time"]} contentStyle={{ 
                        backgroundColor: '#1e1e1e', 
                        border: '1px solid #444', 
                        borderRadius: '8px',
                        color: '#fff' 
                        }}
                        itemStyle={{ color: '#fff' }}/>
                    <Bar dataKey="secondsTracked" fill="rgb(106, 163, 173)"/>
                    </BarChart>
                </ResponsiveContainer>
            </div>
        </div>
    );
}
