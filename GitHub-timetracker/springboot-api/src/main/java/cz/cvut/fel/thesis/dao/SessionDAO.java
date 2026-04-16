package cz.cvut.fel.thesis.dao;

import cz.cvut.fel.thesis.model.Issue;
import cz.cvut.fel.thesis.model.Session;
import cz.cvut.fel.thesis.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Sort;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SessionDAO extends JpaRepository<Session, Long> {
    List<Session> findByUser(User user);
    List<Session> findByUser(User user, Sort sort);
    Optional<Session> findByIdAndUser(Long id, User user);
    List<Session> findByIssueAndUser(Issue issue, User user);
    List<Session> findByIssueAndUserAndFinishedTrue(Issue issue, User user);
    Optional<Session> findByUserAndFinishedFalse(User user);
    List<Session> findByIssueAndUser(Issue issue, User user, Sort sort);
    List<Session> findByUserAndFinishedTrue(User user, Sort sort);
    List<Session> findByUserAndSyncedFalseAndFinishedTrue(User user);
    List<Session> findByIssueAndUserOrderByCreatedAtDesc(Issue issue, User user);

    @Query("""
    SELECT DISTINCT s FROM Session s 
    JOIN s.issue i 
    JOIN i.repository r 
    JOIN s.timeBlocks tb
    WHERE s.user = :user 
    AND s.finished = true
    AND (:repoName IS NULL OR :repoName = '' OR r.name = :repoName)
    AND (:issueTitle IS NULL OR :issueTitle = '' OR i.title = :issueTitle)
    AND (tb.startDate >= :start AND tb.startDate < :end)
    ORDER BY s.id DESC
""")
List<Session> findSessionsForExport(
    @Param("user") User user, 
    @Param("repoName") String repoName, 
    @Param("issueTitle") String issueTitle,
    @Param("start") Instant start, 
    @Param("end") Instant end
);

    @Query("SELECT s FROM Session s WHERE s.user = :user AND s.finished = true AND s.createdAt >= :start AND s.createdAt < :end")
    List<Session> findFinishedSessionsInInterval(@Param("user") User user, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT SUM(s.timeTracked) FROM Session s WHERE s.user = :user AND s.finished = true AND s.createdAt >= :start AND s.createdAt < :end")
    Long sumTimeTrackedByUserAndInterval(@Param("user") User user, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT i, SUM(s.timeTracked) as totalTime " +
           "FROM Session s JOIN s.issue i " +
           "WHERE s.user = :user AND s.finished = true " +
           "GROUP BY i " +
           "ORDER BY totalTime DESC")
    List<Object[]> getTopIssuesByTime(@Param("user") User user); 

    @Query("SELECT l, SUM(s.timeTracked) as totalTime " +
           "FROM Session s JOIN s.issue i JOIN i.labels l " +
           "WHERE s.user = :user AND s.finished = true " +
           "GROUP BY l " +
           "ORDER BY totalTime DESC")
    List<Object[]> getTimeTrackedPerLabel(@Param("user") User user);

    @Query("""
        SELECT i.githubId, COALESCE(SUM(s.timeTracked), 0)
        FROM Session s
        JOIN s.issue i
        WHERE s.user = :user
        AND i.githubId IN :githubIds
        AND s.finished = true
        GROUP BY i.githubId
        """)
    List<Object[]> getTrackedSecondsByIssueGithubIds(@Param("user") User user, @Param("githubIds") Collection<Long> githubIds);

    @Query("""
        SELECT i.githubId, COUNT(s)
        FROM Session s
        JOIN s.issue i
        WHERE s.user = :user
        AND i.githubId IN :githubIds
        AND s.finished = true
        AND s.synced = false
        GROUP BY i.githubId
        """)
    List<Object[]> countUnsyncedFinishedByIssueGithubIds(@Param("user") User user, @Param("githubIds") Collection<Long> githubIds);
}
