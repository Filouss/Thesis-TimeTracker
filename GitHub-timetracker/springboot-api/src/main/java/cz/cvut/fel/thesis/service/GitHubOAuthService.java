package cz.cvut.fel.thesis.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * Loads GitHub OAuth users and ensures they exist in the local database.
 */
@Service
public class GitHubOAuthService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    @Autowired
    private UserService userService;

    /**
     * Loads the GitHub user, ensures a local user exists, and returns the OAuth2 principal.
     *
     * @param userRequest OAuth2 user request
     * @return authenticated GitHub user
     * @throws OAuth2AuthenticationException when the OAuth2 user cannot be loaded
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();

        OAuth2User oauthUser = delegate.loadUser(userRequest);

        Number idNum = oauthUser.getAttribute("id");
        Long githubId = idNum == null ? null : idNum.longValue();
        String username = oauthUser.getAttribute("login");

        userService.findOrCreateAndGetUser(githubId, username);

        return oauthUser;
    }
}
