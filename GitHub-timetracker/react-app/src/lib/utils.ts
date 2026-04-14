export function formatTrackedTime(totalSeconds: number): string {
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);

  if (hours === 0 && minutes === 0) return `<1m`;
  if (hours === 0) return `${minutes}m`;
  return `${hours}h ${minutes}m`;
}

