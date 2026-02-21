package cz.cvut.fel.thesis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record IssueSearchResponseDTO(
        @JsonProperty("total_count") int totalCount,
        @JsonProperty("incomplete_results") boolean incompleteResults,
        List<GitHubIssueDTO> items
) {}
