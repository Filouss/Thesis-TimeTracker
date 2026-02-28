package cz.cvut.fel.thesis.dto;

import java.util.List;

public record IssueDTO(
        Long id,
        String title,
        int number,
        Long GitHUbID,
        List<LabelDTO> labels
) {
}
