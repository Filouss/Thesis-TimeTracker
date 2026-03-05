package cz.cvut.fel.thesis.dto;

import cz.cvut.fel.thesis.model.Session;

import java.util.List;

public record SessionDTO (
    Long id,
    boolean synced,
    List<TimeBlockDTO> timeblocks,
    IssueDTO issue,
    boolean paused,
    String notes
) {
    public static SessionDTO fromEntity(Session session){
        return new SessionDTO(
                session.getId(),
                session.isSynced(),
                session.getTimeBlocks().stream()
                        .map(TimeBlockDTO::fromEntity)
                        .toList(),
                IssueDTO.fromEntity(session.getIssue()),
                session.isPaused(),
                session.getNotes()
        );
    }
}
