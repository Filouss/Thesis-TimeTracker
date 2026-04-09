import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.NotActiveException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import cz.cvut.fel.thesis.client.GitHubAPIClient;
import cz.cvut.fel.thesis.dao.IssueDAO;
import cz.cvut.fel.thesis.dao.LabelDAO;
import cz.cvut.fel.thesis.dao.RepositoryDAO;
import cz.cvut.fel.thesis.dao.SessionDAO;
import cz.cvut.fel.thesis.dao.TimeBlockDAO;
import cz.cvut.fel.thesis.dao.UserDAO;
import cz.cvut.fel.thesis.dto.CommentDTO;
import cz.cvut.fel.thesis.dto.GitHubIssueDTO;
import cz.cvut.fel.thesis.dto.GitHubUserDTO;
import cz.cvut.fel.thesis.dto.IssueRankDTO;
import cz.cvut.fel.thesis.dto.LabelDTO;
import cz.cvut.fel.thesis.dto.OverviewLabelTimeDTO;
import cz.cvut.fel.thesis.dto.SessionDTO;
import cz.cvut.fel.thesis.dto.UpdateIssueRequest;
import cz.cvut.fel.thesis.dto.UpdateSessionRequest;
import cz.cvut.fel.thesis.dto.UpdateTimeBlockRequest;
import cz.cvut.fel.thesis.exceptions.UnassignedIssueException;
import cz.cvut.fel.thesis.model.Issue;
import cz.cvut.fel.thesis.model.Label;
import cz.cvut.fel.thesis.model.Repository;
import cz.cvut.fel.thesis.model.Session;
import cz.cvut.fel.thesis.model.TimeBlock;
import cz.cvut.fel.thesis.model.User;
import cz.cvut.fel.thesis.service.FormatService;
import cz.cvut.fel.thesis.service.IssueService;
import cz.cvut.fel.thesis.service.SessionService;

@ExtendWith(MockitoExtension.class)
public class SessionServiceTest {
    
    @Mock private SessionDAO sessionDAO;
    @Mock private RepositoryDAO repositoryDAO;
    @Mock private IssueDAO issueDAO;
    @Mock private TimeBlockDAO timeBlockDAO;
    @Mock private LabelDAO labelDAO;
    @Mock private UserDAO userDAO;
    @Mock private IssueService issueService;
    @Mock private FormatService syncFormatService;
    @Mock private GitHubAPIClient gitHubAPIClient;

    @InjectMocks
    private SessionService sessionService;

    private User testUser;
    private Issue testIssue;
    private Session testSession;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testUser");
        testUser.setTracking(false);

        Repository testRepo = new Repository();
        testRepo.setOwner("testOwner");
        testRepo.setName("testRepo");

        testIssue = new Issue();
        testIssue.setId(1L);
        testIssue.setIssueNumber(42);
        testIssue.setRepository(testRepo);
        testIssue.setSessions(new ArrayList<>());
        testIssue.setLabels(new HashSet<>());

        testSession = new Session();
        testSession.setId(100L);
        testSession.setUser(testUser);
        testSession.setIssue(testIssue);
        testSession.setFinished(false);
        testSession.setSynced(false);
        testSession.setTimeBlocks(new ArrayList<>());
    }

    @Nested
    class SessionLifecycle {

        @Test
        void startSession_Success_CreatesNewSessionAndStartsTracking() {
            // ARRANGE
            int issueNumber = 42;
            String repo = "testRepo";
            String owner = "testOwner";

            GitHubUserDTO assignee = new GitHubUserDTO(1L, "testUser");
            GitHubIssueDTO fetchedIssue = new GitHubIssueDTO(
                    1L, issueNumber, "Title", "open", "Body", 
                    "https://github.com/testOwner/testRepo/issues/42", 
                    OffsetDateTime.now(), OffsetDateTime.now(), assignee, "issueUrl", 
                    List.of(), "https://api.github.com/repos/" + owner + "/" + repo, 
                    0L, assignee, false
            );

            when(issueService.getIssue(issueNumber, repo, owner)).thenReturn(fetchedIssue);
            when(issueService.getOrCreateRepository(repo, owner)).thenReturn(testIssue.getRepository());
            when(issueService.getOrCreateLabels(fetchedIssue)).thenReturn(new HashSet<>());
            when(issueService.getOrCreateIssue(eq(issueNumber), eq(fetchedIssue), any(Repository.class), anySet()))
                    .thenReturn(testIssue);
            
            Session newSession = new Session();
            newSession.setId(99L); 
            when(sessionDAO.save(any(Session.class))).thenReturn(newSession);

            // ACT
            sessionService.startSession(issueNumber, repo, owner, testUser);

            // ASSERT
            assertTrue(testUser.isTracking());
            assertEquals(99L, testUser.getActiveSessionID());
            verify(timeBlockDAO, times(1)).save(any(TimeBlock.class));
            verify(userDAO, times(1)).save(testUser);
        }

        @Test
        void startSession_Fails_WhenUserNotAssigned() {
            // ARRANGE
            GitHubUserDTO wrongAssignee = new GitHubUserDTO(2L, "anotherUser");
            GitHubIssueDTO unassignedIssue = new GitHubIssueDTO(
                    1L, 42, "Title", "open", "Body", 
                    "https://github.com/testOwner/testRepo/issues/42", 
                    OffsetDateTime.now(), OffsetDateTime.now(), wrongAssignee, "issueUrl", 
                    List.of(), "https://api.github.com/repos/testOwner/testRepo", 
                    0L, wrongAssignee, false
            );
            
            when(issueService.getIssue(42, "testRepo", "testOwner")).thenReturn(unassignedIssue);

            // ACT & ASSERT
            UnassignedIssueException exception = assertThrows(UnassignedIssueException.class, () -> {
                sessionService.startSession(42, "testRepo", "testOwner", testUser);
            });

            assertEquals("Can't start a session for an unassigned issue", exception.getMessage());
            verify(sessionDAO, never()).save(any(Session.class));
        }

        @Test
        void endSession_Success_StopsTrackingAndSavesNotes() throws NotActiveException {
            // ARRANGE
            testUser.setTracking(true);
            testUser.setActiveSessionID(100L);
            testSession.setPaused(false);
            
            TimeBlock activeBlock = new TimeBlock();
            activeBlock.setStartDate(Instant.now().minusSeconds(60));
            testSession.getTimeBlocks().add(activeBlock);

            when(sessionDAO.findById(100L)).thenReturn(Optional.of(testSession));

            // ACT
            sessionService.endSession(testUser, "Hotovo, commitnuto.");

            // ASSERT
            assertTrue(testSession.isFinished());
            assertEquals("Hotovo, commitnuto.", testSession.getNotes());
            assertFalse(testUser.isTracking());
            assertNull(testUser.getActiveSessionID());
            
            verify(sessionDAO, atLeastOnce()).save(testSession);
            verify(userDAO, times(1)).save(testUser);
        }

        @Test
        void pauseSession_Success_ClosesTimeBlock() throws NotActiveException {
            // ARRANGE
            testUser.setActiveSessionID(100L);
            testSession.setPaused(false);

            TimeBlock activeBlock = new TimeBlock();
            activeBlock.setStartDate(Instant.now().minusSeconds(120));
            testSession.getTimeBlocks().add(activeBlock);

            when(sessionDAO.findById(100L)).thenReturn(Optional.of(testSession));

            // ACT
            sessionService.pauseSession(testUser);

            // ASSERT
            assertTrue(testSession.isPaused());
            assertNotNull(testSession.getMostRecentTimeBlock().getEndDate());
            verify(timeBlockDAO, times(1)).save(any(TimeBlock.class));
            verify(sessionDAO, times(1)).save(testSession);
        }
    }

    @Nested
    class GitHubSynchronization {

        @Test
        void deleteSession_OnlyUnsyncedSessions_CallsDeleteApi() {
            // ARRANGE
            testSession.setSynced(false);
            testIssue.setGithubCommentId(555L);
            
            testIssue.getSessions().add(testSession); 

            when(sessionDAO.findByIdAndUser(100L, testUser)).thenReturn(Optional.of(testSession));

            // ACT
            sessionService.deleteSession(testUser, 100L, ZoneId.of("UTC"));

            // ASSERT
            verify(gitHubAPIClient, times(1)).deleteComment(testIssue);
            assertNull(testIssue.getGithubCommentId());
            verify(issueDAO, times(1)).save(testIssue);
            verify(sessionDAO, times(1)).delete(testSession);
        }

        @Test
        void syncSession_WithExistingComment_CallsEditApi() {
            // ARRANGE
            testSession.setFinished(true); 
            testIssue.setGithubCommentId(999L);
            String mockNotes = "notes";
            String formattedBody = "Markdown body komentáře";

            when(sessionDAO.findByIdAndUser(100L, testUser)).thenReturn(Optional.of(testSession));
            when(syncFormatService.buildSessionComments(testIssue, ZoneId.of("UTC"))).thenReturn(formattedBody);

            // ACT
            sessionService.syncSession(100L, mockNotes, testUser, ZoneId.of("UTC"));

            // ASSERT
            assertTrue(testSession.isSynced());
            assertEquals(mockNotes, testSession.getNotes());
            verify(sessionDAO, atLeastOnce()).save(testSession);
            
            verify(gitHubAPIClient, times(1)).editComment(testSession, formattedBody, 999L);
            verify(gitHubAPIClient, never()).createIssueComment(any(), anyString());
        }

        @Test
        void syncSession_WithoutExistingComment_CallsCreateApi() {
            // ARRANGE
            testSession.setFinished(true);
            testIssue.setGithubCommentId(null); // GitHub comment does not exist yet
            String mockNotes = "First entry.";
            String formattedBody = "New comment body";

            when(sessionDAO.findByIdAndUser(100L, testUser)).thenReturn(Optional.of(testSession));
            when(syncFormatService.buildNewComment(eq(testSession), eq(mockNotes), any(), eq(ZoneId.of("UTC"))))
                    .thenReturn(formattedBody);

            // We must simulate that the API returns a DTO with a new comment ID (e.g., 123L)
            CommentDTO mockCommentDTO = new CommentDTO(123L, "body");
            when(gitHubAPIClient.createIssueComment(testIssue, formattedBody)).thenReturn(mockCommentDTO);

            // ACT
            sessionService.syncSession(100L, mockNotes, testUser, ZoneId.of("UTC"));

            // ASSERT
            assertTrue(testSession.isSynced());
            assertEquals(123L, testIssue.getGithubCommentId()); // Verify that the ID was propagated to the Issue
            verify(issueDAO, times(1)).save(testIssue);
            verify(gitHubAPIClient, times(1)).createIssueComment(testIssue, formattedBody);
        }
    }

    @Nested
    class StatisticsAndRanking {

        @Test
        void getRankedIssues_AggregatesAndSortsCorrectly() {
            // ARRANGE
            Issue issueA = new Issue(); issueA.setId(1L); issueA.setTitle("Frontend Bug");
            Issue issueB = new Issue(); issueB.setId(2L); issueB.setTitle("Backend Auth");

            Object[] rowB = new Object[]{issueB, 300L};
            Object[] rowA = new Object[]{issueA, 120L};

            when(sessionDAO.getTopIssuesByTime(testUser)).thenReturn(List.of(rowB, rowA));

            // ACT
            List<IssueRankDTO> rankedIssues = sessionService.getRankedIssues(testUser);

            // ASSERT
            assertEquals(2, rankedIssues.size());

            // Issue B has the most time (300s), so it must be first!
            assertEquals("Backend Auth", rankedIssues.get(0).title());
            assertEquals(300L, rankedIssues.get(0).timeTracked());

            // Issue A in second place (120s total)
            assertEquals("Frontend Bug", rankedIssues.get(1).title());
            assertEquals(120L, rankedIssues.get(1).timeTracked());
        }

        @Test
        void getTimePerLabel_AggregatesTimeCorrectly() {
            // ARRANGE
            Label labelBug = new Label(); labelBug.setId(1L); labelBug.setTitle("bug");
            Label labelEnhancement = new Label(); labelEnhancement.setId(2L); labelEnhancement.setTitle("enhancement");

            Object[] bugRow = new Object[]{labelBug, 150L};
            Object[] enhancementRow = new Object[]{labelEnhancement, 100L};

            when(sessionDAO.getTimeTrackedPerLabel(testUser)).thenReturn(List.of(bugRow, enhancementRow));

            // ACT
            List<OverviewLabelTimeDTO> labelStats = sessionService.getTimePerLabel(testUser);

            // ASSERT
            assertEquals(2, labelStats.size());
            
            // We should have 150 seconds for "bug" label and 100 seconds for "enhancement"
            OverviewLabelTimeDTO bugStat = labelStats.stream()
                    .filter(l -> l.name().equals("bug"))
                    .findFirst().orElseThrow();
            assertEquals(150L, bugStat.secondsTracked());

            OverviewLabelTimeDTO enhancementStat = labelStats.stream()
                    .filter(l -> l.name().equals("enhancement"))
                    .findFirst().orElseThrow();
            assertEquals(100L, enhancementStat.secondsTracked());
        }

        // Helper method to simulate Session with given duration (for statistics)
        private Session createFinishedSessionForMock(Issue issue, long secondsDuration) {
            Session s = new Session();
            s.setIssue(issue);
            s.setFinished(true);
            s.setUser(testUser);
            
            TimeBlock tb = new TimeBlock();
            tb.setStartDate(Instant.now().minusSeconds(secondsDuration));
            tb.setEndDate(Instant.now());
            s.setTimeBlocks(List.of(tb));
            
            return s;
        }
    }

    @Nested
    class EditSession {

        @Test
        void editSession_Success_UpdatesNotesIssueAndTimeBlocks() {
            // ARRANGE
            Long sessionId = 100L;
            testSession.setSynced(true); // We will test that it resets to false
            
            // 1. Mocking Requests
            UpdateSessionRequest updateRequest = mock(UpdateSessionRequest.class);
            UpdateIssueRequest issueRequest = mock(UpdateIssueRequest.class);
            
            when(updateRequest.notes()).thenReturn("Updated notes");
            when(updateRequest.issue()).thenReturn(issueRequest);
            when(updateRequest.synced()).thenReturn(true); // Simulate that user wants resync
            
            when(issueRequest.issueNumber()).thenReturn(42);
            when(issueRequest.repoName()).thenReturn("testRepo");
            when(issueRequest.repoOwner()).thenReturn("testOwner");

            // 2. Mocking GitHub and DB responses
            when(sessionDAO.findByIdAndUser(sessionId, testUser)).thenReturn(Optional.of(testSession));
            
            GitHubUserDTO assignee = new GitHubUserDTO(1L,"testUser");
            GitHubIssueDTO fetchedIssue = new GitHubIssueDTO(
                    999L, 42, "Title", "open", "Body", 
                    "https://github.com/testOwner/testRepo/issues/42", 
                    OffsetDateTime.now(), OffsetDateTime.now(), assignee, "issueUrl", 
                    List.of(), "https://api.github.com/repos/testOwner/testRepo", 
                    0L, assignee, false
            );
            when(issueService.getIssue(42, "testRepo", "testOwner")).thenReturn(fetchedIssue);
            
            // Simulate that Issue already exists in DB
            when(issueDAO.findByGithubId(999L)).thenReturn(Optional.of(testIssue));

            // 3. Mocking new TimeBlocks (e.g., manual time adjustment by user)
            UpdateTimeBlockRequest tbRequest = mock(UpdateTimeBlockRequest.class);
            Instant start = Instant.now().minusSeconds(120); // 2 minutes
            Instant end = Instant.now();
            when(tbRequest.start()).thenReturn(start);
            when(tbRequest.end()).thenReturn(end);
            
            when(updateRequest.timeBlocks()).thenReturn(List.of(tbRequest));

            // Mock for final save
            when(sessionDAO.save(any(Session.class))).thenReturn(testSession);

            // ACT
            Session result = sessionService.editSession(testUser, sessionId, updateRequest);

            // ASSERT
            assertNotNull(result);
            assertEquals("Updated notes", result.getNotes());
            assertEquals(120L, result.getTimeTracked());
            assertEquals(1, result.getTimeBlocks().size());
            assertFalse(result.isSynced());
            
            verify(sessionDAO, times(1)).save(testSession);
        }

        @Test
        void editSession_Fails_WhenTimeBlockEndIsBeforeStart() {
            // ARRANGE
            Long sessionId = 100L;
            
            UpdateSessionRequest updateRequest = mock(UpdateSessionRequest.class);
            UpdateIssueRequest issueRequest = mock(UpdateIssueRequest.class);
            when(updateRequest.issue()).thenReturn(issueRequest);
            when(updateRequest.notes()).thenReturn("Note");

            when(issueRequest.issueNumber()).thenReturn(42);
            when(issueRequest.repoName()).thenReturn("testRepo");
            when(issueRequest.repoOwner()).thenReturn("testOwner");

            when(sessionDAO.findByIdAndUser(sessionId, testUser)).thenReturn(Optional.of(testSession));
            
            // Příprava validního Issue
            GitHubUserDTO assignee = new GitHubUserDTO(1L,"testUser");
            GitHubIssueDTO fetchedIssue = new GitHubIssueDTO(
                    999L, 42, "Title", "open", "Body", 
                    "https://github.com/testOwner/testRepo/issues/42", 
                    OffsetDateTime.now(), OffsetDateTime.now(), assignee, "issueUrl", 
                    List.of(), "https://api.github.com/repos/testOwner/testRepo", 
                    0L, assignee, false
            );
            when(issueService.getIssue(anyInt(), anyString(), anyString())).thenReturn(fetchedIssue);
            when(issueDAO.findByGithubId(anyLong())).thenReturn(Optional.of(testIssue));

            // PREPARING INVALID BLOCK: End is before start
            UpdateTimeBlockRequest invalidTbRequest = mock(UpdateTimeBlockRequest.class);
            Instant start = Instant.now();
            Instant end = Instant.now().minusSeconds(60); 
            when(invalidTbRequest.start()).thenReturn(start);
            when(invalidTbRequest.end()).thenReturn(end);
            
            when(updateRequest.timeBlocks()).thenReturn(List.of(invalidTbRequest));

            // ACT & ASSERT
            ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
                sessionService.editSession(testUser, sessionId, updateRequest);
            });

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("400 BAD_REQUEST \"timeBlocks.endDate must be >= startDate\"", exception.getMessage());
            
            // Verify that bad data was never saved
            verify(sessionDAO, never()).save(any(Session.class));
        }

        @Test
        void editSession_ReturnsNull_WhenSessionNotFound() {
            // ARRANGE
            UpdateSessionRequest updateRequest = mock(UpdateSessionRequest.class);
            UpdateIssueRequest issueRequest = mock(UpdateIssueRequest.class);
            when(updateRequest.issue()).thenReturn(issueRequest);
            
            // Nasimulujeme, že Session v DB neexistuje, nebo nepatří tomuto uživateli
            when(sessionDAO.findByIdAndUser(999L, testUser)).thenReturn(Optional.empty());

            // ACT
            Session result = sessionService.editSession(testUser, 999L, updateRequest);

            // ASSERT
            assertNull(result);
            verify(sessionDAO, never()).save(any());
        }
    }

      @Nested
    class LifecycleEdgeCases {

        @Test
        void resumeSession_Success_SetsPausedToFalseAndCreatesTimeBlock() throws NotActiveException {
            // ARRANGE
            testUser.setActiveSessionID(100L);
            testSession.setPaused(true); // Must be paused to be resumed

            when(sessionDAO.findById(100L)).thenReturn(Optional.of(testSession));

            // ACT
            sessionService.resumeSession(testUser);

            // ASSERT
            assertFalse(testSession.isPaused());
            // Verify that a new TimeBlock was created for new activity
            verify(timeBlockDAO, times(1)).save(any(TimeBlock.class));
            verify(sessionDAO, times(1)).save(testSession);
        }

        @Test
        void pauseSession_ThrowsNotActiveException_WhenNoActiveSession() {
            // ARRANGE
            testUser.setActiveSessionID(null); // User has nothing running

            // ACT & ASSERT
            assertThrows(NotActiveException.class, () -> {
                sessionService.pauseSession(testUser);
            });
            verify(sessionDAO, never()).save(any());
        }
        
        @Test
        void isActivePaused_ReturnsTrue_WhenSessionIsPaused() throws NotActiveException {
            // ARRANGE
            testUser.setActiveSessionID(100L);
            testSession.setPaused(true);
            when(sessionDAO.findById(100L)).thenReturn(Optional.of(testSession));

            // ACT
            boolean result = sessionService.isActivePaused(testUser);

            // ASSERT
            assertTrue(result);
        }
    }

    @Nested
    class FilteringAndMapping {

        @Test
        void getUnsyncedDTOs_FiltersOnlyFinishedAndUnsynced() {
            Session s1 = new Session(); 
            s1.setId(1L); s1.setFinished(false); s1.setSynced(false); 
            s1.setIssue(testIssue); s1.setTimeBlocks(new ArrayList<>()); 

            Session s2 = new Session(); 
            s2.setId(2L); s2.setFinished(true); s2.setSynced(true);   
            s2.setIssue(testIssue); s2.setTimeBlocks(new ArrayList<>()); 

            Session s3 = new Session(); 
            s3.setId(3L); s3.setFinished(true); s3.setSynced(false);  
            s3.setIssue(testIssue); s3.setTimeBlocks(new ArrayList<>()); 

            // Create a local user to make sure we have a clean collection
            User localUser = new User();
            localUser.setSessions(new ArrayList<>(List.of(s1, s2, s3)));

            // ACT
            List<SessionDTO> result = sessionService.getUnsyncedDTOs(localUser);

            // ASSERT
            assertEquals(1, result.size());
            assertEquals(3L, result.get(0).id());
        }

        @Test
        void allSyncedForIssue_ReturnsFalse_WhenOneSessionIsPending() {
            // ARRANGE
            Session s1 = new Session(); s1.setFinished(true); s1.setSynced(true);
            Session s2 = new Session(); s2.setFinished(true); s2.setSynced(false);

            when(sessionDAO.findByIssueAndUser(testIssue, testUser)).thenReturn(List.of(s1, s2));

            // ACT
            boolean result = sessionService.allSyncedForIssue(testIssue, testUser);

            // ASSERT
            assertFalse(result);
        }
    }


    @Nested
    class TimeCalculations {

        @Test
        void getWorkingTimeRatio_AvoidsDivisionByZero_WhenSessionIsInstant() {
            // ARRANGE
            testSession.setFinished(true);
            testSession.setCreatedAt(Instant.now());
            testSession.setTimeTracked(0L);
            
            TimeBlock tb = new TimeBlock();
            tb.setStartDate(testSession.getCreatedAt());
            tb.setEndDate(testSession.getCreatedAt()); 
            testSession.setTimeBlocks(List.of(tb));

            when(sessionDAO.findFinishedSessionsInInterval(eq(testUser), any(), any())).thenReturn(List.of(testSession));

            // ACT
            Float ratio = sessionService.getWorkingTimeRatio(testUser, "Today", ZoneId.of("UTC"));

            // ASSERT
            assertEquals(0.0f, ratio, "Poměr by měl být 0 a kód nesmí spadnout na dělení nulou");
        }

        @Test
        void secondsTrackedForInterval_FiltersByDate() {
            // ARRANGE
            ZoneId zone = ZoneId.of("UTC");
            Instant now = Instant.now();
            when(sessionDAO.sumTimeTrackedByUserAndInterval(eq(testUser), any(), any())).thenReturn(3600L);

            // ACT
            Long result = sessionService.secondsTrackedForInterval(testUser, "ThisWeek", zone);

            // ASSERT
            assertEquals(3600L, result, "Měla by se započítat jen dnešní session");
        }
    }
}
