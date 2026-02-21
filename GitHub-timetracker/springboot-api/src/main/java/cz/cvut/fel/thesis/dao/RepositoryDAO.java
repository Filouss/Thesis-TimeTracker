package cz.cvut.fel.thesis.dao;

import cz.cvut.fel.thesis.model.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryDAO extends JpaRepository<Repository, Long> {
}
