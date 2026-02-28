package cz.cvut.fel.thesis.service;

import cz.cvut.fel.thesis.dao.*;
import cz.cvut.fel.thesis.dto.CommentDTO;
import cz.cvut.fel.thesis.dto.GitHubIssueDTO;
import cz.cvut.fel.thesis.dto.LabelDTO;
import cz.cvut.fel.thesis.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.NotActiveException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private WebClient github;

    @Transactional
    public void startSession(int issueNumber, String repo, String owner, User user) {
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
        stopSession(user);
        Session session = sessionDAO
                .findById(user.getActiveSessionID())
                .orElseThrow(NotActiveException::new);

        session.setActive(false);
        sessionDAO.save(session);

        user.setActiveSessionID(null);
        user.setTracking(false);
        userDAO.save(user);
    }

    @Transactional
    public void stopSession(User user) throws NotActiveException {
        Session session = sessionDAO
                .findById(user.getActiveSessionID())
                .orElseThrow(NotActiveException::new);

        TimeBlock tb = session.getMostRecentTimeBlock();
        tb.setEndDate(LocalDateTime.now());
        timeBlockDAO.save(tb);
    }

    @Transactional
    public void resumeSession(User user) throws NotActiveException {
        Session session = sessionDAO
                .findById(user.getActiveSessionID())
                .orElseThrow(NotActiveException::new);

        createTimeBlock(session);
        sessionDAO.save(session);
    }

    private void createTimeBlock(Session session) {
        TimeBlock timeBlock = new TimeBlock();
        timeBlock.setStartDate(LocalDateTime.now());
        timeBlock.setSession(session);
        timeBlockDAO.save(timeBlock);
    }

    private Session createSession(User user, Issue issue) {
        Session session = new Session();
        session.setActive(true);
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
        issue.setSyncCommentsAmount(0);
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
                    //todo mozna predelat na hledani podle repa a jmena
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
        if (toSync.getIssue().getSyncCommentsAmount() < 3){
            //add a comment
            addSessionComment(toSync,notes);
        } else if (toSync.getIssue().getSyncCommentsAmount() == 3 ) {
            //aggregate to one comment
        } else {
            //edit aggregated comment
        }
        toSync.setNotes(notes);
        toSync.setSynced(true);
        sessionDAO.save(toSync);
    }

    private void addSessionComment(Session toSync, String notes) {
        StringBuilder sb = new StringBuilder();

        sb.append("• ")
                .append(toSync.getMostRecentTimeBlock().getStartDate().toLocalDate())
                .append(" — ")
                .append(toSync.getMostRecentTimeBlock().getStartDate().toLocalTime());
        if (notes != null) {
            sb.append(" — ").append(toSync.getNotes());
        }
        sb.append("\n");

        github.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/issues/{issueNumber}/comments")
                        .build(toSync.getIssue().getRepository().getOwner(),toSync.getIssue().getRepository().getName() , toSync.getIssue().getIssueNumber())
                )
                .bodyValue(Map.of("body", sb.toString()))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class).map(body ->
                                new RuntimeException("GitHub " + resp.statusCode() + " body: " + body)
                        )
                )
                .bodyToMono(CommentDTO.class)
                .block();
    }

}