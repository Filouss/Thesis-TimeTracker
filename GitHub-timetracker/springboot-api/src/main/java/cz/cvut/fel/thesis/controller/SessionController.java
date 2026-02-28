package cz.cvut.fel.thesis.controller;

import cz.cvut.fel.thesis.dto.*;
import cz.cvut.fel.thesis.model.Session;
import cz.cvut.fel.thesis.model.User;
import cz.cvut.fel.thesis.service.IssueService;
import cz.cvut.fel.thesis.service.SessionService;
import cz.cvut.fel.thesis.service.UserService;
import cz.cvut.fel.thesis.utils.GitHubIdConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.io.NotActiveException;
import java.util.List;

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
    public void startSession(@RequestBody IssueRequestData issueData, @AuthenticationPrincipal OAuth2User oAuth2User){
        User user = userService.getUserByGitHubID(GitHubIdConverter.IdToLong(oAuth2User));
        if (user == null) {
            //todo handle with 401
            throw new RuntimeException("User not found");
        }
        if (user.isTracking()){
            try {
                sessionService.endSession(user);
            } catch (NotActiveException e) {
                throw new RuntimeException(e);
            }
        }
        sessionService.startSession(issueData.issueNumber(), issueData.repo(), issueData.owner(), user);

    }

    @PostMapping("/end")
    public void endSession(@AuthenticationPrincipal OAuth2User oAuth2User){
        User user = userService.getUserByGitHubID(GitHubIdConverter.IdToLong(oAuth2User));
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        try {
            sessionService.endSession(user);
        } catch (NotActiveException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/tracking")
    public boolean isTracking(@AuthenticationPrincipal OAuth2User oAuth2User){
        User user = userService.getUserByGitHubID(GitHubIdConverter.IdToLong(oAuth2User));
        if(user == null){
            throw new RuntimeException("User not found in DB");
        }
        return user.isTracking();
    }


    @GetMapping
    public ResponseEntity<List<SessionDTO>> getSessions(@AuthenticationPrincipal OAuth2User oAuth2User){
        User user = userService.getUserByGitHubID(GitHubIdConverter.IdToLong(oAuth2User));
        List<Session> sessions = sessionService.getSessions(user);
        List<SessionDTO> sessionDTOs = sessions.stream()
                .map(session -> new SessionDTO(
                        session.getId(),
                        session.isSynced(),
                        session.getTimeBlocks().stream()
                                .map(tb -> new TimeBlockDTO(
                                        tb.getId(),
                                        tb.getStartDate(),
                                        tb.getEndDate()
                                ))
                                .toList(),
                        new IssueDTO(
                                session.getIssue().getId(),
                                session.getIssue().getTitle(),
                                session.getIssue().getIssueNumber(),
                                session.getIssue().getGithubId(),
                                session.getIssue().getLabels().stream()
                                        .map(label -> new LabelDTO(
                                                label.getId(),
                                                label.getTitle(),
                                                label.getColorHEX()
                                        )).toList()
                        ),
                        session.isActive()
                ))
                .toList();
    return ResponseEntity.ok(sessionDTOs);
    }

    public void syncSession(@RequestBody Long sessionId, String notes, @AuthenticationPrincipal OAuth2User oAuth2User){
        User user = userService.getUserByGitHubID(GitHubIdConverter.IdToLong(oAuth2User));
        sessionService.syncSession(sessionId, notes, user);
    }
}
