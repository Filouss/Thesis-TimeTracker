package cz.cvut.fel.thesis.dto;

import java.util.List;

public record UpdateSessionRequest(
        UpdateIssueRequest issue,
        String notes,
        List<UpdateTimeBlockRequest> timeBlocks,
        boolean synced
) {}

