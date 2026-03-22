package cz.cvut.fel.thesis.service;

import cz.cvut.fel.thesis.dao.*;
import cz.cvut.fel.thesis.dto.*;
import cz.cvut.fel.thesis.exceptions.UnassignedIssueException;
import cz.cvut.fel.thesis.model.*;
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
import java.util.*;

@Service
public class SessionService {

    @Autowired
    private SessionDAO sessionDAO;

    @Autowired
    private RepositoryDAO repositoryDAO;

    @Autowired
    private IssueDAO issueDAO;

    @Autowired
    private TimeBlockDAO timeBlockDAO;

    @Autowired
    private LabelDAO labelDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private IssueService issueService;

    @Autowired
    private FormatService syncFormatService;

    @Autowired
    private WebClient github;

    @Transactional
    public void startSession(int issueNumber, String repo, String owner, User user) {
        if (user.isTracking()) {
            try {
                endSession(user, null);
            } catch (NotActiveException e) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "No active session");
            }
        }

        GitHubIssueDTO fetchedIssue = issueService.getIssue(issueNumber, repo, owner);
        if (fetchedIssue.assignee() == null ||
                !fetchedIssue.assignee().login().equals(user.getUsername())) {

            throw new UnassignedIssueException(HttpStatus.BAD_REQUEST, "Can't start a session for an unassigned issue");
        }

        Repository repository = getOrCreateRepository(repo, owner);
        Set<Label> labels = getOrCreateLabels(fetchedIssue);
        Issue issue = issueService.getOrCreateIssue(issueNumber, fetchedIssue, repository, labels);
        Session session = createSession(user, issue);
        createTimeBlock(session);
        user.setTracking(true);
        user.setActiveSessionID(session.getId());
        userDAO.save(user);
    }

    @Transactional
    public void endSession(User user, String notes) throws NotActiveException {
        pauseSession(user);
        Session session = sessionDAO
                .findById(user.getActiveSessionID())
                .orElseThrow(NotActiveException::new);
        session.setFinished(true);
        if (!notes.isEmpty()) {
            session.setNotes(notes);
        }
        sessionDAO.save(session);

        user.setActiveSessionID(null);
        user.setTracking(false);
        userDAO.save(user);
    }

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
        sessionDAO.save(session);
    }

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

    @Transactional
    public void deleteSession(User user, Long sessionId) {
        Session session = sessionDAO.findByIdAndUser(sessionId, user).orElse(null);
        if (session == null) {
            return;
        }
        sessionDAO.delete(session);
    }

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
            if (fetchedIssue.assignee() == null || !fetchedIssue.assignee().login().equals(user.getUsername())) {
                throw new UnassignedIssueException(HttpStatus.BAD_REQUEST,
                        "Can't create a session for an unassigned issue");
            }
            Issue toSet = issueDAO.findByGithubId(fetchedIssue.id()).orElse(null);
            if (toSet != null) {
                existingSession.setIssue(toSet);
            } else {
                Repository repository = getOrCreateRepository(fetchedIssue.repoName(), fetchedIssue.repoOwnerFromUrl());
                Set<Label> labels = getOrCreateLabels(fetchedIssue);
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

        // DELETE removed blocks
        existingSession.getTimeBlocks().clear();
        existingSession.getTimeBlocks().addAll(finalBlocks);
    }

    private void createTimeBlock(Session session) {
        TimeBlock timeBlock = new TimeBlock();
        timeBlock.setStartDate(Instant.now());
        timeBlock.setSession(session);
        session.getTimeBlocks().add(timeBlock);
        // sessionDAO.save(session);
        timeBlockDAO.save(timeBlock);
    }

    private Session createSession(User user, Issue issue) {
        Session session = new Session();
        session.setPaused(false);
        session.setIssue(issue);
        session.setSynced(false);
        session.setUser(user);
        session.setFinished(false);
        session = sessionDAO.save(session);
        user.setActiveSessionID(session.getId());
        userDAO.save(user);
        return session;
    }

    private Repository getOrCreateRepository(String repoName, String owner) {
        return repositoryDAO
                .findByOwnerAndName(owner, repoName)
                .orElseGet(() -> {
                    Repository newRepo = new Repository();
                    newRepo.setName(repoName);
                    newRepo.setOwner(owner);
                    return repositoryDAO.save(newRepo);
                });
    }

    private Set<Label> getOrCreateLabels(GitHubIssueDTO fetchedIssue) {
        Set<Label> labels = new HashSet<>();

        for (LabelDTO labelDTO : fetchedIssue.labels()) {
            Label label = labelDAO
                    .findByGitHubID(labelDTO.id())
                    .orElseGet(() -> {
                        Label newLabel = new Label();
                        newLabel.setGitHubID(labelDTO.id());
                        newLabel.setTitle(labelDTO.name());
                        return newLabel;
                    });
            label.setColorHEX(labelDTO.color());
            labels.add(labelDAO.save(label));
        }
        return labels;
    }

    public List<Session> getSessions(User user) {
        return sessionDAO.findByUser(user);
    }

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

    private void editSessionComment(Session toSync, String notes, Long commentId, ZoneId userZoneId) {
        toSync.setNotes(notes);
        sessionDAO.save(toSync);
        String body = syncFormatService.buildSessionComments(toSync.getIssue(), userZoneId);

        if (commentId == null) {
            addSessionComment(toSync, notes, userZoneId);
            return;
        }

        try {
            github.patch()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repo}/issues/comments/{commentId}")
                            .build(toSync.getIssue().getRepository().getOwner(),
                                    toSync.getIssue().getRepository().getName(), commentId))
                    .bodyValue(Map.of("body", body))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,
                            resp -> resp.bodyToMono(String.class)
                                    .map(b -> new RuntimeException("GitHub " + resp.statusCode() + " body: " + b)))
                    .bodyToMono(CommentDTO.class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            // comment deleted or inaccessible
            // todo refactor to a method in webclient
            github.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repo}/issues/{issueNumber}/comments")
                            .build(toSync.getIssue().getRepository().getOwner(),
                                    toSync.getIssue().getRepository().getName(), toSync.getIssue().getIssueNumber()))
                    .bodyValue(Map.of("body", body))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,
                            resp -> resp.bodyToMono(String.class)
                                    .map(b -> new RuntimeException("GitHub " + resp.statusCode() + " body: " + b)))
                    .bodyToMono(CommentDTO.class)
                    .block();
        }
    }

    private void addSessionComment(Session toSync, String notes, ZoneId userZoneId) {
        Duration duration = toSync.getDuration();
        String body = syncFormatService.buildNewComment(toSync, notes, duration, userZoneId);

        CommentDTO commentDTO = github.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/issues/{issueNumber}/comments")
                        .build(toSync.getIssue().getRepository().getOwner(),
                                toSync.getIssue().getRepository().getName(), toSync.getIssue().getIssueNumber()))
                .bodyValue(Map.of("body", body))
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        resp -> resp.bodyToMono(String.class)
                                .map(b -> new RuntimeException("GitHub " + resp.statusCode() + " body: " + b)))
                .bodyToMono(CommentDTO.class)
                .block();
        if (commentDTO != null) {
            toSync.getIssue().setGithubCommentId(commentDTO.id());
            issueDAO.save(toSync.getIssue());
        }
    }

    public List<SessionDTO> getUnsyncedDTOs(User user) {
        List<SessionDTO> toReturn = new ArrayList<>();
        for (Session session : user.getSessions()) {
            if (!session.isSynced() && session.isFinished()) {
                toReturn.add(SessionDTO.fromEntity(session));
            }
        }
        return toReturn;
    }

    public boolean isActivePaused(User user) throws NotActiveException {
        if (user.getActiveSessionID() == null) {
            throw new NotActiveException();
        }
        Session session = sessionDAO
                .findById(user.getActiveSessionID())
                .orElseThrow(NotActiveException::new);
        return session.isPaused();
    }

    public boolean allSyncedForIssue(Issue issue, User user) {
        List<Session> sessions = sessionDAO.findByIssueAndUser(issue, user);
        for (Session session : sessions) {
            if (!session.isSynced() && session.isFinished()) {
                return false;
            }
        }
        return true;
    }

    public List<SessionDTO> getSessionDTOsForIssue(Issue issue, User user) {
        List<Session> sessions = sessionDAO.findByIssueAndUser(issue, user);
        List<SessionDTO> dtos = sessions.stream()
                .map(session -> new SessionDTO(
                        session.getId(),
                        session.isSynced(),
                        session.getTimeBlocks().stream()
                                .map(tb -> new TimeBlockDTO(
                                        tb.getStartDate(),
                                        tb.getEndDate()))
                                .toList(),
                        new IssueDTO(
                                session.getIssue().getId(),
                                session.getIssue().getTitle(),
                                session.getIssue().getIssueNumber(),
                                session.getIssue().getGithubId(),
                                session.getIssue().getLabels().stream()
                                        .map(label -> new LabelDTO(
                                                label.getId(),
                                                label.getTitle(),
                                                label.getColorHEX()))
                                        .toList(),
                                session.getIssue().getRepository().getName(),
                                session.getIssue().getRepository().getOwner()),
                        session.isPaused(),
                        session.getNotes(),
                        session.getTimeBlocks().stream()
                                .mapToLong(tb -> {
                                    if (tb.getStartDate() == null)
                                        return 0L;
                                    if (tb.getEndDate() == null)
                                        return 0L;
                                    return java.time.Duration.between(tb.getStartDate(), tb.getEndDate()).getSeconds();
                                })
                                .sum()))
                .toList();

        return dtos;
    }

    public List<DailyTimeTrackDTO> secondsTrackedPerDayThisWeek(User user, ZoneId userZoneId) {
        Instant intervalStart = getIntervalStart("ThisWeek", userZoneId);
        Instant intervalEnd = getIntervalend("ThisWeek", userZoneId);
        
        Map<java.time.DayOfWeek, Long> dailyAccumulator = new EnumMap<>(java.time.DayOfWeek.class);
        for (java.time.DayOfWeek day : java.time.DayOfWeek.values()) {
            dailyAccumulator.put(day, 0L);
        }

        sessionDAO.findByUser(user).forEach(session -> {
            if (session.getTimeBlocks() != null && session.isFinished()) {
                    if (!session.getCreatedAt().isBefore(intervalStart) && session.getCreatedAt().isBefore(intervalEnd)) {
                        // Discover which day of the week this timeblock started on
                        java.time.DayOfWeek day = session.getCreatedAt().atZone(userZoneId).getDayOfWeek();
                        dailyAccumulator.put(day, dailyAccumulator.get(day) + session.getDuration().getSeconds());
                    }
                }
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



    public Long secondsTrackedForInterval(User user, String interval, ZoneId userZoneId) {
        if (interval == null) interval = "ThisWeek";

        Instant intervalStart = getIntervalStart(interval, userZoneId);
        Instant intervalEnd = getIntervalend(interval, userZoneId);

        long totalSeconds = 0;
        for (Session session : sessionDAO.findByUser(user)) {
            if (session.getTimeBlocks() != null && session.isFinished()) {
                for (TimeBlock tb : session.getTimeBlocks()) {
                    if (tb.getStartDate() != null) {
                        // Check if the timeblock started within our calculated interval
                        if (!tb.getStartDate().isBefore(intervalStart) && tb.getStartDate().isBefore(intervalEnd)) {
                            Instant tbEnd = tb.getEndDate() != null ? tb.getEndDate() : Instant.now();
                            totalSeconds += Duration.between(tb.getStartDate(), tbEnd).getSeconds();
                        }
                    }
                }
            }
        }
        return totalSeconds;
    }

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
                return zdtNow.toLocalDate().minusWeeks(1).with(java.time.DayOfWeek.MONDAY).atStartOfDay(userZoneId).toInstant();
            case "ThisMonth":
                return zdtNow.toLocalDate().withDayOfMonth(1).atStartOfDay(userZoneId).toInstant();
            case "LastMonth":
                return zdtNow.toLocalDate().minusMonths(1).withDayOfMonth(1).atStartOfDay(userZoneId).toInstant();
            default: // this year default
                return zdtNow.toLocalDate().withDayOfYear(1).atStartOfDay(userZoneId).toInstant();
        }
    }

    public Float getWorkingTimeRatio(User user, String interval, ZoneId userZoneId) {
        if (interval == null) interval = "ThisWeek";

        Instant intervalStart = getIntervalStart(interval, userZoneId);
        Instant intervalEnd = getIntervalend(interval, userZoneId);
        
        List<Float> sessionRatios = new ArrayList<>();
        
        for (Session session : sessionDAO.findByUser(user)) {
            if (session.getTimeBlocks() != null && session.isFinished()) {
                // check if this session was started within the requested interval
                Instant sessionStart = session.getCreatedAt();
                if (sessionStart != null && !sessionStart.isBefore(intervalStart) && sessionStart.isBefore(intervalEnd)) {
                    
                    TimeBlock mostRecentTb = session.getMostRecentTimeBlock();
                    if (mostRecentTb != null && mostRecentTb.getEndDate() != null) {
                        
                        long totalGrossSeconds = Duration.between(sessionStart, mostRecentTb.getEndDate()).getSeconds();
                        long totalWorkedSeconds = session.getDuration().getSeconds();
                        
                        // Prevent division by zero if a session was started and finished inside the exact same second
                        if (totalGrossSeconds > 0) {
                            // working time divided by Gross (total) time
                            float ratio = (float) totalWorkedSeconds / totalGrossSeconds;
                            sessionRatios.add(ratio);
                        }
                    }
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

    public List<IssueRankDTO> getRankedIssues(User user) {
        Map<Long, Issue> issueMap = new HashMap<>();
        Map<Long, Long> timeMap = new HashMap<>();

        for (Session session : sessionDAO.findByUser(user)) {
            // Only aggregate finished sessions
            if (session.getIssue() != null && session.isFinished()) {
                Issue issue = session.getIssue();
                long seconds = session.getDuration().getSeconds();
                issueMap.put(issue.getId(), issue);
                timeMap.put(issue.getId(), timeMap.getOrDefault(issue.getId(), 0L) + seconds);
            }
        }

        // Sort descending by time tracked
        List<Map.Entry<Long, Long>> sortedEntries = new ArrayList<>(timeMap.entrySet());
        sortedEntries.sort(Map.Entry.<Long, Long>comparingByValue().reversed());

        List<IssueRankDTO> result = new ArrayList<>();
        long otherTimeTracked = 0L;

        for (int i = 0; i < sortedEntries.size(); i++) {
            Map.Entry<Long, Long> entry = sortedEntries.get(i);
            Issue issue = issueMap.get(entry.getKey());
            
            if (i < 5) {
                result.add(new IssueRankDTO(
                        issue.getTitle(),
                        issue.getIssueNumber(),
                        entry.getValue()
                ));
            } else {
                otherTimeTracked += entry.getValue();
            }
        }

        // Add Other if there are more than 5 issues
        if (otherTimeTracked > 0) {
            result.add(new IssueRankDTO("Other", 0, otherTimeTracked));
        }

        return result;
    }

    public List<OverviewLabelTimeDTO> getTimePerLabel(User user) {
        Map<Long, Label> labelMap = new HashMap<>();
        Map<Long, Long> timeMap = new HashMap<>();

        for(Session session : sessionDAO.findByUser(user)){
            if (session.getIssue() != null && session.isFinished()) {
                Set<Label> labels = session.getIssue().getLabels();
                long seconds = session.getDuration().getSeconds();
                for(Label label : labels){
                    labelMap.put(label.getId(), label);
                    timeMap.put(label.getId(), timeMap.getOrDefault(label.getId(), 0L) + seconds);
                }
                
            }
        }

        List<OverviewLabelTimeDTO> toReturn = new ArrayList<>();
        for(Map.Entry<Long, Long> entry : timeMap.entrySet()){
            Label label = labelMap.get(entry.getKey());
            toReturn.add(new OverviewLabelTimeDTO(
                label.getTitle(),
                label.getColorHEX(),
                entry.getValue()
            ));
        }
        return toReturn;
    }



}