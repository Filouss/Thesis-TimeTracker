package cz.cvut.fel.thesis.dao;

import cz.cvut.fel.thesis.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDAO extends JpaRepository<User, Long> {
}
