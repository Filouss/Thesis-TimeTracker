package cz.cvut.fel.thesis.dto;

public record OverviewLabelTimeDTO(
    String name,
    String color, // e.g., "e21d3f"
    Long secondsTracked
) {
    
}
