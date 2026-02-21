package cz.cvut.fel.thesis.dao;

import cz.cvut.fel.thesis.model.TimeBlock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeBlockDAO extends JpaRepository<TimeBlock, Long> {
}
