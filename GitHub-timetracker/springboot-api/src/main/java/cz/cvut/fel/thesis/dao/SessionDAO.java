package cz.cvut.fel.thesis.dao;

import cz.cvut.fel.thesis.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionDAO extends JpaRepository<Session, Long> {
}
