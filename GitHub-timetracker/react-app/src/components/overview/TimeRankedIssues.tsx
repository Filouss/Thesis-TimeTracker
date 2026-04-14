import { Pie, PieChart, ResponsiveContainer, Tooltip } from "recharts";
import { formatTrackedTime } from "../../lib/utils";

type Data = {
    data?: { title: string, number: number, timeTracked: number }[]
}

export default function TimeRankedIssues({ data }: Data) {
    const COLORS = ["#0F7173", "#FFB516", "#E94F37", "#4A6FA5", "#4aa577ff", "#0f473eff"];
    const chartData = data?.map((item, index) => ({
        ...item,
        fill: COLORS[index % COLORS.length]
    }));
    console.log(data);
    return (
        <div className="ranked-wrapper">
            <h3 className="ranked-title">
                Most time consuming issues
            </h3>
            {data?.length ? (
                <div className="ranked-content">
                <div className="ranked-list">
                    {chartData?.map((element, idx) => (
                        <div key={element.number} className="ranked-item">
                            <span 
                                className="ranked-bullet" 
                                style={{ backgroundColor: element.fill }} 
                            />
                            <span className="ranked-index">{idx + 1}.</span>
                            <div className="ranked-title-group">
                                <span className="ranked-title-text">{element.title}</span>
                                <span className="issue-num-span">#{element.number}</span>
                            </div>
                        </div>
                    ))}
                </div>
                <div className="ranked-piechart">
                    <ResponsiveContainer width="100%" height={220}>
                        <PieChart>
                            <Pie
                                data={chartData}
                                dataKey="timeTracked"
                                nameKey="title"
                                cx="50%"
                                cy="50%"
                                outerRadius={100}
                                innerRadius={80}
                                paddingAngle={4}
                                stroke="none">
                            </Pie>
                            <Tooltip formatter={(value: any) => `${formatTrackedTime(value || 0)}`} contentStyle={{
                                backgroundColor: '#1e1e1e',
                                border: '1px solid #444',
                                borderRadius: '8px',
                                color: '#fff'
                            }}
                                itemStyle={{ color: '#fff' }} />
                        </PieChart>
                    </ResponsiveContainer>
                </div>
            </div>
            ) : (
                <p>No data yet.</p>
            )}
        </div>
    )
}