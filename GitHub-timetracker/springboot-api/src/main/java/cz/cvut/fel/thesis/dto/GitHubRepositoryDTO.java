package cz.cvut.fel.thesis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubRepositoryDTO(
//        Long id,
        String name,
        @JsonProperty("full_name") String fullName,
        GitHubUserDTO owner,
        @JsonProperty("html_url") String repositoryUrl
) {
}
