package cz.cvut.fel.thesis.dto;

import java.time.Instant;

public record UpdateTimeBlockRequest(
        Instant start,
        Instant end
) {}
