package cz.cvut.fel.thesis.controller;

import cz.cvut.fel.thesis.dto.*;
import cz.cvut.fel.thesis.model.Issue;
import cz.cvut.fel.thesis.model.Session;
import cz.cvut.fel.thesis.model.User;
import cz.cvut.fel.thesis.service.IssueService;
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
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/session")
public class SessionController {

    @Autowired
    private UserService userService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private CurrentUserProvider userProvider;

    @Autowired
    private IssueService issueService;

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void startSession(@Valid @RequestBody IssueRequestData issueData, @AuthenticationPrincipal OAuth2User oAuth2User){
        User user = userProvider.oauthToUser(oAuth2User);
        sessionService.startSession(issueData.issueNumber(), issueData.repo(), issueData.owner(), user);
    }

    @PostMapping("/end")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void endSession(@AuthenticationPrincipal OAuth2User oAuth2User, @RequestBody NotesDTO noteDto) throws NotActiveException {
        User user = userProvider.oauthToUser(oAuth2User);
        sessionService.endSession(user, noteDto.notes());
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
                                        )).toList(),
                                session.getIssue().getRepository().getName(),
                                session.getIssue().getRepository().getOwner()
                        ),
                        session.isPaused(),
                        session.getNotes(),
                        session.getTimeBlocks().stream()
                                .mapToLong(tb -> {
                                    if (tb.getStartDate() == null) return 0L;
                                    if (tb.getEndDate() == null) return 0L;
                                    return java.time.Duration.between(tb.getStartDate(), tb.getEndDate()).getSeconds();
                                })
                                .sum()
                ))
                .toList();
    return ResponseEntity.ok(sessionDTOs);
    }

    @PostMapping("/sync")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void syncSession(@RequestBody SyncSessionRequest syncReq, @RequestParam String zoneId, @AuthenticationPrincipal OAuth2User oAuth2User){
        User user = userProvider.oauthToUser(oAuth2User);
        ZoneId userZoneId = ZoneId.of(zoneId);
        sessionService.syncSession(syncReq.sessionId(), syncReq.notes(), user, userZoneId);
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

    @DeleteMapping("/{id}/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSession(@PathVariable Long id, @AuthenticationPrincipal OAuth2User oAuth2User){
        //TODO: sync delete with github
        User user = userProvider.oauthToUser(oAuth2User);
        sessionService.deleteSession(user, id);
    }

    @PutMapping("/{id}/update")
    public ResponseEntity<SessionDTO> editSession(@AuthenticationPrincipal OAuth2User oAuth2User, @PathVariable Long id, @RequestBody UpdateSessionRequest updateSessionRequest){
        User user = userProvider.oauthToUser(oAuth2User);
        // ZoneId userZoneId = ZoneId.of(zoneId);
        Session updatedSession = sessionService.editSession(user, id, updateSessionRequest);
        if (updatedSession != null) {
            return ResponseEntity.ok(SessionDTO.fromEntity(updatedSession));
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/issue/{id}")
    public ResponseEntity<List<SessionDTO>> getSessionsForIssue(@AuthenticationPrincipal OAuth2User oAuth2User, @PathVariable Long id){
        User user = userProvider.oauthToUser(oAuth2User);
        Issue issue = issueService.getByGitHubID(id);
        return ResponseEntity.ok(sessionService.getSessionDTOsForIssue(issue, user));
    }

}
