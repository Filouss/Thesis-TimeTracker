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

/**
 * Exposes session lifecycle and history endpoints for time tracking.
 */
@RestController
@RequestMapping("/session")
public class SessionController {
    
    @Autowired
    private SessionService sessionService;

    @Autowired
    private CurrentUserProvider userProvider;

    @Autowired
    private IssueService issueService;

    /**
     * Starts a new tracking session for the selected issue.
     *
     * @param issueData issue identification payload
     * @param oAuth2User authenticated OAuth2 principal
     */
    @PostMapping("/start")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void startSession(@Valid @RequestBody IssueRequestData issueData, @AuthenticationPrincipal OAuth2User oAuth2User){
        User user = userProvider.oauthToUser(oAuth2User);
        sessionService.startSession(issueData.issueNumber(), issueData.repo(), issueData.owner(), user);
    }

    /**
     * Ends the currently active session and stores optional notes.
     *
     * @param oAuth2User authenticated OAuth2 principal
     * @param noteDto notes payload
     * @throws NotActiveException when the user has no active session
     */
    @PostMapping("/end")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void endSession(@AuthenticationPrincipal OAuth2User oAuth2User, @RequestBody NotesDTO noteDto) throws NotActiveException {
        User user = userProvider.oauthToUser(oAuth2User);
        sessionService.endSession(user, noteDto.notes());
    }

    /**
     * Checks whether tracking is currently active for the user.
     *
     * @param oAuth2User authenticated OAuth2 principal
     * @return {@code true} when user is tracking, otherwise {@code false}
     */
    @GetMapping("/tracking")
    public boolean isTracking(@AuthenticationPrincipal OAuth2User oAuth2User){
        User user = userProvider.oauthToUser(oAuth2User);
        return user.isTracking();
    }


    /**
     * Returns finished tracked sessions for the current user with sorting.
     *
     * @param oAuth2User authenticated OAuth2 principal
     * @param sortBy field used for sorting
     * @param direction sort direction
     * @return list of session DTOs
     */
    @GetMapping
    public ResponseEntity<List<SessionDTO>> getSessions(@AuthenticationPrincipal OAuth2User oAuth2User, @RequestParam(defaultValue = "createdAt") String sortBy,
    @RequestParam(defaultValue = "desc") String direction){
        User user = userProvider.oauthToUser(oAuth2User);
        List<Session> sessions = sessionService.getSessions(user, sortBy, direction);
        List<SessionDTO> sessionDTOs = sessions.stream()
                .map(SessionDTO::fromEntity)
                .toList();
    return ResponseEntity.ok(sessionDTOs);
    }

    /**
     * Synchronizes a session with GitHub
     *
     * @param syncReq synchronization payload
     * @param zoneId user time zone id
     * @param oAuth2User authenticated OAuth2 principal
     */
    @PostMapping("/sync")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void syncSession(@RequestBody SyncSessionRequest syncReq, @RequestParam String zoneId, @AuthenticationPrincipal OAuth2User oAuth2User){
        User user = userProvider.oauthToUser(oAuth2User);
        ZoneId userZoneId = ZoneId.of(zoneId);
        sessionService.syncSession(syncReq.sessionId(), syncReq.notes(), user, userZoneId);
    }

    /**
     * Pauses the currently active tracking session.
     *
     * @param oAuth2User authenticated OAuth2 principal
     * @throws NotActiveException when the user has no active session
     */
    @PostMapping("/pause")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void pauseSession(@AuthenticationPrincipal OAuth2User oAuth2User) throws NotActiveException {
        User user = userProvider.oauthToUser(oAuth2User);
        sessionService.pauseSession(user);

    }

    /**
     * Resumes a paused tracking session.
     *
     * @param oAuth2User authenticated OAuth2 principal
     * @throws NotActiveException when the user has no active session
     */
    @PostMapping("/resume")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resumeSession(@AuthenticationPrincipal OAuth2User oAuth2User) throws NotActiveException {
        User user = userProvider.oauthToUser(oAuth2User);
        sessionService.resumeSession(user);
    }

    /**
     * Deletes a specific session and updates related aggregates.
     *
     * @param id session id
     * @param oAuth2User authenticated OAuth2 principal
     * @param zoneId user time zone id
     */
    @DeleteMapping("/{id}/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSession(@PathVariable Long id, @AuthenticationPrincipal OAuth2User oAuth2User, @RequestParam String zoneId){
        User user = userProvider.oauthToUser(oAuth2User);
        ZoneId userZoneId = ZoneId.of(zoneId);
        sessionService.deleteSession(user, id, userZoneId);
    }

    /**
     * Updates an existing session.
     *
     * @param oAuth2User authenticated OAuth2 principal
     * @param id session id
     * @param updateSessionRequest update payload
     * @return updated session DTO when found, otherwise 404
     */
    @PutMapping("/{id}/update")
    public ResponseEntity<SessionDTO> editSession(@AuthenticationPrincipal OAuth2User oAuth2User, @PathVariable Long id, @RequestBody UpdateSessionRequest updateSessionRequest){
        User user = userProvider.oauthToUser(oAuth2User);
        Session updatedSession = sessionService.editSession(user, id, updateSessionRequest);
        if (updatedSession != null) {
            return ResponseEntity.ok(SessionDTO.fromEntity(updatedSession));
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Returns finished sessions linked to a specific GitHub issue id.
     *
     * @param oAuth2User authenticated OAuth2 principal
     * @param id GitHub issue id
     * @param sortBy field used for sorting
     * @param direction sort direction
     * @return list of session DTOs for the issue
     */
    @GetMapping("/issue/{id}")
    public ResponseEntity<List<SessionDTO>> getSessionsForIssue(@AuthenticationPrincipal OAuth2User oAuth2User, @PathVariable Long id, @RequestParam(defaultValue = "createdAt") String sortBy,
    @RequestParam(defaultValue = "desc") String direction){
        User user = userProvider.oauthToUser(oAuth2User);
        Issue issue = issueService.getByGitHubID(id);
        return ResponseEntity.ok(sessionService.getSessionDTOsForIssue(issue, user,  sortBy, direction));
    }

}
