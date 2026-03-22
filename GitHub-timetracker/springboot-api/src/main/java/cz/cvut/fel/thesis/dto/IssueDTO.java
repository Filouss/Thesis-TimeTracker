package cz.cvut.fel.thesis.dto;

import cz.cvut.fel.thesis.model.Issue;

import java.util.List;

public record IssueDTO(
        Long id,
        String title,
        int number,
        Long GitHubID,
        List<LabelDTO> labels,
        String repoName,
        String repoOwner
) {
    public static IssueDTO fromEntity(Issue issue) {
        return new IssueDTO(
                issue.getId(),
                issue.getTitle(),
                issue.getIssueNumber(),
                issue.getGithubId(),
                issue.getLabels().stream()
                        .map(LabelDTO::fromEntity)
                        .toList(),
                issue.getRepository().getName(),
                issue.getRepository().getOwner()
        );
    }
}
