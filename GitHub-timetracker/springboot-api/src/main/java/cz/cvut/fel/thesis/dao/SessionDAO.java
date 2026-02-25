package cz.cvut.fel.thesis.dao;

import cz.cvut.fel.thesis.model.Label;
import cz.cvut.fel.thesis.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SessionDAO extends JpaRepository<Session, Long> {
//    Optional<Session> findById(Long ID);
}
