package cz.cvut.fel.thesis.controller;

import cz.cvut.fel.thesis.dto.GitHubIssueDTO;
import cz.cvut.fel.thesis.dto.IssueRequestData;
import cz.cvut.fel.thesis.model.User;
import cz.cvut.fel.thesis.service.IssueService;
import cz.cvut.fel.thesis.utils.CurrentUserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Handles issue retrieval and pin management endpoints.
 */
@RestController
@RequestMapping("/issues")
public class IssueController {

    @Autowired
    private IssueService issueService;

    @Autowired
    private CurrentUserProvider userProvider;

    /**
     * Fetches issues assigned to the authenticated user from GitHub.
     *
     * @return list of assigned issues
     */
    @GetMapping()
    public ResponseEntity<List<GitHubIssueDTO>> fetchGitHubIssues() {
        return ResponseEntity.ok(issueService.getAssignedIssues());
    }

    /**
     * Pins an issue for the current user.
     *
     * @param issueData issue identity payload
     * @param oAuth2User authenticated OAuth2 principal
     * @return pinned issue data
     */
    @PostMapping("/pin")
    public ResponseEntity<GitHubIssueDTO> pinIssue(@RequestBody IssueRequestData issueData, @AuthenticationPrincipal OAuth2User oAuth2User){
        User user = userProvider.oauthToUser(oAuth2User);
        return ResponseEntity.ok(issueService.pinIssue(issueData.issueNumber(),issueData.repo(),issueData.owner(),user));
    }

    /**
     * Unpins an issue for the current user.
     *
     * @param issueData issue identity payload
     * @param oAuth2User authenticated OAuth2 principal
     * @return unpinned issue data
     */
    @PostMapping("/unpin")
    public ResponseEntity<GitHubIssueDTO> unpinIssue(@RequestBody IssueRequestData issueData, @AuthenticationPrincipal OAuth2User oAuth2User){
        User user = userProvider.oauthToUser(oAuth2User);
        return ResponseEntity.ok(issueService.unpinIssue(issueData.issueNumber(),issueData.repo(),issueData.owner(),user));
    }

}
