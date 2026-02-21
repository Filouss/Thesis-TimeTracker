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
//      tohle moyna nebude potreba kdyz to jen vyparsuju z url
//        GitHubUserDTO creator,
        @JsonProperty("repository_url") String repoUrl,
        String url
) {

}
