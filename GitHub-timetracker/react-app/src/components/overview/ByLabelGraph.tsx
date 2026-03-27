import { Bar, BarChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { formatTrackedTime } from "../../lib/utils";


type Data = {
    data?: {name: string, color: string, secondsTracked: number}[]
}


export default function ByLabelGraph({data} : Data) {
    const chartData = data?.map(item => ({ ...item, fill: `#${item.color}` }));
    console.log(chartData)
    
    return (
        <div className="by-label-graph-wrapper">
            <h3>Most time tracked for issue labels</h3>
            <div className="widget">
                <ResponsiveContainer width="100%" height={300} className={"labelData"}>
                    <BarChart data={chartData}>
                    <XAxis dataKey="name" stroke="rgb(247, 247, 247)"/>
                    <YAxis stroke="rgb(247, 247, 247)" tickFormatter={formatTrackedTime} />
                    <Tooltip formatter={(value: any) => [formatTrackedTime(value || 0), "Time"]} contentStyle={{ 
                        backgroundColor: '#1e1e1e', 
                        border: '1px solid #444', 
                        borderRadius: '8px',
                        color: '#fff' 
                        }}
                        itemStyle={{ color: '#fff' }}/>
                    <Bar dataKey="secondsTracked" />
                    </BarChart>
                </ResponsiveContainer>
            </div>
        </div>
    );
}
