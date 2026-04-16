export type ApiIssue = {
  id: number;
  repository_url: string;
  title: string;
  state: string;
  labels: { id: number; name: string; color: string }[];
  timeTracked?: number;
  html_url: string;
  user: { id: number; login: string };
  body: string;
  created_at: string;
  number: number;
  allSynced: boolean;
};

export type TimeBlock = {
  start: string;
  end: string;
};

export type ApiSession = {
  id: number;
  issue: {
    id: number;
    title: string;
    labels: { id: number; name: string; color: string }[];
    repoName: string;
    repoOwner: string;
    number: number;
  };
  timeblocks: TimeBlock[];
  paused: boolean;
  notes: string;
  trackedSeconds: number;
  synced: boolean;
};

export type HomeIssuesData = {
  assigned: ApiIssue[];
  pinned: ApiIssue[];
  tracking: ApiIssue | null;
  toSync: ApiSession[];
  trackingPaused: boolean;
  currLatestTBStartTime: string | Date;
  currFinishedTBDuration: number | null;
};
