package cz.cvut.fel.thesis.model;

import jakarta.persistence.*;

import java.util.Set;

@Entity
public class Issue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String title;

    @Column
    private int issueNumber;

    @Column(name = "github_id",unique=true)
    private Long githubId;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "issue")
    private Set<Session> sessions;

    @Column
    private State state;

    @ManyToOne
    @JoinColumn(name = "repository_id")
    private Repository repository;

    @ManyToMany
    @JoinTable(
            name = "issue_label",
            joinColumns = @JoinColumn(name = "issue_id"),
            inverseJoinColumns = @JoinColumn(name = "label_id")
    )
    private Set<Label> labels;

    private int syncCommentsAmount;

    public void setLabels(Set<Label> labels) {
        this.labels = labels;
    }

    public Set<Label> getLabels() {
        return labels;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getIssueNumber() {
        return issueNumber;
    }

    public void setIssueNumber(int issueNumber) {
        this.issueNumber = issueNumber;
    }

    public Long getGithubId() {
        return githubId;
    }

    public void setGithubId(Long gitHubID) {
        githubId = gitHubID;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public int getSyncCommentsAmount() {
        return syncCommentsAmount;
    }

    public void setSyncCommentsAmount(int syncCommentsAmount) {
        this.syncCommentsAmount = syncCommentsAmount;
    }
}
