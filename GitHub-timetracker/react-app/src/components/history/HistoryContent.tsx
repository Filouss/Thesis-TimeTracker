import { useState } from "react";
import { formatTrackedTime } from "../../lib/utils";
import "../../styles/HistoryPage.css";
import { ConfirmModal } from "../modals/ConfirmModal";
import { useCardActions } from "../../hooks/useCardActions";
import { EditSessionModal } from "../modals/EditSessionModal";
import { AiOutlineDelete } from "react-icons/ai";
import { IoArrowDownOutline, IoArrowUpOutline } from "react-icons/io5";
import LoadingButton from "../button/LoadingButton";
import Toast from "../modals/Toast";

type TimeBlock = {
  start: string;
  end: string;
};

type ApiSession = {
  id: number;
  issue: { id: number, title: string, labels: { id: number, name: string, color: string }[], repoName: string, repoOwner: string, number: number };
  timeblocks: TimeBlock[];
  paused: boolean;
  notes: string;
  trackedSeconds: number;
  synced: boolean
}

type HistoryContentProps = {
  sessions: ApiSession[];
  onRefetch: (sortBy?: string, direction?: string) => void;
}

export default function HistoryContent({
  sessions,
  onRefetch
}: HistoryContentProps) {
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 6;
  const [showConfirm, setShowConfirm] = useState(false);
  const [editingSession, setEditingSession] = useState<ApiSession | null>(null);
  const [editError, setEditError] = useState<string>("");
  const [showNotes, setShowNotes] = useState(false);
  const [confirmMessageBody, setConfirmMessageBody] = useState("");
  const [confirmTitle, setConfirmTitle] = useState("");
  const [confirmAction, setConfirmAction] = useState<((notes?: string) => void) | null>(null);
  const { editSession, deleteSession, syncSession } = useCardActions(() => onRefetch(sortBy, descDirection ? "desc" : "asc"));
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [descDirection, setDescDirection] = useState(true)
  const [sortBy, setSortBy] = useState("createdAt")
  const [syncingId, setSyncingId] = useState<number | null>(null);
  const [showToast, setShowToast] = useState(false);
  const [toastMessage, setToastMessage] = useState("Action was succesful!")

  async function toggleExpand(id: number) {
    setExpandedId(expandedId === id ? null : id);
  }

  const totalPages = Math.ceil(sessions.length / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const currentSessions = sessions.slice(startIndex, startIndex + itemsPerPage);

  function formatDate(timeblocks: { start: string, end: string }[]) {
    if (!timeblocks || timeblocks.length === 0) return "N/A";
    return new Date(timeblocks[0].start).toLocaleDateString();
  };

  async function handleSaveSession(sessionId: number, timeblocks: { start: string, end: string }[], notes: string, synced: boolean, issueUrl?: string) {
    try {
      setEditError("");
      await editSession(sessionId, timeblocks, notes, synced, issueUrl);
      setEditingSession(null);
      setToastMessage("Session edited succefully")
      setShowToast(true);
    } catch (error: any) {
      if (error.response?.data?.message) {
        setEditError(error.response.data.message);
      } else if (error.message) {
        setEditError(error.message);
      } else {
        setEditError("An error occurred while saving the session");
      }
    }
  };

  function handleSortClick(field: string){
    const isSameField = sortBy === field;
    const newDirection = isSameField ? !descDirection : true; 

    setSortBy(field);
    setDescDirection(newDirection);

    onRefetch(field, newDirection ? "desc" : "asc");
};


  return (
    <div className="history-page-wrapper">
      <Toast 
        isVisible={showToast} 
        message={toastMessage} 
        onClose={() => setShowToast(false)} 
      />
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
      {sessions ? (
          <div className="history-container">
        <h2 className="page-title">Tracked sessions history</h2>
        {/* Table Header */}
        <div className="history-table-header">
          <div 
            className={`col-title ${sortBy === "issue.title" ? "active-sort" : ""}`} 
            onClick={() => handleSortClick("issue.title")}
          >
            Worked issue title
            {sortBy === "issue.title" && (descDirection ? <IoArrowDownOutline /> : <IoArrowUpOutline />)}
          </div>

          <div 
            className={`col-repo ${sortBy === "issue.repository.name" ? "active-sort" : ""}`} 
            onClick={() => handleSortClick("issue.repository.name")}
          >
            Repository
            {sortBy === "issue.repository.name" && (descDirection ? <IoArrowDownOutline /> : <IoArrowUpOutline />)}
          </div>

          <div 
            className={`col-time ${sortBy === "timeTracked" ? "active-sort" : ""}`} 
            onClick={() => handleSortClick("timeTracked")}
          >
            Time tracked
            {sortBy === "timeTracked" && (descDirection ? <IoArrowDownOutline /> : <IoArrowUpOutline />)}
          </div>

          <div 
            className={`col-date ${sortBy === "createdAt" ? "active-sort" : ""}`} 
            onClick={() => handleSortClick("createdAt")}
          >
            Date
            {sortBy === "createdAt" && (descDirection ? <IoArrowDownOutline /> : <IoArrowUpOutline />)}
        </div>

  <div 
    className={`col-synced ${sortBy === "synced" ? "active-sort" : ""}`} 
    onClick={() => handleSortClick("synced")}
  >
    Synced
    {sortBy === "synced" && (descDirection ? <IoArrowDownOutline /> : <IoArrowUpOutline />)}
  </div>
  
  <div className="col-actions"></div>
</div>


        {/* Table Body */}
        <div className="history-list">
          {currentSessions.map((session) => (
            <div key={session.id} className="history-row-container">
              <div key={session.id} className={`history-row-card ${expandedId === session.id ? 'active' : ''}`}
                onClick={() => toggleExpand(session.id)}>
                <div className="col-title">
                  <span className="history-issue-title">{session.issue.title}</span>
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
                  <LoadingButton
                      isLoading={syncingId === session.id}
                      onClick={async(e) => {
                      e.stopPropagation();
                      setSyncingId(session.id)
                      try{
                        await syncSession(session.id, session.notes)
                        setToastMessage("Session synced to GitHub");
                        setShowToast(true)
                      } finally {
                        setSyncingId(null)
                      }
                  }}
                  disabled={session.synced}
                  className="sync-btn"
                  >Sync</LoadingButton>
                  <button className="edit-btn"
                    onClick={(e) => {
                      e.stopPropagation();
                      if (session) setEditingSession(session)
                    }}
                  >Edit</button>
                  <button className="delete-icon-btn" title="Delete session"
                    onClick={(e) => {
                      e.stopPropagation();
                      setConfirmTitle("Are you sure you want to delete this session?");
                      setConfirmMessageBody("This change will be synchronized with GitHub automatically");
                      setShowNotes(false);
                      setConfirmAction(() => () => {
                        deleteSession(session.id)
                        setToastMessage("Session deleted")
                        setShowToast(true)
                      });
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
      ) : (
        <p>No sessions tracked.</p>
      )}
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

