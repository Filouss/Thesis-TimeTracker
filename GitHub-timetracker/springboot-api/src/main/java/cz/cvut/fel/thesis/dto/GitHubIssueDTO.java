package cz.cvut.fel.thesis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;

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
        String url,
        List<LabelDTO> labels,
        @JsonProperty("repository_url") String repoUrl
        ) {
    public String repoOwnerFromUrl() {
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
