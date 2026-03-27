package cz.cvut.fel.thesis.dto;

public record ExportRequestData(
    String issueTitle,
    String repoName,
    String interval,
    String zoneId
) {
}
