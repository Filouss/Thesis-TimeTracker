import cz.cvut.fel.thesis.dto.GitHubIssueDTO;
import cz.cvut.fel.thesis.dto.GitHubUserDTO;
import cz.cvut.fel.thesis.dto.HomeDashboardDTO;
import cz.cvut.fel.thesis.dto.SessionDTO;
import cz.cvut.fel.thesis.model.Issue;
import cz.cvut.fel.thesis.model.User;
import cz.cvut.fel.thesis.service.DashboardService;
import cz.cvut.fel.thesis.service.IssueService;
import cz.cvut.fel.thesis.service.SessionService;
import cz.cvut.fel.thesis.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.NotActiveException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DashboardServiceTest {

    @Mock private IssueService issueService;
    @Mock private UserService userService;
    @Mock private SessionService sessionService;

    @InjectMocks
    private DashboardService dashboardService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("test-user");
        testUser.setActiveSessionID(null);
    }

    @Test
    void getHomeDashboardData_UsesBatchedLookups_ForAssignedAndPinned() throws Exception {
        // ARRANGE
        GitHubIssueDTO issue1 = createIssueDto(101L, "Issue One");
        GitHubIssueDTO issue2 = createIssueDto(202L, "Issue Two");

        when(sessionService.getUnsyncedDTOs(testUser)).thenReturn(List.of());
        when(userService.getPinnedIssueGitHubIds(testUser)).thenReturn(Set.of(101L));
        when(sessionService.isActivePaused(testUser)).thenThrow(new NotActiveException());

        when(issueService.getAssignedIssues()).thenReturn(List.of(issue1, issue2));

        Issue issueEntity1 = new Issue();
        issueEntity1.setGithubId(101L);
        when(issueService.getByGitHubIDs(anyCollection())).thenReturn(Map.of(101L, issueEntity1));
        when(sessionService.getTrackedSecondsByIssueGithubIds(eq(testUser), any())).thenReturn(Map.of(101L, 120L));
        when(sessionService.getAllSyncedByIssueGithubIds(eq(testUser), any())).thenReturn(Map.of(101L, false));

        // ACT
        HomeDashboardDTO result = dashboardService.getHomeDashboardData(testUser);

        // ASSERT
        assertNotNull(result);
        assertNull(result.tracking());
        assertFalse(result.trackingPaused());

        assertEquals(2, result.assigned().size());
        GitHubIssueDTO assigned1 = result.assigned().stream().filter(i -> i.id().equals(101L)).findFirst().orElseThrow();
        GitHubIssueDTO assigned2 = result.assigned().stream().filter(i -> i.id().equals(202L)).findFirst().orElseThrow();

        assertEquals(120L, assigned1.timeTracked());
        assertFalse(assigned1.allSynced());

        assertEquals(0L, assigned2.timeTracked());
        assertTrue(assigned2.allSynced());

        assertEquals(1, result.pinned().size());
        assertEquals(101L, result.pinned().get(0).id());
        assertEquals(120L, result.pinned().get(0).timeTracked());
        assertFalse(result.pinned().get(0).allSynced());

        verify(issueService, times(1)).getAssignedIssues();
        verify(issueService, times(1)).getByGitHubIDs(anyCollection());
        verify(sessionService, times(1)).getTrackedSecondsByIssueGithubIds(eq(testUser), any());
        verify(sessionService, times(1)).getAllSyncedByIssueGithubIds(eq(testUser), any());

        verify(issueService, never()).getByGitHubID(anyLong());
        verify(issueService, never()).getTimeTrackedForIssueInSec(any(), eq(testUser));
        verify(sessionService, never()).allSyncedForIssue(any(), eq(testUser));
    }

    @Test
    void getHomeDashboardData_EncapsulatesActiveIssue_WhenActiveSessionExists() throws Exception {
        // ARRANGE
        testUser.setActiveSessionID(55L);

        GitHubIssueDTO activeIssue = createIssueDto(999L, "Active Issue");
        Issue activeIssueEntity = new Issue();
        activeIssueEntity.setGithubId(999L);

        when(sessionService.getUnsyncedDTOs(testUser)).thenReturn(List.<SessionDTO>of());
        when(userService.getPinnedIssueGitHubIds(testUser)).thenReturn(Set.of());
        when(sessionService.isActivePaused(testUser)).thenReturn(true);

        when(issueService.getIssueBySessionId(55L)).thenReturn(activeIssue);
        when(issueService.getByGitHubID(999L)).thenReturn(activeIssueEntity);
        when(issueService.getTimeTrackedForIssueInSec(activeIssueEntity, testUser)).thenReturn(33L);
        when(sessionService.allSyncedForIssue(activeIssueEntity, testUser)).thenReturn(true);

        when(issueService.getAssignedIssues()).thenReturn(List.of());
        when(issueService.getByGitHubIDs(Set.of())).thenReturn(Map.of());
        when(sessionService.getTrackedSecondsByIssueGithubIds(testUser, Set.of())).thenReturn(Map.of());
        when(sessionService.getAllSyncedByIssueGithubIds(testUser, Set.of())).thenReturn(Map.of());

        // ACT
        HomeDashboardDTO result = dashboardService.getHomeDashboardData(testUser);

        // ASSERT
        assertNotNull(result.tracking());
        assertEquals(999L, result.tracking().id());
        assertEquals(33L, result.tracking().timeTracked());
        assertTrue(result.tracking().allSynced());
        assertTrue(result.trackingPaused());
    }

    private GitHubIssueDTO createIssueDto(Long id, String title) {
        GitHubUserDTO assignee = new GitHubUserDTO(1L, "test-user");
        return new GitHubIssueDTO(
                id,
                1,
                title,
                "open",
                "body",
                null,
                null,
                null,
                List.of(assignee),
                null,
                List.of(),
                "https://api.github.com/repos/owner/repo",
                0L,
                assignee,
                true
        );
    }
}
