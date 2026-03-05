package cz.cvut.fel.thesis.dto;

import java.time.LocalDateTime;

public record UpdateTimeBlockRequest(
        Long id,
        LocalDateTime startDate,
        LocalDateTime endDate
) {}
