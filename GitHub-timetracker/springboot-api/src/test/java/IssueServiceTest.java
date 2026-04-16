import cz.cvut.fel.thesis.client.GitHubAPIClient;
import cz.cvut.fel.thesis.dao.*;
import cz.cvut.fel.thesis.dto.GitHubIssueDTO;
import cz.cvut.fel.thesis.dto.GitHubUserDTO;
import cz.cvut.fel.thesis.exceptions.UnassignedIssueException;
import cz.cvut.fel.thesis.model.*;
import cz.cvut.fel.thesis.service.IssueService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IssueServiceTest {

    @Mock private IssueDAO issueDAO;
    @Mock private UserDAO userDAO;
    @Mock private RepositoryDAO repositoryDAO;
    @Mock private SessionDAO sessionDAO;
    @Mock private GitHubAPIClient gitHubAPIClient;

    @InjectMocks
    private IssueService issueService;

    private User testUser;
    private GitHubUserDTO validAssignee;
    private GitHubUserDTO wrongAssignee;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testUser");
        testUser.setPinnedIssueGithubIds(new HashSet<>()); // Set musí být inicializovaný

        validAssignee = new GitHubUserDTO(1L,"testUser");
        wrongAssignee = new GitHubUserDTO(2L, "anotherUser");
    }

    @Nested
    class PinningIssues {

        @Test
        void pinIssue_Success_AddsToPinnedAndSavesUser() {
            // ARRANGE
            GitHubIssueDTO validIssue = createMockIssueDTO(999L, validAssignee);
            when(gitHubAPIClient.getIssue(42, "repo", "owner")).thenReturn(validIssue);

            // ACT
            GitHubIssueDTO result = issueService.pinIssue(42, "repo", "owner", testUser);

            // ASSERT
            assertTrue(testUser.getPinnedIssueGithubIds().contains(999L));
            verify(userDAO, times(1)).save(testUser);
            assertEquals(999L, result.id());
        }

        @Test
        void pinIssue_Fails_WhenIssueIsUnassigned() {
            // ARRANGE
            GitHubIssueDTO unassignedIssue = createMockIssueDTO(999L, null); // No one is assigned
            when(gitHubAPIClient.getIssue(42, "repo", "owner")).thenReturn(unassignedIssue);

            // ACT & ASSERT
            UnassignedIssueException exception = assertThrows(UnassignedIssueException.class, () -> {
                issueService.pinIssue(42, "repo", "owner", testUser);
            });

            // assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            verify(userDAO, never()).save(any());
        }

        @Test
        void pinIssue_Fails_WhenIssueAssignedToSomeoneElse() {
            // ARRANGE
            GitHubIssueDTO wrongUserIssue = createMockIssueDTO(999L, wrongAssignee);
            when(gitHubAPIClient.getIssue(42, "repo", "owner")).thenReturn(wrongUserIssue);

            // ACT & ASSERT
            assertThrows(UnassignedIssueException.class, () -> {
                issueService.pinIssue(42, "repo", "owner", testUser);
            });
            verify(userDAO, never()).save(any());
        }

        @Test
        void unpinIssue_Success_RemovesFromPinnedAndSavesUser() {
            // ARRANGE
            testUser.getPinnedIssueGithubIds().add(999L); // Already pinned
            GitHubIssueDTO validIssue = createMockIssueDTO(999L, validAssignee);
            when(gitHubAPIClient.getIssue(42, "repo", "owner")).thenReturn(validIssue);

            // ACT
            issueService.unpinIssue(42, "repo", "owner", testUser);

            // ASSERT
            assertFalse(testUser.getPinnedIssueGithubIds().contains(999L));
            verify(userDAO, times(1)).save(testUser);
        }
    }

    @Nested
    class DatabaseEntityOperations {

        @Test
        void getOrCreateIssue_CreatesNew_WhenNotFoundInDB() {
            // ARRANGE
            GitHubIssueDTO fetchedIssue = createMockIssueDTO(123L, validAssignee);
            Repository repo = new Repository();
            Set<Label> labels = Set.of(new Label());

            when(issueDAO.findByGithubId(123L)).thenReturn(Optional.empty()); // Does not exist in DB
            
            // We capture the saved object to verify its data
            when(issueDAO.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // ACT
            Issue result = issueService.getOrCreateIssue(42, fetchedIssue, repo, labels);

            // ASSERT
            assertEquals(123L, result.getGithubId());
            assertEquals(42, result.getIssueNumber());
            assertEquals("Mock Title", result.getTitle());
            assertEquals(State.OPEN, result.getState());
            assertEquals(repo, result.getRepository());
            assertEquals(labels, result.getLabels());
        }

        @Test
        void getOrCreateIssue_UpdatesExisting_WhenFoundInDB() {
            // ARRANGE
            GitHubIssueDTO fetchedIssue = createMockIssueDTO(123L, validAssignee); // state is "open" in the mock
            Repository repo = new Repository();
            Set<Label> labels = Set.of(new Label());

            Issue existingIssue = new Issue();
            existingIssue.setGithubId(123L);
            existingIssue.setState(State.CLOSED); // Original state is CLOSED

            when(issueDAO.findByGithubId(123L)).thenReturn(Optional.of(existingIssue));
            when(issueDAO.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // ACT
            Issue result = issueService.getOrCreateIssue(42, fetchedIssue, repo, labels);

            // ASSERT
            assertEquals(State.OPEN, result.getState()); // State should be updated!
            verify(issueDAO, times(1)).save(existingIssue);
        }

        @Test
        void getTimeTrackedForIssueInSec_CalculatesTotalCorrectly() {
            // ARRANGE
            Issue issue = new Issue();
            
            Session s1 = mock(Session.class);
            when(s1.getDuration()).thenReturn(Duration.ofSeconds(100));

            Session s2 = mock(Session.class);
            when(s2.getDuration()).thenReturn(Duration.ofSeconds(250));

            when(sessionDAO.findByIssueAndUserAndFinishedTrue(issue, testUser)).thenReturn(List.of(s1, s2));

            // ACT
            Long totalSeconds = issueService.getTimeTrackedForIssueInSec(issue, testUser);

            // ASSERT
            assertEquals(350L, totalSeconds);
        }
    }

    @Nested
    class DelegationAndRetrieval {

        @Test
        void getAssignedIssues_ReturnsEmptyList_WhenApiReturnsNull() {
            // ARRANGE
            when(gitHubAPIClient.getAssignedIssues()).thenReturn(null);

            // ACT
            List<GitHubIssueDTO> result = issueService.getAssignedIssues();

            // ASSERT
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void getIssueBySessionId_ReturnsIssueDTO_WhenSessionAndIssueExist() {
            // ARRANGE
            Session session = new Session();
            Issue issue = new Issue();
            issue.setIssueNumber(10);
            
            Repository repo = new Repository();
            repo.setName("repoName");
            repo.setOwner("repoOwner");
            issue.setRepository(repo);
            session.setIssue(issue);

            when(sessionDAO.findById(100L)).thenReturn(Optional.of(session));
            
            GitHubIssueDTO expectedDto = createMockIssueDTO(999L, validAssignee);
            when(gitHubAPIClient.getIssue(10, "repoName", "repoOwner")).thenReturn(expectedDto);

            // ACT
            GitHubIssueDTO result = issueService.getIssueBySessionId(100L);

            // ASSERT
            assertNotNull(result);
            assertEquals(999L, result.id());
        }

        @Test
        void getIssueBySessionId_ReturnsNull_WhenSessionDoesNotExist() {
            when(sessionDAO.findById(100L)).thenReturn(Optional.empty());
            assertNull(issueService.getIssueBySessionId(100L));
        }

        @Test
        void getIssueNamesForQuery_DelegatesToDAO() {
            List<String> expected = List.of("Issue1", "Issue2");
            when(issueDAO.findIssueSuggestionsForUser(testUser, "Iss")).thenReturn(expected);

            List<String> result = issueService.getIssueNamesForQuery("Iss", testUser);

            assertEquals(expected, result);
        }

        @Test
        void getActiveSessionLatestTBStartTimeForIssue_ReturnsCurrentUnfinishedSessionBlockStart() {
            Issue issue = new Issue();
            Session activeSession = new Session();
            TimeBlock timeBlock = new TimeBlock();
            Instant start = Instant.now().minusSeconds(90);
            timeBlock.setStartDate(start);
            activeSession.setTimeBlocks(new ArrayList<>(List.of(timeBlock)));

            when(sessionDAO.findByUserAndFinishedFalse(testUser)).thenReturn(Optional.of(activeSession));

            assertEquals(start, issueService.getActiveSessionLatestTBStartTimeForIssue(issue, testUser));
        }

        @Test
        void getActiveSessionFinishedTBDurationForIssue_ReturnsCurrentUnfinishedSessionDuration() {
            Issue issue = new Issue();
            Session activeSession = new Session();
            activeSession.setTimeTracked(120L);

            when(sessionDAO.findByUserAndFinishedFalse(testUser)).thenReturn(Optional.of(activeSession));

            assertEquals(120L, issueService.getActiveSessionFinishedTBDurationForIssue(issue, testUser));
        }
    }


    private GitHubIssueDTO createMockIssueDTO(Long id, GitHubUserDTO assignee) {
        return new GitHubIssueDTO(
                id,
                42,
                "Mock Title",
                "open",
                "Mock Body",
                null, null, null,
                assignee == null ? null : List.of(assignee),
                null,
                List.of(),
                null, null, null, false
        );
    }
}