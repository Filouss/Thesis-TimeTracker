package cz.cvut.fel.thesis.model;

import jakarta.persistence.*;

import java.util.Set;

@Entity
public class Issue {
    @Id
    private Long id;

    @Column
    private String title;

    @Column
    private int issueNumber;

    @Column(unique=true)
    private Long GitHub_ID;

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

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
