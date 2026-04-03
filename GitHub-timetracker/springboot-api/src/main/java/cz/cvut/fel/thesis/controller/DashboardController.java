package cz.cvut.fel.thesis.controller;

import java.io.NotActiveException;
import java.text.Format;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import cz.cvut.fel.thesis.dto.OverviewGraphsDashboardDTO;
import cz.cvut.fel.thesis.dto.OverviewLabelTimeDTO;
import cz.cvut.fel.thesis.dto.OverviewStatsDashboardDTO;
import cz.cvut.fel.thesis.dto.SessionDTO;
import cz.cvut.fel.thesis.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cz.cvut.fel.thesis.dto.DailyTimeTrackDTO;
import cz.cvut.fel.thesis.dto.GitHubIssueDTO;
import cz.cvut.fel.thesis.dto.HomeDashboardDTO;
import cz.cvut.fel.thesis.dto.IssueRankDTO;
import cz.cvut.fel.thesis.model.Issue;
import cz.cvut.fel.thesis.model.User;
import cz.cvut.fel.thesis.service.FormatService;
import cz.cvut.fel.thesis.service.IssueService;
import cz.cvut.fel.thesis.service.UserService;
import cz.cvut.fel.thesis.utils.CurrentUserProvider;

/**
 * Exposes aggregated dashboard data for home and overview screens.
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private IssueService issueService;

    @Autowired
    private UserService userService;
    
    @Autowired
    private CurrentUserProvider userProvider;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private FormatService formatService;

    /**
     * Returns home dashboard data including active, pinned, and assigned issues.
     *
     * @param oAuth2User authenticated OAuth2 principal
     * @return home dashboard payload
     */
    @GetMapping("/home")
    public ResponseEntity<HomeDashboardDTO> getHomepageData(@AuthenticationPrincipal OAuth2User oAuth2User) {
        User user = userProvider.oauthToUser(oAuth2User);

        List<SessionDTO> sessions = sessionService.getUnsyncedDTOs(user);
        Long activeSessionId = user.getActiveSessionID();
        Boolean trackingPaused;
        try {
            trackingPaused = sessionService.isActivePaused(user);
        } catch (NotActiveException e) {
            trackingPaused = false;
        }
        GitHubIssueDTO active = null;
        if (activeSessionId != null) {
            active = issueService.getIssueBySessionId(user.getActiveSessionID());
            if (active != null) {
                 Issue issueEntity = issueService.getByGitHubID(active.id());
                 if (issueEntity != null) {
                     Long timeInSeconds = issueService.getTimeTrackedForIssueInSec(issueEntity, user);
                     boolean allSynced = sessionService.allSyncedForIssue(issueEntity, user);
                     active = GitHubIssueDTO.withTimeTrackedAndSync(active, timeInSeconds, allSynced);
                 }
            }
        }


        List<GitHubIssueDTO> pinned  = new java.util.ArrayList<>();
        List<GitHubIssueDTO> assigned = new java.util.ArrayList<>();
        for (GitHubIssueDTO issue : issueService.getAssignedIssues()) {
            Issue issueEntity = issueService.getByGitHubID(issue.id());
            if (issueEntity != null) {
                Long timeInSeconds = issueService.getTimeTrackedForIssueInSec(issueEntity, user);
                boolean allSynced = sessionService.allSyncedForIssue(issueEntity, user);
                GitHubIssueDTO issueWithTime = GitHubIssueDTO.withTimeTrackedAndSync(issue, timeInSeconds, allSynced);
                assigned.add(issueWithTime);
            } else {
                assigned.add(GitHubIssueDTO.withTimeTrackedAndSync(issue, 0L, true));
            }
            if (userService.getPinnedIssueGitHubIds(user).contains(issue.id())) {
                if (issueEntity != null) {
                    Long timeInSeconds = issueService.getTimeTrackedForIssueInSec(issueEntity, user);
                    boolean allSynced = sessionService.allSyncedForIssue(issueEntity, user);
                    GitHubIssueDTO issueWithTime = GitHubIssueDTO.withTimeTrackedAndSync(issue, timeInSeconds, allSynced);
                    pinned.add(issueWithTime);
                } else{
                    pinned.add(GitHubIssueDTO.withTimeTrackedAndSync(issue, 0L, true));
                }
            }
        }

        return ResponseEntity.ok(
            new HomeDashboardDTO(
                assigned,
                pinned,
                active,
                sessions,
                trackingPaused
            )
        );
    }

    /**
     * Returns overview graph datasets for the current user.
     *
     * @param oAuth2User authenticated OAuth2 principal
     * @param zoneId user time zone id
     * @return overview graph payload
     */
    @GetMapping("/overview/graphs")
    public ResponseEntity<OverviewGraphsDashboardDTO> getOverviewGraphData(@AuthenticationPrincipal OAuth2User oAuth2User,  @RequestParam String zoneId) {
        User user = userProvider.oauthToUser(oAuth2User);
        ZoneId userZoneId = ZoneId.of(zoneId);
        List<DailyTimeTrackDTO> dailyTimeTracked = sessionService.secondsTrackedPerDayThisWeek(user, userZoneId);
        List<IssueRankDTO> rankedIssues = sessionService.getRankedIssues(user);
        List<OverviewLabelTimeDTO> labelTimeDTOs = sessionService.getTimePerLabel(user);
        return ResponseEntity.ok(new OverviewGraphsDashboardDTO(
            dailyTimeTracked,
            rankedIssues,
            labelTimeDTOs
        ));
    }

    /**
     * Returns overview statistics for a selected interval.
     *
     * @param oAuth2User authenticated OAuth2 principal
     * @param interval optional interval selector
     * @param zoneId user time zone id
     * @return overview statistics payload
     */
    @GetMapping("/overview/stats")
    public ResponseEntity<OverviewStatsDashboardDTO> getOverviewStatsData(@AuthenticationPrincipal OAuth2User oAuth2User,
        @RequestParam(name = "interval", required = false) String interval,  @RequestParam String zoneId) {
        User user = userProvider.oauthToUser(oAuth2User);
        ZoneId userZoneId = ZoneId.of(zoneId);
        Long totalTimeTrackedSec = sessionService.secondsTrackedForInterval(user, interval, userZoneId);
        Float workingTimeRatio = sessionService.getWorkingTimeRatio(user,interval, userZoneId);
        return ResponseEntity.ok(new OverviewStatsDashboardDTO(
            totalTimeTrackedSec,
            workingTimeRatio
        ));
    }
}
