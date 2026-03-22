package cz.cvut.fel.thesis.dto;

import cz.cvut.fel.thesis.model.Session;

import java.time.ZoneId;
import java.util.List;

public record SessionDTO (
    Long id,
    boolean synced,
    List<TimeBlockDTO> timeblocks,
    IssueDTO issue,
    boolean paused,
    String notes,
    Long trackedSeconds
) {
    public static SessionDTO fromEntity(Session session){
        Long trackedSeconds = session.getTimeBlocks().stream()
                .mapToLong(tb -> {
                    if (tb.getStartDate() == null) return 0L;
                    if (tb.getEndDate() == null) return 0L;
                    return java.time.Duration.between(tb.getStartDate(), tb.getEndDate()).getSeconds();
                })
                .sum();

        return new SessionDTO(
                session.getId(),
                session.isSynced(),
                session.getTimeBlocks().stream()
                        .map(TimeBlockDTO::fromEntity)
                        .toList(),
                IssueDTO.fromEntity(session.getIssue()),
                session.isPaused(),
                session.getNotes(),
                trackedSeconds
        );
    }
}
