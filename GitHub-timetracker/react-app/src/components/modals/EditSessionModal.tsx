import { useState, useEffect } from "react";

type TimeBlock = {
  start: string;
  end: string;
};

type Session = {
  id: number;
  issue: { id: number, title: string, labels: { id: number, name: string, color: string }[], repoName: string, repoOwner: string, number: number };
  timeblocks: {start: string, end: string}[];
  paused: boolean;
  notes: string;
  trackedSeconds: number;
  synced: boolean
};

type EditSessionModalProps = {
  session: Session | null;
  onSave: (sessionId: number, timeblocks: TimeBlock[], notes: string, synced: boolean ,issueUrl?: string) => void;
  onCancel: () => void;
  error?: string;
};

export function EditSessionModal({ session, onSave, onCancel, error }: EditSessionModalProps) {
  const [timeblocks, setTimeblocks] = useState<TimeBlock[]>([]);
  const [notes, setNotes] = useState("");
  const [issueUrl, setIssueUrl] = useState("");
  const [parsedIssue, setParsedIssue] = useState<{ owner: string; repo: string; issueNumber: number } | null>(null);
  const [urlError, setUrlError] = useState("");

  useEffect(() => {
    if (session) {
      const formattedTB = session.timeblocks.map(tb => ({
        start: formatDateTime(tb.start),
        end: tb.end ? formatDateTime(tb.end) : ""       
      }));
      setTimeblocks(formattedTB);
      setNotes(session.notes || "");
      // Initialize with current issue URL format
      const currentUrl = `https://github.com/${session.issue.repoOwner}/${session.issue.repoName}/issues/${session.issue.number}`;
      setIssueUrl(currentUrl);
      setParsedIssue({
        owner: session.issue.repoOwner,
        repo: session.issue.repoName,
        issueNumber: session.issue.number
      });
    }
  }, [session]);

  if (!session) return null;

  function updateTimeblock(index: number, field: 'start' | 'end', value: string) {
    const updated = [...timeblocks];
    updated[index] = { ...updated[index], [field]: value };
    setTimeblocks(updated);
  };

  function addTimeblock() {
    setTimeblocks([...timeblocks, { start: "", end: "" }]);
  }

  function parseGitHubUrl(url: string) {
    setUrlError("");
    setParsedIssue(null);

    if (!url.trim()) {
      return;
    }

    try {
      // Expected format: https://github.com/{owner}/{repo}/issues/{number}
      const urlPattern = /^https:\/\/github\.com\/([^\/]+)\/([^\/]+)\/issues\/(\d+)/;
      const match = url.match(urlPattern);

      if (!match) {
        setUrlError("Invalid GitHub issue URL format. Expected: https://github.com/[owner]/[repoName]/issues/[issueNumber]");
        return;
      }

      const [, owner, repo, issueNumber] = match;
      setParsedIssue({
        owner,
        repo,
        issueNumber: parseInt(issueNumber, 10)
      });
    } catch (error) {
      setUrlError("Failed to parse GitHub URL");
    }
  };

  function formatDateTime(dateStr: string): string {
    const date = new Date(dateStr);
    // Adjust for the local timezone offset to trick toISOString into printing local time
    const offset = date.getTimezoneOffset() * 60000;
    const localDate = new Date(date.getTime() - offset);
    return localDate.toISOString().slice(0, 16);
  }

  const handleUrlChange = (url: string) => {
    setIssueUrl(url);
    parseGitHubUrl(url);
  };

  const removeTimeblock = (index: number) => {
    setTimeblocks(timeblocks.filter((_, i) => i !== index));
  };

  const isValidUrl = (url: string) => {
    const urlPattern = /^https:\/\/github\.com\/([^\/]+)\/([^\/]+)\/issues\/(\d+)/;
    return urlPattern.test(url);
  };

  const hasTimeblockError = timeblocks.some(block => {
    if (!block.start || !block.end) return true;
    return new Date(block.start) > new Date(block.end);
  });

  const isSaveDisabled = !isValidUrl(issueUrl) || hasTimeblockError;

  const handleSave = () => {
    // Convert local datetime-local strings back to absolute UTC strings for the backend
    const formattedTimeblocks = timeblocks.map((tb) => ({
      start: new Date(tb.start).toISOString(),
      end: tb.end ? new Date(tb.end).toISOString() : "",
    }));
    
    onSave(session.id, formattedTimeblocks, notes, session.synced, issueUrl || undefined);
  };

  return (
    <div className="modal-overlay" onClick={onCancel}>
      <div className="modal edit-modal" onClick={e => e.stopPropagation()}>
        <h3>Edit Session</h3>

        <div className="issue-section">
          <h4>GitHub Issue</h4>
          <input
            type="url"
            value={issueUrl}
            onChange={(e) => handleUrlChange(e.target.value)}
            placeholder="https://github.com/owner/repo/issues/number"
            className="issue-url-input"
          />
          {urlError && <div className="error-message">{urlError}</div>}
          {error && <div className="error-message backend-error">{error}</div>}
          {parsedIssue && (
            <div className="parsed-issue-info">
              <strong>Parsed:</strong> {parsedIssue.owner}/{parsedIssue.repo}#{parsedIssue.issueNumber}
            </div>
          )}
        </div>

        <div className="timeblocks-section">
          <h4>Time Blocks</h4>
          {timeblocks.map((block, index) => {
            const isInvalid = block.start && block.end && new Date(block.start) > new Date(block.end);
            return (
              <div key={index} className="timeblock-row">
                <div className="timeblock-inputs">
                  <input
                    type="datetime-local"
                    value={block.start}
                    className={isInvalid ? 'invalid-input' : ''}
                    onChange={(e) => updateTimeblock(index, 'start', e.target.value)}
                  />
                  <span>to</span>
                  <input
                    type="datetime-local"
                    value={block.end || ""}
                    className={isInvalid ? 'invalid-input' : ''}
                    onChange={(e) => updateTimeblock(index, 'end', e.target.value)}
                  />
                </div>
                <div className="button-spacer">
                  {index > 0 && (
                    <button onClick={() => removeTimeblock(index)}>Remove</button>
                  )}
                </div>
              </div>
            );
          })}
          {hasTimeblockError && <div className="error-message">End time must be after Start time for all blocks.</div>}
          <button onClick={addTimeblock} className="tb-add">Add Time Block</button>
        </div>

        <div className="notes-section">
          <h4>Notes</h4>
          <textarea
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="Session notes..."
          />
        </div>

        <div className="modal-actions">
          <button onClick={onCancel} className="modal-cancel">Cancel</button>
          <button 
            onClick={handleSave} 
            className={`modal-confirm ${isSaveDisabled ? 'disabled' : ''}`}
            disabled={isSaveDisabled}
          >
            Save Changes
          </button>
        </div>
      </div>
    </div>
  );
}