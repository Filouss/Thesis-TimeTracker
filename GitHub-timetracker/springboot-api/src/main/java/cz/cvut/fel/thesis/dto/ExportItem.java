package cz.cvut.fel.thesis.dto;

public record ExportItem(
    String issueTitle,
    String repoName,
    Long timeTracked,
    String createdAt
) {
    
}
