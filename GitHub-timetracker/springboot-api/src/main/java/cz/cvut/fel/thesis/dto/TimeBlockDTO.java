package cz.cvut.fel.thesis.dto;

import cz.cvut.fel.thesis.model.TimeBlock;

import java.time.Instant;
import java.time.LocalDateTime;

public record TimeBlockDTO(
        Instant start,
        Instant end
) {
    public static TimeBlockDTO fromEntity(TimeBlock tb) {
        return new TimeBlockDTO(
                tb.getStartDate(),
                tb.getEndDate()
        );
    }
}
