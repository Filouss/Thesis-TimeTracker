import {  useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useCardActions } from "../../hooks/useCardActions";
import { IssueCard } from "./IssueCard";
import { SessionCard } from "./SessionCard";
import { ConfirmModal } from "../modals/ConfirmModal";
import { EditSessionModal } from "../modals/EditSessionModal";
import { useIssues } from "../../context/IssueContext";
import LoadingButton from "../button/LoadingButton";
import type { ApiSession } from "../../types";
import "../../styles/modals.css"
import { ActiveTimer } from "./ActiveTimer";
import { useToast } from "../../context/ToastContext";

export default function HomeContent() {
    const navigate = useNavigate();
    const {data, loading, refetch} = useIssues();


    useEffect(() => {
        refetch();
    }, [refetch]);


    const {startTracking, syncSession, openGithub, Pin, unPin, editSession, pauseTracking, resumeTracking, endTracking} = useCardActions(refetch);
    const [showConfirm, setShowConfirm] = useState(false);
    const [editingSession, setEditingSession] = useState<ApiSession | null>(null);
    const [editError, setEditError] = useState<string>("");
    const [showNotes, setShowNotes] = useState(false);
    const [confirmMessageBody, setConfirmMessageBody] = useState("");
    const [confirmTitle, setConfirmTitle] = useState("");
    const [confirmAction, setConfirmAction] = useState<((notes?: string) => void) | null>(null);
    const [isSyncingAll, setIsSyncingAll] = useState(false);
    const { showToast } = useToast();

    function getErrorMessage(error: unknown): string {
        if (typeof error === "object" && error !== null) {
            const typedError = error as {
                response?: { data?: { message?: string } };
                message?: string;
            };
            return typedError.response?.data?.message ?? typedError.message ?? "An error occurred while saving the session";
        }
        return "An error occurred while saving the session";
    }

    async function handleSaveSession(sessionId: number, timeblocks: {start: string, end: string}[], notes: string, synced: boolean ,issueUrl?: string) {
        try {
            setEditError("");
            await editSession(sessionId, timeblocks, notes, synced, issueUrl);
            setEditingSession(null);
            showToast("Session updated successfully", "success");
        } catch (error: unknown) {
            setEditError(getErrorMessage(error));
        }
    };

    function handleStartTracking(issueNumber: number, repository_url: string){
        if(data?.tracking) {
            setConfirmTitle("Are you sure you want start a new session?");
            setConfirmMessageBody("Your currently active session will be ended and moved to Ready to Sync");
            setShowNotes(false);
            setConfirmAction(() => async () => {
                await startTracking(issueNumber, repository_url);
            });
            setShowConfirm(true)
        } else {
            return startTracking(issueNumber, repository_url);
        }
    }

    async function syncAll(){
        if(data)
        await Promise.all(data.toSync.map(s => syncSession(s.id, s.notes)));
        setIsSyncingAll(false);
    }

    if (loading && !data) return <div>Loading dashboard...</div>;
    if (!data) return <div>Error loading data.</div>;

    const isCurrentIssuePinned = data.tracking && data.pinned.some(pinnedIssue => pinnedIssue.id === data.tracking?.id);

    return (
        <div className="homepage-content">
                    {showConfirm && (
                        <ConfirmModal
                            title={confirmTitle}
                            body={confirmMessageBody}
                            onConfirm={(notes) => {
                                confirmAction?.(notes);
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
                    <div className="homepage-issues">
                        <div className="card-wrapper">
                            <div className="issue-section-title">
                                <div className="title-with-count">
                                    <span>Assigned Issues</span>
                                    <span>{data?.assigned.length}</span>
                                </div>
                            </div>
                            <div className="homeCard-list" id="assigned">
                                {data.assigned && data?.assigned?.length > 0 ? (data.assigned.map((issue) => 
                                {
                                    return(
                                    <IssueCard
                                        key={issue.id}
                                        issue={issue}
                                        onStartTracking={handleStartTracking}
                                        onOpenGithub={openGithub}
                                        PinBtnAction={Pin}
                                        onPauseTracking={pauseTracking}
                                        onResumeTracking={resumeTracking}
                                        isCurrent={data.tracking?.id === issue.id}
                                        isPaused={data.trackingPaused}
                                        onClick={() => navigate(`/issues/${issue.id}`)}
                                    />
                                )
                                }
                                )) : <p>No assigned issues found. Make sure you have issues assigned to you on GitHub.</p>}
                            </div>
                        </div>
                        <div className="card-wrapper">
                            <div className="issue-section-title">
                                Pinned Issues
                            </div>
                            <div className="homeCard-list" id="pinned">
                                {data.pinned && data.pinned.length > 0 ? (data.pinned.map((issue) => (
                                    <IssueCard
                                        key={issue.id}
                                        issue={issue}
                                        onStartTracking={handleStartTracking}
                                        onOpenGithub={openGithub}
                                        PinBtnAction={unPin}
                                        onPauseTracking={pauseTracking}
                                        onResumeTracking={resumeTracking}
                                        isCurrent={data.tracking?.id === issue.id}
                                        isPaused={data.trackingPaused}
                                        onClick={() => navigate(`/issues/${issue.id}`)}
                                    />
                                    ))) : <p>No pinned issues. Click the pin icon on any issue to pin it.</p>}
                            </div>
                        </div>
                        <div className="card-wrapper">
                            <div className="tracking-wrapper">
                                <div className="issue-section-title">
                                    Currently tracking
                                </div>
                                {data.tracking ? (
                                    <IssueCard
                                        key={data.tracking.id}
                                        issue={data.tracking}
                                        onStartTracking={() => {}}
                                        onOpenGithub={openGithub}
                                        PinBtnAction={isCurrentIssuePinned ? unPin : Pin}
                                        onPauseTracking={pauseTracking}
                                        onResumeTracking={resumeTracking}
                                        isCurrent={true}
                                        isPaused={data.trackingPaused}
                                        onClick={() => navigate(`/issues/${data.tracking!.id}`)}
                                    />
                                ) : (
                                    <p>No issue currently tracking.</p>
                                )}
                            </div>
                            <div className="sync-wrapper">
                                <div className="issue-section-title">
                                    <div className="title-with-count" id="toSync">
                                        <span>Ready to sync</span>
                                        <span>{data.toSync.length}</span>
                                    </div>
                                </div>
                                <div className="homeCard-list">
                                    {data.toSync.length > 0 ? (
                                        [...data.toSync]
                                        .sort((a, b) => {
                                            const timeA = new Date(a.timeblocks[0]?.start || 0).getTime();
                                            const timeB = new Date(b.timeblocks[0]?.start || 0).getTime();
                                            return timeB - timeA;
                                        })
                                        .map((session: ApiSession) => (
                                        <SessionCard
                                            key={session.id}
                                            session={session}
                                            onSync={async (sessionId, notes) => {
                                                await syncSession(sessionId, notes);
                                                showToast("Session synchronized successfully", "success");
                                            }}
                                            onEdit={(sessionId) => {
                                                const sessionToEdit = data.toSync.find(s => s.id === sessionId);
                                                if (sessionToEdit) setEditingSession(sessionToEdit);
                                            }}
                                        />
                                    ))
                                    ) : 
                                        <p>Sessions that are ready to be synchronized with GitHub will be displayed here.</p>
                                    }
                                </div>
                            </div>
                        </div>
                    </div>
                <div className="homepage-btns">
                    <div className="left-btns">
                        <button onClick={() => {
                        setConfirmTitle("Are you sure you want to end the session?");
                        setConfirmMessageBody("You can now add any session notes and this session will be added to the Ready to sync section.");
                        setShowNotes(true);
                        setConfirmAction(() => (notes?: string) => {
                            endTracking(notes || "");
                        });
                        setShowConfirm(true)
                        }                    
                        }
                        disabled={!data.tracking}
                        className="endSessionBtn"
                        >End session</button>
                        <div>
                            {data.tracking ? (
                                <ActiveTimer isActive={!data.trackingPaused} accumulatedSeconds={data.currFinishedTBDuration} currentTrackStartTime={data.currLatestTBStartTime}  ></ActiveTimer>
                            )
                             : <p></p>}
                        </div>
                    </div>
                    <LoadingButton
                        isLoading={isSyncingAll}
                        onClick={() => {
                        setConfirmTitle("Are you sure you want to sync all sessions to GitHub?");
                        setConfirmMessageBody("This will update the linked issues with the tracked time and notes.");
                        setShowNotes(false);
                        setConfirmAction(() => () => {
                            setIsSyncingAll(true);
                            syncAll();
                            showToast("All sessions synchronized successfully", "success");
                        });
                        setShowConfirm(true);
                        
                        
                    }}
                    disabled={data.toSync.length === 0}
                    className="syncAllBtn"
                    >Sync all to GitHub</LoadingButton>
                    
                </div>
        </div>
    )
};
