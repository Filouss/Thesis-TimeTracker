package cz.cvut.fel.thesis.dto;

public record DailyTimeTrackDTO(
    String weekDay, 
    Long secondsTracked
) {}