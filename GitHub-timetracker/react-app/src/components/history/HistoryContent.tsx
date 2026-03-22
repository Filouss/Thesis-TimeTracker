import { useState } from "react";
import { formatTrackedTime } from "../../lib/utils";
import "../../styles/HistoryPage.css";
import { ConfirmModal } from "../modals/ConfirmModal";
import { useCardActions } from "../../hooks/useCardActions";
import { EditSessionModal } from "../modals/EditSessionModal";
import { AiOutlineDelete } from "react-icons/ai";

type TimeBlock = {
  start: string;
  end: string;
};

type ApiSession = {
  id:number;
  issue: {id: number, title: string, labels: {id:number, name: string, color: string}[], repoName: string, repoOwner: string, number: number};
  timeblocks: TimeBlock[];
  paused: boolean;
  notes: string;
  trackedSeconds: number;
  synced: boolean
}

type HistoryContentProps = {
    sessions: ApiSession[];
}

export default function HistoryContent({ 
     sessions 
    }: HistoryContentProps) {
        const [currentPage, setCurrentPage] = useState(1);
    const itemsPerPage = 6;
    const [showConfirm, setShowConfirm] = useState(false);
    const [editingSession, setEditingSession] = useState<ApiSession | null>(null);const [editError, setEditError] = useState<string>("");
    const [showNotes, setShowNotes] = useState(false);
    const [confirmMessageBody, setConfirmMessageBody] = useState("");
    const [confirmTitle, setConfirmTitle] = useState("");
    const [confirmAction, setConfirmAction] = useState<((notes?: string) => void) | null>(null);
    const {editSession, deleteSession, syncSession} = useCardActions(() => {});
    const [expandedId, setExpandedId] = useState<number | null>(null);

    const toggleExpand = (id: number) => {
        setExpandedId(expandedId === id ? null : id);
    };
  
  const totalPages = Math.ceil(sessions.length / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const currentSessions = sessions.slice(startIndex, startIndex + itemsPerPage);

  console.log("tyhle sessny");
  console.log(sessions);
  const formatDate = (timeblocks:  {start: string, end: string}[]) => {
    if (!timeblocks || timeblocks.length === 0) return "N/A";
    return new Date(timeblocks[0].start).toLocaleDateString();
  };

  const handleSaveSession = async (sessionId: number, timeblocks: {start: string, end: string}[], notes: string, synced: boolean, issueUrl?: string) => {
        try {
            setEditError("");
            await editSession(sessionId, timeblocks, notes, synced , issueUrl);
            setEditingSession(null);
        } catch (error: any) {
            // Handle backend errors
            if (error.response?.data?.message) {
                setEditError(error.response.data.message);
            } else if (error.message) {
                setEditError(error.message);
            } else {
                setEditError("An error occurred while saving the session");
            }
        }
    };

  return (
    <div className="history-page-wrapper">
        {showConfirm && (
            <ConfirmModal
                title={confirmTitle}
                body={confirmMessageBody}
                onConfirm={() => {
                    confirmAction?.();
                    setShowConfirm(false);
                }}
                onCancel={() => setShowConfirm(false)}
                noteFieldDisplayed={showNotes}
            />
            )}

        {editingSession && (
            <EditSessionModal
                session={editingSession}
                onSave={handleSaveSession}
                onCancel={() => {
                    setEditingSession(null);
                    setEditError("");
                }}
                error={editError}
            />
        )}
      <div className="history-container">
        <h2 className="page-title">Tracked sessions history</h2>

        {/* Table Header */}
        <div className="history-table-header">
          <div className="col-title">Worked issue title</div>
          <div className="col-repo">Repository</div>
          <div className="col-time">Time tracked</div>
          <div className="col-date">Date</div>
          <div className="col-synced">Synced</div>
          <div className="col-actions"></div>
        </div>

        {/* Table Body */}
        <div className="history-list">
          {currentSessions.map((session) => (
            <div key={session.id} className="history-row-container">
            <div key={session.id} className={`history-row-card ${expandedId === session.id ? 'active' : ''}`}
            onClick={() => toggleExpand(session.id)}>
              <div className="col-title">
                <span className="issue-title">{session.issue.title}</span>
                <span className="issue-num">#{session.issue.number}</span>
              </div>
              
              <div className="col-repo">
                {session.issue.repoName}
              </div>
              
              <div className="col-time">
                {formatTrackedTime(session.trackedSeconds)}
              </div>
              
              <div className="col-date">
                {formatDate(session.timeblocks)}
              </div>
              
              <div className="col-synced">
                
                <span className={`synced-${session.synced}`}>{session.synced ? "Yes" : "No"}</span>
              </div>

              <div className="col-actions">
                <button disabled={session.synced}
                className="sync-btn" onClick={(e) => {
                    e.stopPropagation();
                    syncSession(session.id, session.notes)
                    }}>
                     Sync
                </button>
                <button className="edit-btn"
                onClick={(e) => {
                    e.stopPropagation();
                    if (session) setEditingSession(session)}}
                >Edit</button>
                <button className="delete-icon-btn" title="Delete session"
                onClick={(e) => {
                    e.stopPropagation();
                    setConfirmTitle("Are you sure you want to delete this session?");
                    setConfirmMessageBody("This change will be synchronized with GitHub automatically");
                    setShowNotes(false);
                    setConfirmAction(() => () => deleteSession(session.id));
                    setShowConfirm(true)
                    }}>             
                   <AiOutlineDelete />
                </button>
              </div>
            </div>
            {/* Expandable Notes Section */}
            {expandedId === session.id && (
            <div className="history-row-notes">
                <div className="notes-content">
                <strong>Session Notes:</strong>
                <p>{session.notes || "No notes provided for this session."}</p>
                </div>
            </div>
            )}
            </div>
          ))}
        </div>

        
      </div>
      {/* Pagination Controls */}
        {totalPages > 1 && (
          <div className="pagination-bar">
            {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
              <button
                key={page}
                className={`page-num ${currentPage === page ? 'active' : ''}`}
                onClick={() => setCurrentPage(page)}
              >
                {page}
              </button>
            ))}
          </div>
        )}
    </div>
  );
}

