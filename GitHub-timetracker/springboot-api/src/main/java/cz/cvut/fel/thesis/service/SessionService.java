package cz.cvut.fel.thesis.service;

import cz.cvut.fel.thesis.client.GitHubAPIClient;
import cz.cvut.fel.thesis.dao.*;
import cz.cvut.fel.thesis.dto.*;
import cz.cvut.fel.thesis.exceptions.UnassignedIssueException;
import cz.cvut.fel.thesis.model.*;

import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.io.NotActiveException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Manages session lifecycle, synchronization, and session data aggregation.
 */
@Service
public class SessionService {

    @Autowired
    private SessionDAO sessionDAO;

    @Autowired
    private IssueDAO issueDAO;

    @Autowired
    private TimeBlockDAO timeBlockDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private IssueService issueService;

    @Autowired
    private FormatService syncFormatService;

    @Autowired
    private GitHubAPIClient gitHubAPIClient;

    /**
     * Starts tracking a session for the given issue and user.
     *
     * @param issueNumber issue number in the repository
     * @param repo repository name
     * @param owner repository owner
     * @param user application user
     * @throws NotActiveException when the current active session cannot be closed cleanly
     */
    @Transactional
    public void startSession(int issueNumber, String repo, String owner, User user) {
        //check for tracking, end if needed
        if (user.isTracking()) {
            try {
                endSession(user, null);
                sessionDAO.flush(); 
                userDAO.flush();
            } catch (NotActiveException e) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "No active session");
            }
        }

        GitHubIssueDTO fetchedIssue = issueService.getIssue(issueNumber, repo, owner);
        if (fetchedIssue.assignees() == null ||
                fetchedIssue.assignees().stream().noneMatch(assignee -> assignee.login().equals(user.getUsername()))) {

            throw new UnassignedIssueException(HttpStatus.BAD_REQUEST, "Can't start a session for an unassigned issue");
        }

        Repository repository = issueService.getOrCreateRepository(repo, owner);
        Set<Label> labels = issueService.getOrCreateLabels(fetchedIssue);
        Issue issue = issueService.getOrCreateIssue(issueNumber, fetchedIssue, repository, labels);
        Session session = createSession(user, issue);
        createTimeBlock(session);
        user.setTracking(true);
        user.setActiveSessionID(session.getId());
        userDAO.save(user);
    }

    /**
     * Ends the current active session and persists optional notes.
     *
     * @param user application user
     * @param notes optional session notes
     * @throws NotActiveException when the user has no active session
     */
    @Transactional
    public void endSession(User user, String notes) throws NotActiveException {
        pauseSession(user);
        Session session = sessionDAO
                .findById(user.getActiveSessionID())
                .orElseThrow(NotActiveException::new);
        session.setFinished(true);
        if (notes == null || !notes.isEmpty()) {
            session.setNotes(notes);
        }
        sessionDAO.save(session);

        user.setActiveSessionID(null);
        user.setTracking(false);
        userDAO.save(user);
    }

    /**
     * Pauses the user's active session and closes the current time block.
     *
     * @param user application user
     * @throws NotActiveException when the user has no active session
     */
    @Transactional
    public void pauseSession(User user) throws NotActiveException {
        Session session = sessionDAO
                .findById(user.getActiveSessionID())
                .orElseThrow(NotActiveException::new);

        if (session.isPaused()) {
            return;
        }
        session.setPaused(true);

        // Force reload timeblocks from DB to ensure we have the most recent one
        session.getTimeBlocks().size();

        TimeBlock tb = session.getMostRecentTimeBlock();
        if (tb != null) {
            tb.setEndDate(Instant.now());
            timeBlockDAO.save(tb);
        }

        // recalculate timeTracked
        long seconds = session.getTimeBlocks().stream()
                .filter(block -> block.getStartDate() != null && block.getEndDate() != null)
                .mapToLong(block -> Duration.between(block.getStartDate(), block.getEndDate()).getSeconds())
                .sum();
        session.setTimeTracked(seconds);

        sessionDAO.save(session);
    }

    /**
     * Resumes the user's paused active session.
     *
     * @param user application user
     * @throws NotActiveException when the user has no active session
     */
    @Transactional
    public void resumeSession(User user) throws NotActiveException {
        Session session = sessionDAO
                .findById(user.getActiveSessionID())
                .orElseThrow(NotActiveException::new);

        if (!session.isPaused()) {
            return;
        }
        session.setPaused(false);
        createTimeBlock(session);
        sessionDAO.save(session);
    }

    /**
     * Deletes a session and updates or removes the related GitHub comment.
     *
     * @param user application user
     * @param sessionId session id
     * @param userZoneId user time zone
     */
    @Transactional
    public void deleteSession(User user, Long sessionId, ZoneId userZoneId) {
        Session session = sessionDAO.findByIdAndUser(sessionId, user).orElse(null);
        if (session == null) {
            return;
        }
        Issue issue = session.getIssue();
        issue.getSessions().remove(session);
        //if the issue has no synced sessions, delete the comment
        if (issue.getSessions().stream().noneMatch(Session::isSynced) && issue.getGithubCommentId() != null) {
            try {
                gitHubAPIClient.deleteComment(issue);
            } catch (WebClientResponseException.NotFound e) {
                //comment already deleted
                return;
            }
            issue.setGithubCommentId(null);
            issueDAO.save(issue);
            sessionDAO.delete(session);
            return;
        }
        //edit the github comment after session deletion
        String body = syncFormatService.buildSessionComments(session.getIssue(), userZoneId);
        if (issue.getGithubCommentId() == null) {
            // no comment created yet, no need to fetch
            sessionDAO.delete(session);
            return;
        }
        try {
            gitHubAPIClient.editComment(session, body, issue.getGithubCommentId());
        } catch (WebClientResponseException.NotFound e) {
            // comment deleted or inaccessible
            gitHubAPIClient.createIssueComment(issue, body);
        }
        sessionDAO.delete(session);
    }

    /**
     * Updates the selected session with new issue, notes, and time block data.
     *
     * @param user application user
     * @param sessionId session id
     * @param sessionUpdate update payload
     * @return updated session, or {@code null} if it does not exist for the user
     */
    @Transactional
    public Session editSession(User user, Long sessionId, UpdateSessionRequest sessionUpdate) {
        Session existingSession = sessionDAO.findByIdAndUser(sessionId, user).orElse(null);
        UpdateIssueRequest updateIssue = sessionUpdate.issue();
        if (existingSession == null) {
            return null;
        }
        ;
        existingSession.setNotes(sessionUpdate.notes());
        GitHubIssueDTO fetchedIssue = issueService.getIssue(updateIssue.issueNumber(), updateIssue.repoName(),
                updateIssue.repoOwner());
        if (fetchedIssue != null) {
            if (fetchedIssue.assignees() == null || fetchedIssue.assignees().stream().noneMatch(assignee -> assignee.login().equals(user.getUsername()))) {
                throw new UnassignedIssueException(HttpStatus.BAD_REQUEST,
                        "Can't create a session for an unassigned issue");
            }
            Issue toSet = issueDAO.findByGithubId(fetchedIssue.id()).orElse(null);
            if (toSet != null) {
                //issue already created in db
                existingSession.setIssue(toSet);
            } else {
                //issue not found in db, create it
                Repository repository = issueService.getOrCreateRepository(fetchedIssue.repoName(), fetchedIssue.repoOwnerFromUrl());
                Set<Label> labels = issueService.getOrCreateLabels(fetchedIssue);
                Issue toSave = issueService.getOrCreateIssue(updateIssue.issueNumber(), fetchedIssue, repository,
                        labels);
                existingSession.setIssue(toSave);
            }
        }
        if (sessionUpdate.timeBlocks() != null) {
            replaceTimeBlocks(existingSession, sessionUpdate.timeBlocks());
        }
        // user can now resync changes
        if (sessionUpdate.synced()) {
            existingSession.setSynced(false);
        }
        return sessionDAO.save(existingSession);
    }

    /**
     * Replaces the session time blocks with validated updated blocks.
     *
     * @param existingSession session to update
     * @param updateTimeBlockRequests replacement time blocks
     */
    private void replaceTimeBlocks(Session existingSession, List<UpdateTimeBlockRequest> updateTimeBlockRequests) {
        // final set to add to session
        List<TimeBlock> finalBlocks = new ArrayList<>();

        for (UpdateTimeBlockRequest dto : updateTimeBlockRequests) {
            if (dto.start() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "timeBlocks.startDate is required");
            }
            if (dto.end() != null && dto.end().isBefore(dto.start())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "timeBlocks.endDate must be >= startDate");
            }

            TimeBlock tb = new TimeBlock();

            tb.setStartDate(dto.start());
            tb.setEndDate(dto.end());
            tb.setSession(existingSession);

            finalBlocks.add(tb);
        }

        // UPDATE createdAt to the earliest block start for sorting
        Instant earliestStart = finalBlocks.stream()
                .map(TimeBlock::getStartDate)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
        existingSession.setCreatedAt(earliestStart);
        existingSession.getTimeBlocks().clear();
        existingSession.getTimeBlocks().addAll(finalBlocks);

        // recalculate timeTracked
        long seconds = finalBlocks.stream()
                .filter(block -> block.getStartDate() != null && block.getEndDate() != null)
                .mapToLong(block -> Duration.between(block.getStartDate(), block.getEndDate()).getSeconds())
                .sum();
        existingSession.setTimeTracked(seconds);
    }

    /**
     * Creates and persists a new time block for the session.
     *
     * @param session session to extend
     */
    private void createTimeBlock(Session session) {
        TimeBlock timeBlock = new TimeBlock();
        timeBlock.setStartDate(Instant.now());
        timeBlock.setSession(session);
        session.getTimeBlocks().add(timeBlock);
        timeBlockDAO.save(timeBlock);
    }

    /**
     * Creates a new session entity and persists it.
     *
     * @param user application user
     * @param issue issue being tracked
     * @return persisted session
     */
    private Session createSession(User user, Issue issue) {
        Session session = new Session();
        session.setPaused(false);
        session.setIssue(issue);
        session.setSynced(false);
        session.setUser(user);
        session.setFinished(false);
        session.setCreatedAt(Instant.now());
        session.setTimeTracked(0L);
        session = sessionDAO.save(session);
        user.setActiveSessionID(session.getId());
        return session;
    }

    /**
     * Returns sessions for a user with the requested sort order.
     *
     * @param user application user
     * @param sortBy sort field
     * @param direction sort direction
     * @return sorted session list
     */
    public List<Session> getSessions(User user, String sortBy, String direction) {
        if (sortBy == null)
            sortBy = "createdAt";
        if (direction == null)
            direction = "desc";
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        return sessionDAO.findByUserAndFinishedTrue(user, sort);
    }

    /**
     * Synchronizes a finished session with its GitHub issue comment.
     *
     * @param sessionId session id
     * @param notes optional notes
     * @param user application user
     * @param userZoneId user time zone
     */
    public void syncSession(Long sessionId, String notes, User user, ZoneId userZoneId) {
        Session toSync = sessionDAO.findByIdAndUser(sessionId, user).orElse(null);
        if (toSync == null || !toSync.isFinished()) {
            return;
        }
        ;
        Long commentId = toSync.getIssue().getGithubCommentId();
        if (commentId == null) {
            // add a comment
            addSessionComment(toSync, notes, userZoneId);
        } else {
            // edit existing comment
            editSessionComment(toSync, notes, commentId, userZoneId);
        }
        toSync.setNotes(notes);
        toSync.setSynced(true);
        sessionDAO.save(toSync);
    }

    /**
     * Updates an existing GitHub comment for a synchronized session.
     *
     * @param toSync session being synchronized
     * @param notes notes text
     * @param commentId GitHub comment id
     * @param userZoneId user time zone
     */
    private void editSessionComment(Session toSync, String notes, Long commentId, ZoneId userZoneId) {
        toSync.setNotes(notes);
        toSync.setSynced(true);
        sessionDAO.save(toSync);
        String body = syncFormatService.buildSessionComments(toSync.getIssue(), userZoneId);

        try {
            gitHubAPIClient.editComment(toSync, body, commentId);
        } catch (WebClientResponseException.NotFound e) {
            // comment deleted or inaccessible
            CommentDTO commentDTO = gitHubAPIClient.createIssueComment(toSync.getIssue(), body);
            if (commentDTO != null) {
                toSync.getIssue().setGithubCommentId(commentDTO.id());
                issueDAO.save(toSync.getIssue());
            }
        }
    }

    /**
     * Creates a new GitHub comment for a synchronized session.
     *
     * @param toSync session being synchronized
     * @param notes notes text
     * @param userZoneId user time zone
     */
    private void addSessionComment(Session toSync, String notes, ZoneId userZoneId) {
        Duration duration = toSync.getDuration();
        String body = syncFormatService.buildNewComment(toSync, notes, duration, userZoneId);

        CommentDTO commentDTO = gitHubAPIClient.createIssueComment(toSync.getIssue(), body);
        if (commentDTO != null) {
            toSync.getIssue().setGithubCommentId(commentDTO.id());
            issueDAO.save(toSync.getIssue());
        }
    }

    /**
     * Returns unsynced finished sessions for the user.
     *
     * @param user application user
     * @return unsynced session DTOs
     */
    public List<SessionDTO> getUnsyncedDTOs(User user) {
        return sessionDAO.findByUserAndSyncedFalseAndFinishedTrue(user).stream()
                .map(SessionDTO::fromEntity)
                .toList();
    }

    /**
     * Determines whether the user's active session is paused.
     *
     * @param user application user
     * @return {@code true} when the active session is paused
     * @throws NotActiveException when the user has no active session
     */
    public boolean isActivePaused(User user) throws NotActiveException {
        if (user.getActiveSessionID() == null) {
            throw new NotActiveException();
        }
        Session session = sessionDAO
                .findById(user.getActiveSessionID())
                .orElseThrow(NotActiveException::new);
        return session.isPaused();
    }

    /**
     * Checks whether all finished sessions for an issue are synced.
     *
     * @param issue issue entity
     * @param user application user
     * @return {@code true} when all finished sessions are synced
     */
    public boolean allSyncedForIssue(Issue issue, User user) {
        List<Session> sessions = sessionDAO.findByIssueAndUser(issue, user);
        for (Session session : sessions) {
            if (!session.isSynced() && session.isFinished()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns tracked seconds grouped by issue GitHub id for the given user.
     *
     * @param user application user
     * @param issueGithubIds issue GitHub ids
     * @return map of issue GitHub id to tracked seconds
     */
    public Map<Long, Long> getTrackedSecondsByIssueGithubIds(User user, Set<Long> issueGithubIds) {
        if (issueGithubIds == null || issueGithubIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : sessionDAO.getTrackedSecondsByIssueGithubIds(user, issueGithubIds)) {
            Long githubId = (Long) row[0];
            Long trackedSeconds = (Long) row[1];
            result.put(githubId, trackedSeconds != null ? trackedSeconds : 0L);
        }
        return result;
    }

    /**
     * Returns whether all finished sessions are synced, grouped by issue GitHub id.
     *
     * @param user application user
     * @param issueGithubIds issue GitHub ids
     * @return map of issue GitHub id to sync state
     */
    public Map<Long, Boolean> getAllSyncedByIssueGithubIds(User user, Set<Long> issueGithubIds) {
        if (issueGithubIds == null || issueGithubIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Boolean> result = new HashMap<>();
        issueGithubIds.forEach(id -> result.put(id, true));
        for (Object[] row : sessionDAO.countUnsyncedFinishedByIssueGithubIds(user, issueGithubIds)) {
            Long githubId = (Long) row[0];
            Long unsyncedCount = (Long) row[1];
            result.put(githubId, unsyncedCount == null || unsyncedCount == 0L);
        }
        return result;
    }

    /**
     * Returns session DTOs for a given issue with sorting.
     *
     * @param issue issue entity
     * @param user application user
     * @param sortBy sort field
     * @param direction sort direction
     * @return session DTO list for the issue
     */
    public List<SessionDTO> getSessionDTOsForIssue(Issue issue, User user, String sortBy, String direction) {
        if (sortBy == null)
            sortBy = "createdAt";
        if (direction == null)
            direction = "desc";
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        List<Session> sessions = sessionDAO.findByIssueAndUser(issue, user, sort).stream().filter(Session::isFinished).toList();
        return sessions.stream()
                .map(SessionDTO::fromEntity)
                .toList();
    }

    /**
     * Calculates the time tracked for each day of the current week summary.
     *
     * @param user       application user
     * @param userZoneId user time zone
     * @return a list representing tracked time per day
     */
    public List<DailyTimeTrackDTO> secondsTrackedPerDayThisWeek(User user, ZoneId userZoneId) {
        Instant intervalStart = getIntervalStart("ThisWeek", userZoneId);
        Instant intervalEnd = getIntervalend("ThisWeek", userZoneId);

        Map<java.time.DayOfWeek, Long> dailyAccumulator = new EnumMap<>(java.time.DayOfWeek.class);
        for (java.time.DayOfWeek day : java.time.DayOfWeek.values()) {
            dailyAccumulator.put(day, 0L);
        }

        sessionDAO.findFinishedSessionsInInterval(user, intervalStart, intervalEnd).forEach(session -> {
            java.time.DayOfWeek day = session.getCreatedAt().atZone(userZoneId).getDayOfWeek();
            dailyAccumulator.put(day, dailyAccumulator.get(day) + session.getDuration().getSeconds());
        });

        List<DailyTimeTrackDTO> toReturn = new ArrayList<>();
        // Iterate over Monday through Sunday
        for (java.time.DayOfWeek day : java.time.DayOfWeek.values()) {
            // Converts MONDAY to e.g. "Monday"
            String dayName = day.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH);
            toReturn.add(new DailyTimeTrackDTO(dayName, dailyAccumulator.get(day)));
        }

        return toReturn;
    }

    /**
     * Calculates the total tracked time for a user within a specific interval.
     *
     * @param user       application user
     * @param interval   reporting interval (default is "ThisWeek")
     * @param userZoneId user time zone
     * @return total tracked seconds
     */
    public Long secondsTrackedForInterval(User user, String interval, ZoneId userZoneId) {
        if (interval == null)
            interval = "ThisWeek";

        Instant intervalStart = getIntervalStart(interval, userZoneId);
        Instant intervalEnd = getIntervalend(interval, userZoneId);

        Long totalSeconds = sessionDAO.sumTimeTrackedByUserAndInterval(user, intervalStart, intervalEnd);
        return totalSeconds != null ? totalSeconds : 0L;
    }

    /**
     * Returns the end boundary for a named reporting interval.
     *
     * @param interval interval identifier
     * @param userZoneId user time zone
     * @return interval end instant
     */
    private Instant getIntervalend(String interval, ZoneId userZoneId) {
        ZonedDateTime zdtNow = ZonedDateTime.now(userZoneId);
        switch (interval) {
            case "Yesterday":
                return zdtNow.toLocalDate().atStartOfDay(userZoneId).toInstant();
            case "LastWeek":
                return zdtNow.toLocalDate().with(java.time.DayOfWeek.MONDAY).atStartOfDay(userZoneId).toInstant();
            case "LastMonth":
                return zdtNow.toLocalDate().withDayOfMonth(1).atStartOfDay(userZoneId).toInstant();
            case "Today":
            case "ThisWeek":
            case "ThisMonth":
            default: // This year
                return Instant.now();
        }
    }

    /**
     * Returns the start boundary for a named reporting interval.
     *
     * @param interval interval identifier
     * @param userZoneId user time zone
     * @return interval start instant
     */
    private Instant getIntervalStart(String interval, ZoneId userZoneId) {
        ZonedDateTime zdtNow = ZonedDateTime.now(userZoneId);
        switch (interval) {
            case "Today":
                return zdtNow.toLocalDate().atStartOfDay(userZoneId).toInstant();
            case "Yesterday":
                return zdtNow.toLocalDate().minusDays(1).atStartOfDay(userZoneId).toInstant();
            case "ThisWeek":
                return zdtNow.toLocalDate().with(java.time.DayOfWeek.MONDAY).atStartOfDay(userZoneId).toInstant();
            case "LastWeek":
                return zdtNow.toLocalDate().minusWeeks(1).with(java.time.DayOfWeek.MONDAY).atStartOfDay(userZoneId)
                        .toInstant();
            case "ThisMonth":
                return zdtNow.toLocalDate().withDayOfMonth(1).atStartOfDay(userZoneId).toInstant();
            case "LastMonth":
                return zdtNow.toLocalDate().minusMonths(1).withDayOfMonth(1).atStartOfDay(userZoneId).toInstant();
            default: // this year default
                return zdtNow.toLocalDate().withDayOfYear(1).atStartOfDay(userZoneId).toInstant();
        }
    }

    /**
     * Computes the average working-time ratio for sessions in the selected interval.
     *
     * @param user application user
     * @param interval interval identifier
     * @param userZoneId user time zone
     * @return average working-time ratio, or {@code 0.0f} when unavailable
     */
    public Float getWorkingTimeRatio(User user, String interval, ZoneId userZoneId) {
        if (interval == null)
            interval = "ThisWeek";

        Instant intervalStart = getIntervalStart(interval, userZoneId);
        Instant intervalEnd = getIntervalend(interval, userZoneId);

        List<Float> sessionRatios = new ArrayList<>();

        for (Session session : sessionDAO.findFinishedSessionsInInterval(user, intervalStart, intervalEnd)) {
            TimeBlock mostRecentTb = session.getMostRecentTimeBlock();
            if (mostRecentTb != null && mostRecentTb.getEndDate() != null) {
                long totalGrossSeconds = Duration.between(session.getCreatedAt(), mostRecentTb.getEndDate()).getSeconds();
                long totalWorkedSeconds = session.getDuration().getSeconds();
                if (totalGrossSeconds > 0) {
                    float ratio = (float) totalWorkedSeconds / totalGrossSeconds;
                    sessionRatios.add(ratio);
                }
            }
        }

        if (sessionRatios.isEmpty()) {
            return 0.0f;
        }

        // Calculate and return the average of the ratios
        float sum = 0f;
        for (Float ratio : sessionRatios) {
            sum += ratio;
        }
        return sum / sessionRatios.size();
    }

    /**
     * Returns issue rankings by tracked time for the given user.
     *
     * @param user application user
     * @return ranked issues with an optional "Other" bucket
     */
    public List<IssueRankDTO> getRankedIssues(User user) {
        List<Object[]> queryResults = sessionDAO.getTopIssuesByTime(user);

        List<IssueRankDTO> result = new ArrayList<>();
        long otherTimeTracked = 0L;

        for (int i = 0; i < queryResults.size(); i++) {
            Object[] row = queryResults.get(i);
            Issue issue = (Issue) row[0];
            Long totalTime = (Long) row[1];
            if (totalTime == null) totalTime = 0L;

            if (i < 5) {
                result.add(new IssueRankDTO(issue.getTitle(), issue.getIssueNumber(), totalTime));
            } else {
                otherTimeTracked += totalTime;
            }
        }

        // Add Other if there are more than 5 issues
        if (otherTimeTracked > 0) {
            result.add(new IssueRankDTO("Other", 0, otherTimeTracked));
        }

        return result;
    }

    /**
     * Aggregates tracked time by issue label for the given user.
     *
     * @param user application user
     * @return tracked time per label
     */
    public List<OverviewLabelTimeDTO> getTimePerLabel(User user) {
        List<Object[]> queryResults = sessionDAO.getTimeTrackedPerLabel(user);
        List<OverviewLabelTimeDTO> toReturn = new ArrayList<>();

        for (Object[] row : queryResults) {
            Label label = (Label) row[0];
            Long totalTime = (Long) row[1];
            if (totalTime == null) totalTime = 0L;
            toReturn.add(new OverviewLabelTimeDTO(label.getTitle(), label.getColorHEX(), totalTime));
        }
        return toReturn;
    }

    /**
     * Returns export rows for sessions matching the requested filters.
     *
     * @param user application user
     * @param issueTitle optional issue title filter
     * @param repoName optional repository name filter
     * @param interval reporting interval
     * @param userZoneId user time zone
     * @return exportable session data
     */
    public List<ExportItem> getExportData(User user, String issueTitle, String repoName, String interval,
            ZoneId userZoneId) {
        Instant start = getIntervalStart(interval, userZoneId);
        Instant end = getIntervalend(interval, userZoneId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(userZoneId);

        List<Session> sessions = sessionDAO.findSessionsForExport(user, repoName, issueTitle, start, end);

        return sessions.stream()
                .map(s -> new ExportItem(
                        s.getIssue().getTitle(),
                        s.getIssue().getRepository().getName(),
                        s.getDuration().getSeconds(),
                        formatter.format(s.getCreatedAt())))
                .toList();
    }

}
