package cz.cvut.fel.thesis.dto;

import java.util.List;

public record OverviewGraphsDashboardDTO(
        List<DailyTimeTrackDTO> dailyData,
        List<IssueRankDTO> rankedIssues,
        List<OverviewLabelTimeDTO> timeByLabel
) {
}
