package cz.cvut.fel.thesis.dao;

import cz.cvut.fel.thesis.model.Issue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueDAO extends JpaRepository<Issue, Long> {
}
