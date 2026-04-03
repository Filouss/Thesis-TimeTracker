package cz.cvut.fel.thesis.utils;

import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Helper class for converting GitHub OAuth attributes into typed identifiers.
 */
public class GitHubIdConverter {
    /**
     * Extracts GitHub user id from an OAuth2 principal.
     *
     * @param oAuth2User authenticated OAuth2 principal
     * @return GitHub user id, or {@code null} when unavailable
     */
    public static Long userIdToLong(OAuth2User oAuth2User){
        Number idNum = oAuth2User.getAttribute("id");
        Long githubID = idNum == null ? null : idNum.longValue();
        return githubID;
    }
}
