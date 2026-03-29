import { BaseCard } from "./BaseCard";
import { formatTrackedTime } from "../../lib/utils";

type Session = {
  id: number;
  issue: { id: number, title: string, labels: { id: number, name: string, color: string }[], repoName: string, number: number };
  timeblocks: { start: string, end: string }[];
  paused: boolean;
  notes: string;
  trackedSeconds: number;
  synced: boolean
};

type SessionCardProps = {
  session: Session;
  onSync?: (sessionId: number, notes: string, synced: boolean) => void;
  onEdit?: (sessionId: number) => void;
};

export function SessionCard({
  session,
  onSync,
  onEdit,
}: SessionCardProps) {
  const issue = session.issue;

  return (
    <BaseCard
      header={
        <>
          <div className="header-left">
            <div className="issue-info">
              <span className="repo-name">{issue.repoName}</span>
              <span className="issue-number"> #{issue.number}</span>
            </div>
          </div>
        </>
      }
      meta={
        <>
          
          <div className="issue-title">{issue.title}</div>
          <div className="tracked-time">
            {`Tracked time: ` + formatTrackedTime(session.trackedSeconds)}
          </div>
        </>
      }
      footer={
        <>
        <div className="labels-row">
            {(() => {
              const maxChars = 30;
              let currentChars = 0;
              const displayedLabels = [];
              let remaining = 0;

              for (const label of issue.labels || []) {
                if (displayedLabels.length === 0 || currentChars + label.name.length <= maxChars) {
                  displayedLabels.push(label);
                  currentChars += label.name.length;
                } else {
                  remaining++;
                }
              }

              return (
                <>
                  {displayedLabels.map((label) => (
                    <span
                      key={label.id}
                      className="label-chip"
                      style={label.color ? { borderLeft: `3px solid #${label.color}` } : undefined}
                    >
                      {label.name}
                    </span>
                  ))}
                  {remaining > 0 && (
                    <span className="label-chip more-labels">
                      +{remaining} more
                    </span>
                  )}
                </>
              );
            })()}
          </div>
          <div className="card-btns">
            <button className="tile-btn" onClick={() => onEdit?.(session.id)}>
            Edit
          </button>

          <button className="tile-btn" onClick={() => onSync?.(session.id, session.notes, session.synced)}>
            Sync to GitHub
          </button>
          </div>
          
        </>
      }
    />
  );
}