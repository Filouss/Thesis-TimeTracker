package cz.cvut.fel.thesis.model;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "app_user")
public class User {

    public User(String username, Long gitHub_ID) {
        this.username = username;
        gitHubID = gitHub_ID;
        sessions = new HashSet();
        tracking = false;
        pinnedIssueGithubIds = new HashSet();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique=true,name = "github_id")
    private Long gitHubID;

    @Column
    private String username;

    @OneToMany(mappedBy = "user")
    private Set<Session> sessions;

    private boolean tracking;

    private Long activeSessionID;

    @ElementCollection
    @CollectionTable(
            name = "user_pinned_issues",
            joinColumns = @JoinColumn(name = "user_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "issue_github_id"})
    )
    @Column(name = "issue_github_id")
    private Set<Long> pinnedIssueGithubIds = new HashSet<>();

    public User() {

    }

    public Set<Long> getPinnedIssueGithubIds() {
        return pinnedIssueGithubIds;
    }

    public void setTracking(boolean tracking) {
        this.tracking = tracking;
    }

    public Long getActiveSessionID() {
        return activeSessionID;
    }

    public void setActiveSessionID(Long activeSessionID) {
        this.activeSessionID = activeSessionID;
    }

    public Set<Session> getSessions() {
        return sessions;
    }

    public Long getGitHubID() {
        return gitHubID;
    }

    public void setGitHubID(long gitHubID) {
        this.gitHubID = gitHubID;
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

    public boolean isTracking() {
        return tracking;
    }
}
