package cz.cvut.fel.thesis.dto;

public record IssueRankDTO(
        String title,
        int number,
        long timeTracked
) {
}
