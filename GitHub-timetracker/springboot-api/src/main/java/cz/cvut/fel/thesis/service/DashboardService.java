package cz.cvut.fel.thesis.service;

import java.io.NotActiveException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.cvut.fel.thesis.dto.GitHubIssueDTO;
import cz.cvut.fel.thesis.dto.HomeDashboardDTO;
import cz.cvut.fel.thesis.dto.SessionDTO;
import cz.cvut.fel.thesis.model.Issue;
import cz.cvut.fel.thesis.model.User;

@Service
public class DashboardService {

    @Autowired
    private IssueService issueService;

    @Autowired
    private UserService userService;

    @Autowired
    private SessionService sessionService;

     /**
      * Retrieves home dashboard data for the given user, including active, pinned, and assigned issues.
      *
      * @param user authenticated user
      * @return home dashboard data
      */
    public HomeDashboardDTO getHomeDashboardData(User user) {
        List<SessionDTO> toSync = sessionService.getUnsyncedDTOs(user);
        Long activeSessionId = user.getActiveSessionID();
        Set<Long> pinnedIssueIds = userService.getPinnedIssueGitHubIds(user);
        Instant currStartTime = null;

        //check if active session is paused
        Boolean trackingPaused;
        try {
            trackingPaused = sessionService.isActivePaused(user);
        } catch (NotActiveException e) {
            trackingPaused = false;
        }

        //get active issue with time tracked and sync status
        GitHubIssueDTO active = null;
        if (activeSessionId != null) {
            active = issueService.getIssueBySessionId(user.getActiveSessionID());
            if (active != null) {
                 Issue issueEntity = issueService.getByGitHubID(active.id());
                 if (issueEntity != null) {
                     Long timeInSeconds = issueService.getTimeTrackedForIssueInSec(issueEntity, user);
                     boolean allSynced = sessionService.allSyncedForIssue(issueEntity, user);
                     active = GitHubIssueDTO.withTimeTrackedAndSync(active, timeInSeconds, allSynced);
                     currStartTime = issueService.getActiveSessionStartTimeForIssue(issueEntity, user);
                 }
            }
        }

        //get assigned and pinned issues with time tracked and sync status
        List<GitHubIssueDTO> assignedIssues = issueService.getAssignedIssues();
        Set<Long> assignedIssueIds = assignedIssues.stream().map(GitHubIssueDTO::id).collect(java.util.stream.Collectors.toSet());
        Map<Long, Issue> issuesByGithubId = issueService.getByGitHubIDs(assignedIssueIds);
        Map<Long, Long> trackedSecondsByIssue = sessionService.getTrackedSecondsByIssueGithubIds(user, assignedIssueIds);
        Map<Long, Boolean> allSyncedByIssue = sessionService.getAllSyncedByIssueGithubIds(user, assignedIssueIds);

        List<GitHubIssueDTO> pinned  = new java.util.ArrayList<>();
        List<GitHubIssueDTO> assigned = new java.util.ArrayList<>();
        for (GitHubIssueDTO issue : assignedIssues) {
            Issue issueEntity = issuesByGithubId.get(issue.id());
            if (issueEntity != null) {
                //issue has been tracked before, wrap with time tracked and sync status
                Long timeInSeconds = trackedSecondsByIssue.getOrDefault(issue.id(), 0L);
                boolean allSynced = allSyncedByIssue.getOrDefault(issue.id(), true);
                GitHubIssueDTO issueWithTime = GitHubIssueDTO.withTimeTrackedAndSync(issue, timeInSeconds, allSynced);
                assigned.add(issueWithTime);
                if (pinnedIssueIds.contains(issue.id())) {
                    pinned.add(issueWithTime);
                }
            } else {
                //issue has never been tracked, add without time and sync info
                GitHubIssueDTO issueWithTime = GitHubIssueDTO.withTimeTrackedAndSync(issue, 0L, true);
                assigned.add(issueWithTime);
                if (pinnedIssueIds.contains(issue.id())) {
                    pinned.add(issueWithTime);
                }
            }
        }

        return new HomeDashboardDTO(
            assigned,
            pinned,
            active,
            toSync,
            trackingPaused,
            currStartTime
        );
        
    }
    
}
