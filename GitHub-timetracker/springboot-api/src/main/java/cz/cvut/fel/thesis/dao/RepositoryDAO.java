package cz.cvut.fel.thesis.dao;

import cz.cvut.fel.thesis.model.Repository;
import cz.cvut.fel.thesis.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RepositoryDAO extends JpaRepository<Repository, Long> {
    Optional<Repository> findByOwnerAndName(String owner, String name);

    @Query("""
        SELECT DISTINCT r.name 
        FROM Session s 
        JOIN s.issue i 
        JOIN i.repository r 
        WHERE s.user = :user 
        AND r.name LIKE %:query%
        """)
    List<String> findRepoSuggestionsForUser(
        @Param("user") User user, 
        @Param("query") String query
    );
}
