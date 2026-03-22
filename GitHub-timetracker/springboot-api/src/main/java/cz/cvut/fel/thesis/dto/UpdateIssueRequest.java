package cz.cvut.fel.thesis.dto;

public record UpdateIssueRequest(
        String repoOwner,
        String repoName,
        int issueNumber
) {
}
