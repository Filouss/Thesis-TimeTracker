package cz.cvut.fel.thesis.dto;

import java.util.List;

public record SessionDTO (
    Long id,
    boolean synced,
    List<TimeBlockDTO> timeblocks,
    IssueDTO issue,
    boolean paused
) { }
