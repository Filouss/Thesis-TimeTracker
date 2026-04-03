package cz.cvut.fel.thesis.model;

import jakarta.persistence.*;

import java.util.Set;

/**
 * Represents a GitHub label associated with tracked issues.
 */
@Entity
public class Label {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String title;

    @Column(unique=true,name = "github_id")
    private Long gitHubID;

    @Column
    private String colorHEX;

    @ManyToMany(mappedBy = "labels")
    private Set<Issue> issues;

    public String getColorHEX() {
        return colorHEX;
    }

    public void setColorHEX(String colorHEX) {
        this.colorHEX = colorHEX;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getGitHubID() {
        return gitHubID;
    }

    public void setGitHubID(Long gitHubID) {
        this.gitHubID = gitHubID;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
