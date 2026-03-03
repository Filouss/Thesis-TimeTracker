package cz.cvut.fel.thesis.dto;

import jakarta.validation.constraints.NotNull;

public record SyncSessionRequest(
        @NotNull Long sessionId,
        @NotNull String notes
) {
}
