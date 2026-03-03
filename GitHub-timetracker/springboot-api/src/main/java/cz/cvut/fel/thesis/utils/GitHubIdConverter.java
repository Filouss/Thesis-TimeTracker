package cz.cvut.fel.thesis.utils;

import org.springframework.security.oauth2.core.user.OAuth2User;

public class GitHubIdConverter {
    public static Long userIdToLong(OAuth2User oAuth2User){
        Number idNum = oAuth2User.getAttribute("id");
        Long githubID = idNum == null ? null : idNum.longValue();
        return githubID;
    }
}
