package cz.cvut.fel.thesis.service;

import cz.cvut.fel.thesis.dao.IssueDAO;
import cz.cvut.fel.thesis.dao.RepositoryDAO;
import cz.cvut.fel.thesis.dao.SessionDAO;
import cz.cvut.fel.thesis.dao.UserDAO;
import cz.cvut.fel.thesis.dto.GitHubIssueDTO;
import cz.cvut.fel.thesis.exceptions.UnassignedIssueException;
import cz.cvut.fel.thesis.model.*;
import cz.cvut.fel.thesis.client.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;


import java.util.List;
import java.util.Set;

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
        if (issueDTO.assignee() == null || !issueDTO.assignee().login().equals(user.getUsername())) {
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
        if (issueDTO.assignee() == null || !issueDTO.assignee().login().equals(user.getUsername())) {
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
     * Calculates total tracked time for a given issue and user.
     *
     * @param issue issue entity
     * @param user application user
     * @return total tracked seconds
     */
    public Long getTimeTrackedForIssueInSec(Issue issue, User user) {
        return sessionDAO.findByIssueAndUser(issue, user).stream()
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

}
