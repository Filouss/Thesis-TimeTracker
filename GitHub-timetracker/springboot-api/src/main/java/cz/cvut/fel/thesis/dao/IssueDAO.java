package cz.cvut.fel.thesis.dao;

import cz.cvut.fel.thesis.model.Issue;
import cz.cvut.fel.thesis.model.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IssueDAO extends JpaRepository<Issue, Long> {
    Optional<Issue> findByGithubId(Long githubID);
    List<Issue> findByTitleContainingIgnoreCase(String query);

    @Query("""
        SELECT DISTINCT i.title 
        FROM Session s 
        JOIN s.issue i 
        WHERE s.user = :user 
        AND i.title LIKE %:query%
        """)
    List<String> findIssueSuggestionsForUser(
        @Param("user") User user, 
        @Param("query") String query
    );
}
