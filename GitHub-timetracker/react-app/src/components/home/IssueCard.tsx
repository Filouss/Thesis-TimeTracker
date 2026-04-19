import { TiPinOutline } from "react-icons/ti";
import { BaseCard } from "./BaseCard";
import { FaPause } from "react-icons/fa";
import { FaPlay } from "react-icons/fa";
 import { FaRegPlayCircle } from "react-icons/fa";
import { formatTrackedTime } from "../../lib/utils";
import LoadingButton from "../button/LoadingButton";
import { useState } from "react";
import type { ApiIssue } from "../../types";

type IssueCardProps = {
  issue: ApiIssue;
  onStartTracking: (issueNumber: number, repository_url: string) => void;
  onPauseTracking: () => void;
  onResumeTracking: () => void;
  onOpenGithub: (url: string) => void;
  PinBtnAction: (issueNumber: number, repository_url: string) => void;
  isCurrent: boolean;
  isPaused: boolean;
  onClick?: () => void;
};

export function IssueCard({
  issue,
  onStartTracking,
  onOpenGithub,
  PinBtnAction,
  isCurrent,
  isPaused,
  onPauseTracking,
  onResumeTracking,
  onClick
}: IssueCardProps) {
    let trackingButton;
    const [isLoading, setIsLoading] = useState(false);

    if (!isCurrent) {
        trackingButton = (
          <LoadingButton
          isLoading={isLoading}
            className="tile-btn"
            id="tracking"
            onClick={async (e) =>{ 
              e.stopPropagation();
              setIsLoading(true);
              try {
                await onStartTracking(issue.number, issue.repository_url); 
              } finally {
                setIsLoading(false); 
              }}}
          >
            Track <FaPlay />
          </LoadingButton>);
    } else if (isPaused) {
        trackingButton = (
            <LoadingButton
            isLoading={isLoading}
            id="tracking"
            className="tile-btn"
            onClick={async (e) => {
              e.stopPropagation();
              setIsLoading(true);
              try {
                await onResumeTracking();
              } finally {
                setIsLoading(false);
              }
            }}
            style={{ backgroundColor: '#28a745', color: 'white', border: 'none' }}
            >
            Resume <FaRegPlayCircle />
            </LoadingButton>
        );
    } else {
        trackingButton = (
            <LoadingButton
            isLoading={isLoading}
            id="tracking"
            className="tile-btn"
            onClick={async (e) => {
              e.stopPropagation();
              setIsLoading(true);
              try {
                await onPauseTracking();
              } finally {
                setIsLoading(false);
              }
            }}
            style={{ backgroundColor: '#da7134ff', color: 'white', border: 'none' }}
            >
            Pause <FaPause />
            </LoadingButton>
        );
    }

  return (
    <BaseCard
      header={
        <>
          <div className="header-left">
            <div className="issue-info">
              <span className="repo-name">{issue.repository_url?.split("/").pop() ?? "unknown-repo"}</span>
              <span className="issue-number"> #{issue.number}</span>
              <div className={`status-badge-${issue.state}`}>{issue.state}</div>
            </div>
          </div>

          <div className="pin-action" onClick={(e) =>{
            e.stopPropagation(); 
            PinBtnAction(issue.number, issue.repository_url)}}>
            <TiPinOutline />
          </div>
        </>
      }
      meta={
        <>
        <div className="issue-title">{issue.title}</div>
        <div className="mid-row-right">
          <div className="tracked-time">
            {`Tracked time: ` + formatTrackedTime(issue.timeTracked ?? 0)}          
          </div>
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
              // limit the amount of characters used for labels, if there are too many, show "+X more"
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
            {trackingButton}
          <button className="tile-btn" onClick={(e) => {
            e.stopPropagation();
            onOpenGithub(issue.html_url);
          }}>
            GitHub
          </button>
          </div>
          
        </>
      }
      onClick={onClick}
    />
  );
}