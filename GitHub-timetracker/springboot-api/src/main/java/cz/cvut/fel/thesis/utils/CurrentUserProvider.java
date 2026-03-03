package cz.cvut.fel.thesis.utils;

import cz.cvut.fel.thesis.exceptions.UserNotFoundException;
import cz.cvut.fel.thesis.model.User;
import cz.cvut.fel.thesis.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentUserProvider {
    @Autowired
    private UserService userService;

    public User oauthToUser(OAuth2User principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        Long githubId = GitHubIdConverter.userIdToLong(principal);
        User user = userService.getUserByGitHubID(githubId);
        if (user == null) {
            throw new UserNotFoundException(githubId);
        }
        return user;
    }
}
