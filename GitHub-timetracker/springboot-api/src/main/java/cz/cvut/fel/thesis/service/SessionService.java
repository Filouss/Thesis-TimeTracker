package cz.cvut.fel.thesis.service;

import cz.cvut.fel.thesis.dao.*;
import cz.cvut.fel.thesis.dto.CommentDTO;
import cz.cvut.fel.thesis.dto.GitHubIssueDTO;
import cz.cvut.fel.thesis.dto.LabelDTO;
import cz.cvut.fel.thesis.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.io.NotActiveException;
import java.time.Duration;
import java.time.LocalDateTime;
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

        Repository repository = getOrCreateRepository(fetchedIssue);
        Set<Label> labels = getOrCreateLabels(fetchedIssue);
        Issue issue = getOrCreateIssue(issueNumber, fetchedIssue, repository, labels);
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

    private Issue getOrCreateIssue(int issueNumber, GitHubIssueDTO fetchedIssue, Repository repository, Set<Label> labels) {
        Issue issue = issueDAO
                .findByGithubId(fetchedIssue.id())
                .orElseGet(() -> {
                    Issue newIssue = new Issue();
                    newIssue.setGithubId(fetchedIssue.id());
                    return newIssue;
                });

        issue.setIssueNumber(issueNumber);
        issue.setTitle(fetchedIssue.title());
        issue.setState(
                "open".equalsIgnoreCase(fetchedIssue.state())
                        ? State.OPEN
                        : State.CLOSED
        );
        issue.setRepository(repository);
        issue.setLabels(labels);
        return issueDAO.save(issue);
    }

    private Repository getOrCreateRepository(GitHubIssueDTO fetchedIssue) {
        Repository repository = repositoryDAO
                .findByOwnerAndName(fetchedIssue.owner(), fetchedIssue.repoName())
                .orElseGet(() -> {
                    Repository newRepo = new Repository();
                    newRepo.setName(fetchedIssue.repoName());
                    newRepo.setOwner(fetchedIssue.owner());
                    return repositoryDAO.save(newRepo);
                });
        return repository;
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
        Session toSync = sessionDAO.findByIdAndUser(sessionId,user);
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