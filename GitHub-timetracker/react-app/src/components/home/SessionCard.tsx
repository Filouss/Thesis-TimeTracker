import { BaseCard } from "./BaseCard";
import { formatTrackedTime } from "../../lib/utils";
import LoadingButton from "../button/LoadingButton";
import { useState } from "react";
import type { ApiSession } from "../../types";

type SessionCardProps = {
  session: ApiSession;
  onSync?: (sessionId: number, notes: string, synced: boolean) => void;
  onEdit?: (sessionId: number) => void;
};

export function SessionCard({
  session,
  onSync,
  onEdit,
}: SessionCardProps) {
  const issue = session.issue;
  const [isSyncing, setIsSyncing] = useState(false);
  const sortedLabels = [...(issue.labels || [])].sort((left, right) =>
    left.name.localeCompare(right.name, undefined, { sensitivity: "base" })
  );

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
            {`Session time: ` + formatTrackedTime(session.trackedSeconds)}
          </div>
        </>
      }
      footer={
        <>
        <div className="labels-row">
            {(() => {
              const maxChars = 25;
              let currentChars = 0;
              const displayedLabels = [];
              let remaining = 0;

              for (const label of sortedLabels) {
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
            View & Edit
          </button>

          <LoadingButton 
            className="tile-btn" 
            isLoading={isSyncing}
            onClick={async () => {
              if (onSync) {
                setIsSyncing(true);
                try {
                  await onSync(session.id, session.notes, session.synced);
                } finally {
                  setIsSyncing(false);
                }
              }
            }}>
            Sync to GitHub
          </LoadingButton>
          </div>
          
        </>
      }
    />
  );
}