package cz.cvut.fel.thesis.service;

import cz.cvut.fel.thesis.dao.IssueDAO;
import cz.cvut.fel.thesis.dao.LabelDAO;
import cz.cvut.fel.thesis.dao.RepositoryDAO;
import cz.cvut.fel.thesis.dao.SessionDAO;
import cz.cvut.fel.thesis.dao.UserDAO;
import cz.cvut.fel.thesis.dto.GitHubIssueDTO;
import cz.cvut.fel.thesis.dto.LabelDTO;
import cz.cvut.fel.thesis.exceptions.UnassignedIssueException;
import cz.cvut.fel.thesis.model.*;
import cz.cvut.fel.thesis.client.GitHubAPIClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Handles GitHub issue retrieval, persistence, and user issue actions.
 */
@Service
public class IssueService {

    @Autowired
    private IssueDAO issueDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private RepositoryDAO repositoryDAO;

    @Autowired
    private SessionDAO sessionDAO;

    @Autowired
    private GitHubAPIClient gitHubAPIClient;

    @Autowired
    private LabelDAO labelDAO;

    /**
     * Returns issues currently assigned to the authenticated user from GitHub.
     *
     * @return assigned GitHub issues
     */
    public List<GitHubIssueDTO> getAssignedIssues() {
        List<GitHubIssueDTO> searchResponse = gitHubAPIClient.getAssignedIssues();

        if (searchResponse == null || searchResponse.isEmpty()) { return List.of();}

        return searchResponse;
    }

    /**
     * Fetches the GitHub issue from given params
     *
     * @param issueNumber issue number in the repository
     * @param repo repository name
     * @param owner repository owner
     * @return GitHub issue DTO
     */
    public GitHubIssueDTO getIssue(int issueNumber, String repo, String owner) {
        return gitHubAPIClient.getIssue(issueNumber, repo, owner);
    }

    /**
     * Pins an issue for the given user
     *
     * @param issueNumber issue number in the repository
     * @param repo repository name
     * @param owner repository owner
     * @param user application user
     * @return pinned issue DTO
     */
    public GitHubIssueDTO pinIssue(int issueNumber, String repo, String owner, User user) {
        GitHubIssueDTO issueDTO = getIssue(issueNumber, repo, owner);
        if (issueDTO.assignees() == null || issueDTO.assignees().stream().noneMatch(assignee -> assignee.login().equals(user.getUsername()))) {
            throw new UnassignedIssueException(HttpStatus.BAD_REQUEST, "Can't pin unassigned issue");
        }
        user.getPinnedIssueGithubIds().add(issueDTO.id());
        userDAO.save(user);
        return issueDTO;
    }

    /**
     * Unpins an issue for the given user
     *
     * @param issueNumber issue number in the repository
     * @param repo repository name
     * @param owner repository owner
     * @param user application user
     * @return unpinned issue DTO
     */
    public GitHubIssueDTO unpinIssue(int issueNumber, String repo, String owner, User user) {
        GitHubIssueDTO issueDTO = getIssue(issueNumber, repo, owner);
        if (issueDTO.assignees() == null || issueDTO.assignees().stream().noneMatch(assignee -> assignee.login().equals(user.getUsername()))) {
            throw new UnassignedIssueException(HttpStatus.BAD_REQUEST, "Can't unpin unassigned issue");
        }
        user.getPinnedIssueGithubIds().remove(issueDTO.id());
        userDAO.save(user);
        return issueDTO;
    }

    /**
     * Returns an existing issue or creates a new persisted issue from GitHub data.
     *
     * @param issueNumber issue number in the repository
     * @param fetchedIssue GitHub issue payload
     * @param repository repository entity
     * @param labels issue labels
     * @return persisted issue entity
     */
    public Issue getOrCreateIssue(int issueNumber, GitHubIssueDTO fetchedIssue, Repository repository, Set<Label> labels) {
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

    /**
     * Returns the GitHub issue DTO for the issue associated with the given session id.
     *
     * @param id session id
     * @return GitHub issue DTO or {@code null} if the session or issue is missing
     */
    public GitHubIssueDTO getIssueBySessionId(Long id) {
        Session session = sessionDAO.findById(id).orElse(null);
        if (session == null) {return null;}
        Issue issue = session.getIssue();
        if (issue == null) {return null;}
        return getIssue(issue.getIssueNumber(), issue.getRepository().getName(), issue.getRepository().getOwner());
    }

    /**
     * Calculates total tracked time of finished sessions for a given issue and user.
     *
     * @param issue issue entity
     * @param user application user
     * @return total tracked seconds
     */
    public Long getTimeTrackedForIssueInSec(Issue issue, User user) {
        return sessionDAO.findByIssueAndUserAndFinishedTrue(issue, user).stream()
                .map(s -> s.getDuration().getSeconds())
                .reduce(0L, Long::sum);
    }

    /**
     * Looks up an issue by database id.
     *
     * @param id issue database id
     * @return issue entity or {@code null}
     */
    public Issue get(Long id) {
        return issueDAO.findById(id).orElse(null);
    }

    /**
     * Looks up an issue by GitHub id.
     *
     * @param githubId GitHub issue id
     * @return issue entity or {@code null}
     */
    public Issue getByGitHubID(Long githubId) {
        return issueDAO.findByGithubId(githubId).orElse(null);
    }

    /**
     * Returns issues indexed by GitHub id for the provided ids.
     *
     * @param githubIds GitHub issue ids
     * @return map of GitHub id to issue entity
     */
    public Map<Long, Issue> getByGitHubIDs(Collection<Long> githubIds) {
        if (githubIds == null || githubIds.isEmpty()) {
            return Map.of();
        }
        return issueDAO.findByGithubIdIn(githubIds).stream()
                .collect(Collectors.toMap(Issue::getGithubId, Function.identity()));
    }

    /**
     * Returns repository name suggestions for the given query.
     *
     * @param query search text
     * @param user application user
     * @return matching repository names
     */
    public List<String> getRepoNamesForQuery(String query, User user) {
        return repositoryDAO.findRepoSuggestionsForUser(user,query);
    }

    /**
     * Returns issue title suggestions for the given query.
     *
     * @param query search text
     * @param user application user
     * @return matching issue names
     */
    public List<String> getIssueNamesForQuery(String query, User user) {
        return issueDAO.findIssueSuggestionsForUser(user, query);
        
    }

    
    /**
     * Returns an existing repository or creates a new one.
     *
     * @param repoName repository name
     * @param owner repository owner
     * @return repository entity
     */
    public Repository getOrCreateRepository(String repoName, String owner) {
        return repositoryDAO
                .findByOwnerAndName(owner, repoName)
                .orElseGet(() -> {
                    Repository newRepo = new Repository();
                    newRepo.setName(repoName);
                    newRepo.setOwner(owner);
                    return repositoryDAO.save(newRepo);
                });
    }

    /**
     * Returns persisted labels for a GitHub issue, creating missing labels as needed.
     *
     * @param fetchedIssue GitHub issue payload
     * @return persisted label set
     */
    public Set<Label> getOrCreateLabels(GitHubIssueDTO fetchedIssue) {
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

    /**
     * Returns timestamp of the most recent session start
     *
     * @param issueEntity issue to get start time of most recent session for
     * @param User User entity
     * @return Instant of the most recent session start time for the given issue and user, or {@code null} if no sessions exist
     */
    public Instant getActiveSessionStartTimeForIssue(Issue issueEntity, User user) {
        List<Session> sessions = sessionDAO.findByIssueAndUserOrderByCreatedAtDesc(issueEntity, user);
        if (sessions.isEmpty()) {
            return null;
        }
        
        return sessions.get(0).getCreatedAt();
    }


}
