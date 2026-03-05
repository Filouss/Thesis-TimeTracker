package cz.cvut.fel.thesis.service;

import cz.cvut.fel.thesis.dao.IssueDAO;
import cz.cvut.fel.thesis.dao.UserDAO;
import cz.cvut.fel.thesis.dto.GitHubIssueDTO;
import cz.cvut.fel.thesis.dto.IssueSearchResponseDTO;
import cz.cvut.fel.thesis.exceptions.UnassignedIssueException;
import cz.cvut.fel.thesis.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
public class IssueService {

    @Autowired
    private WebClient github;

    @Autowired
    private IssueDAO issueDAO;

    @Autowired
    private UserDAO userDAO;

    public List<GitHubIssueDTO> getAssignedIssues() {
        List<GitHubIssueDTO> searchResponse = github.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/issues")
                        .queryParam("filter", "assigned")
                        .queryParam("per_page", 100)
                        .build()
                )
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class).map(body ->
                                new RuntimeException("GitHub " + resp.statusCode() + " body: " + body)
                        )
                )
                .bodyToFlux(GitHubIssueDTO.class)
                .collectList()
                .block();

        if (searchResponse == null || searchResponse.isEmpty()) { return List.of();}

        return searchResponse;
    }

    public GitHubIssueDTO getIssue(int issueNumber, String repo, String owner) {
        return github.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/issues/{issueNumber}")
                        .build(owner,repo,issueNumber)
                )
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class).map(body ->
                                new RuntimeException("GitHub " + resp.statusCode() + " body: " + body)
                        )
                )
                .bodyToMono(GitHubIssueDTO.class)
                .block();
    }

    public GitHubIssueDTO pinIssue(int issueNumber, String repo, String owner, User user) {
        GitHubIssueDTO issueDTO = getIssue(issueNumber, repo, owner);
        if (issueDTO.assignee() == null || !issueDTO.assignee().login().equals(user.getUsername())) {
            throw new UnassignedIssueException(HttpStatus.BAD_REQUEST, "Can't pin unassigned issue");
        }
        user.getPinnedIssueGithubIds().add(issueDTO.id());
        userDAO.save(user);
        return issueDTO;
    }

    public GitHubIssueDTO unpinIssue(int issueNumber, String repo, String owner, User user) {
        GitHubIssueDTO issueDTO = getIssue(issueNumber, repo, owner);
        if (issueDTO.assignee() == null || !issueDTO.assignee().login().equals(user.getUsername())) {
            throw new UnassignedIssueException(HttpStatus.BAD_REQUEST, "Can't unpin unassigned issue");
        }
        user.getPinnedIssueGithubIds().remove(issueDTO.id());
        userDAO.save(user);
        return issueDTO;
    }

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
}
