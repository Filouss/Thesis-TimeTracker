package cz.cvut.fel.thesis.dao;

import cz.cvut.fel.thesis.model.Repository;
import cz.cvut.fel.thesis.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RepositoryDAO extends JpaRepository<Repository, Long> {
    Optional<Repository> findByOwnerAndName(String owner, String name);
}
