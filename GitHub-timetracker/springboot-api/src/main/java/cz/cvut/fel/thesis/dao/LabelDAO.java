package cz.cvut.fel.thesis.dao;

import cz.cvut.fel.thesis.model.Label;
import cz.cvut.fel.thesis.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LabelDAO extends JpaRepository<Label, Long> {
}
