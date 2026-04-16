export function formatTrackedTime(totalSeconds: number): string {
  const totalMinutes = Math.round(totalSeconds / 60);
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;

  if (totalSeconds === 0) return "0m";
  if (totalSeconds > 0 && totalMinutes === 0) return `<1m`;
  if (hours === 0) return `${minutes}m`;
  return `${hours}h ${minutes}m`;
}

