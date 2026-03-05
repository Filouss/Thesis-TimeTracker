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
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private SyncFormatService syncFormatService;

    @Autowired
    private WebClient github;

    @Transactional
    public void startSession(int issueNumber, String repo, String owner, User user) {
        if (user.isTracking()){
            try {
                endSession(user);
            } catch (NotActiveException e) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "No active session");
            }
        }

        GitHubIssueDTO fetchedIssue = issueService.getIssue(issueNumber, repo, owner);
        System.out.println("login " + user.getUsername() + " gh assignee "+ fetchedIssue.assignee().login());
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
    public void endSession(User user) throws NotActiveException {
        pauseSession(user);
        Session session = sessionDAO
                .findById(user.getActiveSessionID())
                .orElseThrow(NotActiveException::new);

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

        if (session.isPaused()){return;}
        session.setPaused(true);
        TimeBlock tb = session.getMostRecentTimeBlock();
        tb.setEndDate(LocalDateTime.now());
        timeBlockDAO.save(tb);
        sessionDAO.save(session);
    }

    @Transactional
    public void resumeSession(User user) throws NotActiveException {
        Session session = sessionDAO
                .findById(user.getActiveSessionID())
                .orElseThrow(NotActiveException::new);

        if (!session.isPaused()){return;}
        createTimeBlock(session);
        sessionDAO.save(session);
    }

    @Transactional
    public void deleteSession(User user, Long sessionId) {
        Session session = sessionDAO.findByIdAndUser(sessionId, user).orElse(null);
        if (session == null){return;}
        sessionDAO.delete(session);
    }

    @Transactional
    public Session editSession(User user, Long sessionId, UpdateSessionRequest sessionUpdate) {
        Session existingSession = sessionDAO.findByIdAndUser(sessionId, user).orElse(null);
        UpdateIssueRequest updateIssue = sessionUpdate.issue();
        if (existingSession == null){return null;};
        existingSession.setNotes(sessionUpdate.notes());
        Issue toSet = issueDAO.findByGithubId(sessionUpdate.issue().gitHubId()).orElse(null);
        if (toSet != null){
            existingSession.setIssue(toSet);
        } else {
            GitHubIssueDTO fetchedIssue = issueService.getIssue(updateIssue.issueNumber(), updateIssue.repoName(), updateIssue.repoOwner());
            if (fetchedIssue.assignee() == null || !fetchedIssue.assignee().login().equals(user.getUsername())) {
                throw new UnassignedIssueException(HttpStatus.BAD_REQUEST, "Can't create a session for an unassigned issue");
            }
            Repository repository = getOrCreateRepository(fetchedIssue.repoName(), fetchedIssue.repoOwnerFromUrl());
            Set<Label> labels = getOrCreateLabels(fetchedIssue);
            Issue toSave = issueService.getOrCreateIssue(updateIssue.issueNumber(),fetchedIssue,repository,labels);
            existingSession.setIssue(toSave);
        }
        if (sessionUpdate.timeBlocks() != null){
            replaceTimeBlocks(existingSession, sessionUpdate.timeBlocks());
        }
        return sessionDAO.save(existingSession);
    }

    private void replaceTimeBlocks(Session existingSession, List<UpdateTimeBlockRequest> updateTimeBlockRequests) {
        // Existing blocks by id
        Map<Long, TimeBlock> existingById = existingSession.getTimeBlocks().stream()
                .filter(tb -> tb.getId() != null)
                .collect(Collectors.toMap(TimeBlock::getId, Function.identity()));

        // IDs we want to keep
        Set<Long> keepIds = new HashSet<>();

        //final set to add to session
        Set<TimeBlock> finalBlocks = new HashSet<>();

        for (UpdateTimeBlockRequest dto : updateTimeBlockRequests) {
            if (dto.startDate() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "timeBlocks.startDate is required");
            }
            if (dto.endDate() != null && dto.endDate().isBefore(dto.startDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "timeBlocks.endDate must be >= startDate");
            }

            TimeBlock tb;

            if (dto.id() == null) {
                // CREATE new block
                tb = new TimeBlock();
            } else {
                // UPDATE existing block
                tb = existingById.get(dto.id());
                if (tb == null) {
                    // client sent an id that isn't in this session
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown timeBlock id: " + dto.id());
                }
                keepIds.add(dto.id());
            }

            tb.setStartDate(dto.startDate());
            tb.setEndDate(dto.endDate());
            tb.setSession(existingSession);

            finalBlocks.add(tb);
        }

        // DELETE removed blocks
        existingSession.getTimeBlocks().clear();
        existingSession.getTimeBlocks().addAll(finalBlocks);
    }

    private void createTimeBlock(Session session) {
        TimeBlock timeBlock = new TimeBlock();
        timeBlock.setStartDate(LocalDateTime.now());
        timeBlock.setSession(session);
        session.getTimeBlocks().add(timeBlock);
        sessionDAO.save(session);
        timeBlockDAO.save(timeBlock);
    }

    private Session createSession(User user, Issue issue) {
        Session session = new Session();
        session.setPaused(false);
        session.setIssue(issue);
        session.setSynced(false);
        session.setUser(user);
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

        for (LabelDTO labelDTO : fetchedIssue.labels()){
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

    public List<Session> getSessions(User user){
        return sessionDAO.findByUser(user);
    }

    public void syncSession(Long sessionId, String notes, User user){
        Session toSync = sessionDAO.findByIdAndUser(sessionId,user).orElse(null);
        if (toSync == null){return;};
        Long commentId = toSync.getIssue().getGithubCommentId();
        if (commentId == null){
            //add a comment
            addSessionComment(toSync,notes);
        } else {
            //edit existing comment
            editSessionComment(toSync,notes, commentId);
        }
        toSync.setNotes(notes);
        toSync.setSynced(true);
        sessionDAO.save(toSync);
    }

    private void editSessionComment(Session toSync, String notes, Long commentId) {
        toSync.setNotes(notes);
        sessionDAO.save(toSync);
        String body = syncFormatService.buildSessionComments(toSync.getIssue());

        if (commentId == null){
            addSessionComment(toSync,notes);
            return;
        }

        try {
            github.patch()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repo}/issues/{issueNumber}/comments/{commentId}")
                            .build(toSync.getIssue().getRepository().getOwner(),toSync.getIssue().getRepository().getName() , toSync.getIssue().getIssueNumber(), commentId)
                    )
                    .bodyValue(Map.of("body",body))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class).map(b ->
                                    new RuntimeException("GitHub " + resp.statusCode() + " body: " + b)
                            )
                    )
                    .bodyToMono(CommentDTO.class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            //comment deleted or inaccessible
            //todo refactor to a method in webclient
            github.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repo}/issues/{issueNumber}/comments")
                            .build(toSync.getIssue().getRepository().getOwner(),toSync.getIssue().getRepository().getName() , toSync.getIssue().getIssueNumber())
                    )
                    .bodyValue(Map.of("body", body))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class).map(b ->
                                    new RuntimeException("GitHub " + resp.statusCode() + " body: " + b)
                            )
                    )
                    .bodyToMono(CommentDTO.class)
                    .block();
        }
    }


    private void addSessionComment(Session toSync, String notes) {
        Duration duration = toSync.getDuration();
        String body = syncFormatService.buildNewComment(toSync, notes, duration);

        CommentDTO commentDTO = github.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/issues/{issueNumber}/comments")
                        .build(toSync.getIssue().getRepository().getOwner(),toSync.getIssue().getRepository().getName() , toSync.getIssue().getIssueNumber())
                )
                .bodyValue(Map.of("body", body))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class).map(b ->
                                new RuntimeException("GitHub " + resp.statusCode() + " body: " + b)
                        )
                )
                .bodyToMono(CommentDTO.class)
                .block();
        if (commentDTO != null) {
            toSync.getIssue().setGithubCommentId(commentDTO.id());
            issueDAO.save(toSync.getIssue());
        }
    }

}