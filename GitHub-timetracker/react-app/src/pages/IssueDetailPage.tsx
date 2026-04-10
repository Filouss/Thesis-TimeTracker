import { useParams } from "react-router-dom";
import { useIssues } from "../context/IssueContext";
import IssueDetail from "../components/IssueDetail/IssueDetail";
import "../styles/issueDetail.css"
import { useCardActions } from "../hooks/useCardActions";

export default function IssueDetailPage(
) {
    const {id} = useParams();
    const {data, loading, error, refetch} = useIssues();
    const {startTracking, openGithub, pauseTracking, resumeTracking} = useCardActions(refetch);

    const issueId = Number(id);
    const hasValidIssueId = Number.isFinite(issueId);
    const issue = hasValidIssueId ? data?.assigned.find(i => i.id === issueId) : undefined;

    if (loading) {
        return <div>Loading...</div>;
    }

    if (error) {
        return <div>Failed to load issue data: {error}</div>;
    }

    if (!hasValidIssueId) {
        return <div>Invalid issue id.</div>;
    }

    if (!issue) {
        return <div>Issue not found.</div>;
    }

    return (
        <div className="issue-detail-page">
            <IssueDetail 
                issue={issue}
                onStartTracking={startTracking}
                onOpenGithub={openGithub}
                onPauseTracking={pauseTracking}
                onResumeTracking={resumeTracking}
                isCurrent={data?.tracking?.id === issue.id}
                isPaused={data?.trackingPaused}
                isTracking={data?.tracking !== null}
                />
        </div>
    )
};
