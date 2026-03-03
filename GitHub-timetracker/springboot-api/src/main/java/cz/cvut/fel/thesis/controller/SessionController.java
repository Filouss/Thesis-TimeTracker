package cz.cvut.fel.thesis.controller;

import cz.cvut.fel.thesis.dto.*;
import cz.cvut.fel.thesis.model.Session;
import cz.cvut.fel.thesis.model.User;
import cz.cvut.fel.thesis.service.SessionService;
import cz.cvut.fel.thesis.service.UserService;
import cz.cvut.fel.thesis.utils.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    private final CurrentUserProvider userProvider = new CurrentUserProvider();

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void startSession(@Valid @RequestBody IssueRequestData issueData, @AuthenticationPrincipal OAuth2User oAuth2User){
        User user = userProvider.oauthToUser(oAuth2User);
        sessionService.startSession(issueData.issueNumber(), issueData.repo(), issueData.owner(), user);
    }

    @PostMapping("/end")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void endSession(@AuthenticationPrincipal OAuth2User oAuth2User) throws NotActiveException {
        User user = userProvider.oauthToUser(oAuth2User);
        sessionService.endSession(user);
    }

    @GetMapping("/tracking")
    public boolean isTracking(@AuthenticationPrincipal OAuth2User oAuth2User){
        User user = userProvider.oauthToUser(oAuth2User);
        return user.isTracking();
    }


    @GetMapping
    public ResponseEntity<List<SessionDTO>> getSessions(@AuthenticationPrincipal OAuth2User oAuth2User){
        User user = userProvider.oauthToUser(oAuth2User);
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
                        session.isPaused()
                ))
                .toList();
    return ResponseEntity.ok(sessionDTOs);
    }

    @PostMapping("/sync")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void syncSession(@RequestBody SyncSessionRequest syncReq, @AuthenticationPrincipal OAuth2User oAuth2User){
        User user = userProvider.oauthToUser(oAuth2User);
        sessionService.syncSession(syncReq.sessionId(), syncReq.notes(), user);
    }

    @PostMapping("/pause")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void pauseSession(@AuthenticationPrincipal OAuth2User oAuth2User) throws NotActiveException {
        User user = userProvider.oauthToUser(oAuth2User);
        sessionService.pauseSession(user);

    }

    @PostMapping("/resume")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resumeSession(@AuthenticationPrincipal OAuth2User oAuth2User) throws NotActiveException {
        User user = userProvider.oauthToUser(oAuth2User);
        sessionService.resumeSession(user);
    }


    //todo

    @PostMapping("/{id}/delete")
    public void deleteSession(@PathVariable Long id, @AuthenticationPrincipal OAuth2User oAuth2User){

    }

    @PostMapping
    public void editSession(@AuthenticationPrincipal OAuth2User oAuth2User){

    }
}
