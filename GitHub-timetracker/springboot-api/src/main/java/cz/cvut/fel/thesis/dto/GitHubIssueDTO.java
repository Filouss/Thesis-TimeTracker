package cz.cvut.fel.thesis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubIssueDTO(
        Long id,
        int number,
        String title,
        String state,
        String body,
        @JsonProperty("html_url") String htmlUrl,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("updated_at") OffsetDateTime updatedAt,
        GitHubUserDTO assignee,
        @JsonProperty("repository_url") String repoUrl,
        String url
) {
    public String owner() {
        return parseOwnerRepo()[0];
    }

    public String repoName() {
        return parseOwnerRepo()[1];
    }

    private String[] parseOwnerRepo() {
        // https://api.github.com/repos/{owner}/{repo}
        String[] parts = repoUrl.split("/repos/");
        if (parts.length != 2) {
            throw new IllegalStateException("Invalid repository_url: " + repoUrl);
        }
        return parts[1].split("/");
    }
}
