package cz.cvut.fel.thesis.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.ZoneId;
import java.util.List;

import cz.cvut.fel.thesis.dto.ExportItem;
import cz.cvut.fel.thesis.dto.ExportDTO;
import cz.cvut.fel.thesis.dto.ExportRequestData;
import cz.cvut.fel.thesis.dto.suggestionDTO;
import cz.cvut.fel.thesis.model.User;
import cz.cvut.fel.thesis.service.IssueService;
import cz.cvut.fel.thesis.service.SessionService;
import cz.cvut.fel.thesis.utils.CurrentUserProvider;


/**
 * Provides export suggestions and generated export data.
 */
@RestController
@RequestMapping("/export")
public class ExportController {

    @Autowired
    private IssueService issueService;

    @Autowired
    private CurrentUserProvider userProvider;

    @Autowired
    private SessionService sessionService;

    /**
     * Returns repository name suggestions for export filtering.
     *
     * @param oAuth2User authenticated OAuth2 principal
     * @param query search text
     * @return repository suggestions
     */
    @GetMapping("/repo")
    public ResponseEntity<suggestionDTO> getRepoSuggestions(@AuthenticationPrincipal OAuth2User oAuth2User, @RequestParam String query){
        User user = userProvider.oauthToUser(oAuth2User);
        return ResponseEntity.ok(new suggestionDTO(issueService.getRepoNamesForQuery(query, user)));
    }

    /**
     * Returns issue title suggestions for export filtering.
     *
     * @param oAuth2User authenticated OAuth2 principal
     * @param query search text
     * @return issue suggestions
     */
    @GetMapping("/issue")
    public ResponseEntity<suggestionDTO> getIssueSuggestions(@AuthenticationPrincipal OAuth2User oAuth2User, @RequestParam String query){
        User user = userProvider.oauthToUser(oAuth2User);
        return ResponseEntity.ok(new suggestionDTO(issueService.getIssueNamesForQuery(query,user)));
    }    

    /**
     * Produces export data for selected filters and interval.
     *
     * @param oAuth2User authenticated OAuth2 principal
     * @param reqData request payload with export filters
     * @return export result items
     */
    @PostMapping
    public ResponseEntity<ExportDTO> getExportData(@AuthenticationPrincipal OAuth2User oAuth2User, @RequestBody ExportRequestData reqData){
        User user = userProvider.oauthToUser(oAuth2User);
        ZoneId zoneId = ZoneId.of(reqData.zoneId() != null ? reqData.zoneId() : "UTC");
        List<ExportItem> exportItems = sessionService.getExportData(user, reqData.issueTitle(), reqData.repoName(), reqData.interval(), zoneId);
        return ResponseEntity.ok(new ExportDTO(exportItems));
    }
}