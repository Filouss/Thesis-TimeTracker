package cz.cvut.fel.thesis.model;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "app_user")
public class User {
    @Id
    private Long id;

    @Column(unique=true)
    private long GitHub_ID;

    @Column
    private String username;

    @OneToMany(mappedBy = "user")
    private Set<Session> sessions;

    @ElementCollection
    @CollectionTable(
            name = "user_pinned_issues",
            joinColumns = @JoinColumn(name = "user_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "issue_github_id"})
    )
    @Column(name = "issue_github_id", nullable = false)
    private Set<Long> pinnedIssueGithubIds = new HashSet<>();

    public long getGitHub_ID() {
        return GitHub_ID;
    }

    public void setGitHub_ID(long gitHub_ID) {
        GitHub_ID = gitHub_ID;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
