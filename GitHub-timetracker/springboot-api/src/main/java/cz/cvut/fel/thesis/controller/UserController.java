package cz.cvut.fel.thesis.controller;

import cz.cvut.fel.thesis.model.User;
import cz.cvut.fel.thesis.service.UserService;
import cz.cvut.fel.thesis.utils.CurrentUserProvider;
import cz.cvut.fel.thesis.utils.GitHubIdConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Set;

/**
 * Provides user-related endpoints for authenticated requests.
 */
@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private WebClient github;

    @Autowired
    private UserService userService;

    @Autowired
    private CurrentUserProvider userProvider;

    /**
     * Returns attributes of the current authenticated OAuth2 user.
     *
     * @param user authenticated OAuth2 principal
     * @return raw user attributes map
     */
    @GetMapping("/me")
    public Object me(@AuthenticationPrincipal OAuth2User user) {
        return user.getAttributes();
    }

    /**
     * Fetches profile information directly from GitHub API.
     *
     * @return GitHub user profile payload
     */
    @GetMapping("/github/me")
    public Object githubMe() {
        return github.get()
                .uri("/user")
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }

    /**
     * Returns ids of issues pinned by the current user.
     *
     * @param oAuth2User authenticated OAuth2 principal
     * @return set of pinned GitHub issue ids
     */
    @GetMapping("/pinnedIds")
    public ResponseEntity<Set<Long>> fetchPinnedIssues(@AuthenticationPrincipal OAuth2User oAuth2User) {
        User user = userProvider.oauthToUser(oAuth2User);
        return new ResponseEntity<>(userService.getPinnedIssueGitHubIds(user), HttpStatus.OK);
    }

}
