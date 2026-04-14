import { useEffect, useState } from "react";
import { formatTrackedTime } from "../../lib/utils";

interface ActiveTimerProps {
    startTime: string | Date;
}

export const ActiveTimer = ({ startTime }: ActiveTimerProps) => {
    const [elapsedTime, setElapsedTime] = useState<number>(0);

    useEffect(() => {
        if (!startTime) return;
        console.log(startTime);

        const startTimestamp = new Date(startTime).getTime();

        const updateTimer = () => {
            const now = Date.now();
            setElapsedTime(now - startTimestamp);
        };
        
        updateTimer();

        
        const updateInterval = setInterval(updateTimer, 1000);

        //interval cleanup on component unmount
        return () => clearInterval(updateInterval);
        
    }, [startTime]);

    return (
        <div className="text-xl font-mono font-bold">
            Current session time: {formatTrackedTime(elapsedTime/1000)}
        </div>
    );
};