package cz.cvut.fel.thesis.dao;

import cz.cvut.fel.thesis.model.Issue;
import cz.cvut.fel.thesis.model.Session;
import cz.cvut.fel.thesis.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Sort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SessionDAO extends JpaRepository<Session, Long> {
    List<Session> findByUser(User user);
    List<Session> findByUser(User user, Sort sort);
    Optional<Session> findByIdAndUser(Long id, User user);
    List<Session> findByIssueAndUser(Issue issue, User user);
    List<Session> findByIssueAndUser(Issue issue, User user, Sort sort);

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
}
