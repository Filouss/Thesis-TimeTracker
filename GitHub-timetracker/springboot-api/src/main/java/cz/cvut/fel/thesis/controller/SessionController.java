package cz.cvut.fel.thesis.controller;

import cz.cvut.fel.thesis.dto.StartSessionRequest;
import cz.cvut.fel.thesis.model.User;
import cz.cvut.fel.thesis.service.IssueService;
import cz.cvut.fel.thesis.service.SessionService;
import cz.cvut.fel.thesis.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/session")
public class SessionController {

    @Autowired
    private UserService userService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private IssueService issueService;

    @PostMapping("/start")
    public void startSession(@RequestBody StartSessionRequest issueData, @AuthenticationPrincipal OAuth2User oAuth2User){
        User user = userService.getUserByGitHubID(IDtoLong(oAuth2User));
        if (user == null) {
            //todo handle with 401
            throw new RuntimeException("User not found");
        }
        if (user.isTracking()){
            //end current
        }
        sessionService.startSession(issueData.issueNumber(), issueData.repo(), issueData.owner(), user);

    }

    @GetMapping("/tracking")
    public boolean isTracking(@AuthenticationPrincipal OAuth2User oAuth2User){
        User user = userService.getUserByGitHubID(IDtoLong(oAuth2User));
        if(user == null){
            throw new RuntimeException("User not found in DB");
        }
        return user.isTracking();
    }

    private Long IDtoLong(OAuth2User oAuth2User){
        Number idNum = oAuth2User.getAttribute("id");
        Long githubID = idNum == null ? null : idNum.longValue();
        return githubID;
    }
}
