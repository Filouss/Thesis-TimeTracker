import { useEffect, useState } from "react";
import { formatTrackedTime } from "../../lib/utils";

interface ActiveTimerProps {
    isActive: boolean;
    accumulatedSeconds: number | null; 
    currentTrackStartTime: string | Date | null; //resume or start time tracking
}

export const ActiveTimer = ({ isActive, accumulatedSeconds, currentTrackStartTime }: ActiveTimerProps) => {
    const [displayTime, setDisplayTime] = useState<string>(
        formatTrackedTime(accumulatedSeconds || 0)
    );

    useEffect(() => {
        if (!isActive || !currentTrackStartTime) {
            setDisplayTime(formatTrackedTime(accumulatedSeconds || 0));
            return;
        }

        const startTimestamp = new Date(currentTrackStartTime).getTime();

        const updateTimer = () => {
            const now = Date.now();
            
            const currentTrackedMs = Math.max(0, now - startTimestamp); 
            
            // Total Time = Past recorded time + Current live tracked time
            const totalSeconds = (accumulatedSeconds || 0) + Math.floor(currentTrackedMs / 1000);
            
            setDisplayTime(formatTrackedTime(totalSeconds));
        };

        updateTimer();

        // Start the visual ticker
        const intervalId = setInterval(updateTimer, 1000);

        return () => clearInterval(intervalId);
    }, [isActive, accumulatedSeconds, currentTrackStartTime]);

    return (
        <div className="text-xl font-mono font-bold">
            Current session time: {displayTime}
        </div>
    );
};