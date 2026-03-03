package cz.cvut.fel.thesis.service;

import cz.cvut.fel.thesis.dao.IssueDAO;
import cz.cvut.fel.thesis.dao.UserDAO;
import cz.cvut.fel.thesis.dto.GitHubIssueDTO;
import cz.cvut.fel.thesis.dto.IssueSearchResponseDTO;
import cz.cvut.fel.thesis.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class IssueService {

    @Autowired
    private WebClient github;

    @Autowired
    private IssueDAO issueDao;

    @Autowired
    private UserDAO userDAO;

    public List<GitHubIssueDTO> getAssignedIssues() {
        IssueSearchResponseDTO searchResponse = github.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/issues")
                        //change to assigned
                        .queryParam("q", "assigned:@me")
                        .queryParam("per_page", 100)
                        .build()
                )
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class).map(body ->
                                new RuntimeException("GitHub " + resp.statusCode() + " body: " + body)
                        )
                )
                .bodyToMono(IssueSearchResponseDTO.class)
                .block();

        if (searchResponse == null || searchResponse.items().isEmpty()) { return List.of();}

        return searchResponse.items();
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

    public void pinIssue(int issueNumber, String repo, String owner, User user) {
        GitHubIssueDTO issueDTO = getIssue(issueNumber, repo, owner);
        //todo handle fetching out of auth scope issue and return the issue maybe for frontend
        user.getPinnedIssueGithubIds().add(issueDTO.id());
        userDAO.save(user);
    }

    public void unpinIssue(int issueNumber, String repo, String owner, User user) {
        GitHubIssueDTO issueDTO = getIssue(issueNumber, repo, owner);
        user.getPinnedIssueGithubIds().remove(issueDTO.id());
        userDAO.save(user);
    }
}
