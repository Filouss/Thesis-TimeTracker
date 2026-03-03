package cz.cvut.fel.thesis.controller;

import cz.cvut.fel.thesis.dto.GitHubIssueDTO;
import cz.cvut.fel.thesis.dto.IssueRequestData;
import cz.cvut.fel.thesis.model.User;
import cz.cvut.fel.thesis.service.IssueService;
import cz.cvut.fel.thesis.service.UserService;
import cz.cvut.fel.thesis.utils.CurrentUserProvider;
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
    private IssueService issueService;

    private final CurrentUserProvider userProvider = new CurrentUserProvider();

    @GetMapping()
    public ResponseEntity<List<GitHubIssueDTO>> fetchGitHubIssues() {
        return ResponseEntity.ok(issueService.getAssignedIssues());
    }

    @PostMapping("/pin")
    public ResponseEntity<?> pinIssue(@RequestBody IssueRequestData issueData, @AuthenticationPrincipal OAuth2User oAuth2User){
        User user = userProvider.oauthToUser(oAuth2User);
        issueService.pinIssue(issueData.issueNumber(),issueData.repo(),issueData.owner(),user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unpin")
    public ResponseEntity<?> unpinIssue(@RequestBody IssueRequestData issueData, @AuthenticationPrincipal OAuth2User oAuth2User){
        User user = userProvider.oauthToUser(oAuth2User);
        issueService.unpinIssue(issueData.issueNumber(),issueData.repo(),issueData.owner(),user);
        return ResponseEntity.ok().build();
    }

}
