package cz.cvut.fel.thesis.dao;

import cz.cvut.fel.thesis.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserDAO extends JpaRepository<User, Long> {
    Optional<User> findByGitHubID(Long githubID);
}
