package cz.cvut.fel.thesis.dao;

import cz.cvut.fel.thesis.model.Issue;
import cz.cvut.fel.thesis.model.Label;
import cz.cvut.fel.thesis.model.Session;
import cz.cvut.fel.thesis.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionDAO extends JpaRepository<Session, Long> {
    List<Session> findByUser(User user);
    Optional<Session> findByIdAndUser(Long id, User user);
    List<Session> findByIssueAndUser(Issue issue, User user);
}
