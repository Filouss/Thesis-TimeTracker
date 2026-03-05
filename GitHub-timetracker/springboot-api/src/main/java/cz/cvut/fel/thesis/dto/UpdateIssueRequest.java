package cz.cvut.fel.thesis.dto;

public record UpdateIssueRequest(
        Long gitHubId,
        String repoOwner,
        String repoName,
        int issueNumber
) {
}
