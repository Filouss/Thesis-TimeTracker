package cz.cvut.fel.thesis.service;

import cz.cvut.fel.thesis.dao.IssueDAO;
import cz.cvut.fel.thesis.dto.GitHubIssueDTO;
import cz.cvut.fel.thesis.dto.IssueSearchResponseDTO;
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

    public List<GitHubIssueDTO> getAssignedIssues() {
        IssueSearchResponseDTO searchResponse = github.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/issues")
                        //change to assigned
                        .queryParam("q", "involves:@me")
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
}
