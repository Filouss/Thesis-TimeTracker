package cz.cvut.fel.thesis.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
public class UserController {

    @Autowired
    private WebClient github;

    @GetMapping("/me")
    public Object me(@AuthenticationPrincipal OAuth2User user) {
        return user.getAttributes();
    }

    @GetMapping("/api/github/me")
    public Object githubMe() {
        return github.get()
                .uri("/user")
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }
}
