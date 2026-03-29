import { useParams } from "react-router-dom";
import { useIssues } from "../context/IssueContext";
import IssueDetail from "../components/IssueDetail/IssueDetail";
import "../styles/issueDetail.css"
import { useCardActions } from "../hooks/useCardActions";

export default function IssueDetailPage(
) {
    const {id} = useParams();
    const {data, refetch} = useIssues();
    const {startTracking, openGithub, pauseTracking, resumeTracking} = useCardActions(refetch);

    const issue = data?.assigned.find(i => i.id === Number(id));
    console.log(issue);

    if (!issue) {
        return <div>Loading...</div>;
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
