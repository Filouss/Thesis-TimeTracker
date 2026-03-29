import "../../styles/issueDetail.css";
import { formatTrackedTime } from '../../lib/utils';
import { FaPause, FaPlay } from "react-icons/fa6";
import { FaRegPlayCircle } from "react-icons/fa";
import { IoIosArrowBack } from "react-icons/io";
import { useNavigate } from "react-router-dom";
import { useState } from "react";
import { ConfirmModal } from "../modals/ConfirmModal";

type ApiIssue = {
  id: number;
  repository_url: string;
  title: string;
  state: string;
  labels: { id: number, name: string, color: string }[];
  timeTracked?: number;
  html_url: string;
  user: { id: number, login: string };
  body: string;
  created_at: string;
  number: number;
  allSynced: boolean;
};

type IssueDetailProps = {
  issue: ApiIssue;
  onStartTracking: (issueNumber: number, repository_url: string) => void;
  onPauseTracking: () => void;
  onResumeTracking: () => void;
  onOpenGithub: (url: string) => void;
  isCurrent?: boolean;
  isPaused?: boolean;
  isTracking: boolean
}

export default function IssueDetail({
  issue,
  onStartTracking,
  onPauseTracking,
  onResumeTracking,
  onOpenGithub,
  isCurrent,
  isPaused,
  isTracking
}: IssueDetailProps) {
  const [showConfirm, setShowConfirm] = useState(false);
  const [confirmMessageBody, setConfirmMessageBody] = useState("");
  const [confirmTitle, setConfirmTitle] = useState("");
  const [confirmAction, setConfirmAction] = useState<((notes?: string) => void) | null>(null);
  const repoName = issue.repository_url?.split("/").pop() || "repository";
  const navigate = useNavigate();
  const formatTime = formatTrackedTime(issue.timeTracked || 0);

  let trackingButton;


  function handleStartTracking(issueNumber: number, repository_url: string) {
    if (isTracking) {
      setConfirmTitle("Are you sure you want start a new session?");
      setConfirmMessageBody("Your currently active session will be ended and moved to Ready to Sync");
      setConfirmAction(() => () => {
        onStartTracking(issueNumber, repository_url);
      });
      setShowConfirm(true)
    } else {
      onStartTracking(issueNumber, repository_url);
    }
  }

  if (!isCurrent) {
    trackingButton = (
      <button
        className="tile-btn"
        id="tracking"
        onClick={() => handleStartTracking(issue.number, issue.repository_url)}
      >
        Start tracking <FaPlay />
      </button>
    );
  } else if (isPaused) {
    trackingButton = (
      <button
        id="tracking"
        className="tile-btn"
        onClick={() => onResumeTracking()}
        style={{ backgroundColor: '#28a745', color: 'white', border: 'none' }}
      >
        Resume tracking <FaRegPlayCircle />
      </button>
    );
  } else {
    trackingButton = (
        <button
        id="tracking"
          className="tile-btn"
        onClick={() => onPauseTracking()}
        style={{ backgroundColor: '#da7134ff', color: 'white', border: 'none' }}
        >
        Pause tracking <FaPause />
        </button>
    );
  }

  return (
    <div className="issue-detail-container">
      {showConfirm && (
        <ConfirmModal
          title={confirmTitle}
          body={confirmMessageBody}
          onConfirm={(notes) => {
            confirmAction?.(notes);
            setShowConfirm(false);
          }}
          onCancel={() => setShowConfirm(false)}
          noteFieldDisplayed={false}
        />
      )}
      <div className="issue-detail-content-wrapper">
        <div className="back-button-wrapper">
          <button onClick={() => navigate(-1)}><IoIosArrowBack /></button>
        </div>
        {/* Main Issue Card */}
        <div className="issue-card">
          <div className="card-header">
            <div className="title-section">
              <h1>{issue.title} <span className="issue-number"> #{issue.number}</span></h1>
              <p className="repo-text">{repoName}</p>
            </div>
            <div className={`status-badge-${issue.state}`}>
              {issue.state}
            </div>
          </div>

          <div className="description-box">
            <h4>Description</h4>
            <p>{issue.body || "No description provided."}</p>
          </div>

          <div className="labels-row">
            {issue.labels.map(label => (
              <span
                key={label.id}
                className="label-pill"
                style={{ borderLeft: `3px solid #${label.color}` }}
              >
                {label.name}
              </span>
            ))}
          </div>

          <div className="meta-info">
            <p>Created by: <strong>{issue.user?.login || "Unknown"}</strong></p>
            <p>Created at: <strong>{new Date(issue.created_at).toLocaleDateString()}</strong></p>

            <button
              className="github-btn"
              onClick={() => onOpenGithub(issue.html_url)}
            >
              Open in GitHub
            </button>
          </div>
        </div>

        {/* Footer Actions */}
        <div className="footer-actions">
          <div className="footer-left">
            <button className="history-btn" onClick={() => navigate(`/issues/${issue.id}/history`)}
            >Issue work history</button>
            <div className={`sync-status-${issue.allSynced}`}>
              {/* TODO: different colors */}
              {issue.allSynced ? "Synchronized ✓" : "Unsynced changes ⚠️"}
            </div>
          </div>

          <div className="footer-right">
            <div className="time-display">
              Tracked time: <span>{formatTime}</span>
            </div>
            {trackingButton}
          </div>
        </div>
      </div>
    </div>
  );
}
