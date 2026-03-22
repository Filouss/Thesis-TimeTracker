package cz.cvut.fel.thesis.dto;

public record OverviewStatsDashboardDTO(
    Long totalTimeTracked,
    Float workingTimeRatio
) {
    
}
