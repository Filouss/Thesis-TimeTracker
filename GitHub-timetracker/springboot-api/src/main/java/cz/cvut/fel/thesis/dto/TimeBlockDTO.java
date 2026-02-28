package cz.cvut.fel.thesis.dto;

import java.time.Instant;
import java.time.LocalDateTime;

public record TimeBlockDTO(
        Long id,
        LocalDateTime start,
        LocalDateTime end
) {
}
