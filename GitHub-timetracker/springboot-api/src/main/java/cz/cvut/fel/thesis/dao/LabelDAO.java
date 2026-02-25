package cz.cvut.fel.thesis.dao;

import cz.cvut.fel.thesis.model.Label;
import cz.cvut.fel.thesis.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;

public interface LabelDAO extends JpaRepository<Label, Long> {
    Optional<Label> findByGitHubID(Long githubID);
}
