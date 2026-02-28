package cz.cvut.fel.thesis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CommentDTO(
        Long id,
        String body
) {
}
