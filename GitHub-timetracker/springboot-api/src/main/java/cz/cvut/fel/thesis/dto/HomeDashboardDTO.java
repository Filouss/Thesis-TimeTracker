package cz.cvut.fel.thesis.dto;

import java.time.Instant;
import java.util.List;

public record HomeDashboardDTO(
    List<GitHubIssueDTO> assigned,
    List<GitHubIssueDTO> pinned,
    GitHubIssueDTO tracking,
    List<SessionDTO> toSync,
    Boolean trackingPaused,
    Instant currLatestTBStartTime,
    Long currFinishedTBDuration
) {    
}
