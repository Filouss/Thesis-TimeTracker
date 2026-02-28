package cz.cvut.fel.thesis.controller;

import cz.cvut.fel.thesis.dto.GitHubIssueDTO;
import cz.cvut.fel.thesis.dto.IssueRequestData;
import cz.cvut.fel.thesis.model.User;
import cz.cvut.fel.thesis.service.IssueService;
import cz.cvut.fel.thesis.service.UserService;
import cz.cvut.fel.thesis.utils.GitHubIdConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@RestController
@RequestMapping("/issues")
public class IssueController {

    @Autowired
    private WebClient github;

    @Autowired
    private IssueService issueService;

    @Autowired
    private UserService userService;


    @GetMapping()
    public ResponseEntity<List<GitHubIssueDTO>> fetchGitHubIssues() {
        return ResponseEntity.ok(issueService.getAssignedIssues());
    }

    @GetMapping("/raw")
    public String issuesRaw() {
        return github.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/issues")
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
                .bodyToMono(String.class)
                .block();
    }

    @PostMapping("/pin")
    public ResponseEntity<?> pinIssue(@RequestBody IssueRequestData issueData, @AuthenticationPrincipal OAuth2User oAuth2User){
        User user = userService.getUserByGitHubID(GitHubIdConverter.IdToLong(oAuth2User));
        issueService.pinIssue(issueData.issueNumber(),issueData.repo(),issueData.owner(),user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unpin")
    public ResponseEntity<?> unpinIssue(@RequestBody IssueRequestData issueData, @AuthenticationPrincipal OAuth2User oAuth2User){
        User user = userService.getUserByGitHubID(GitHubIdConverter.IdToLong(oAuth2User));
        issueService.unpinIssue(issueData.issueNumber(),issueData.repo(),issueData.owner(),user);
        return ResponseEntity.ok().build();
    }

}
