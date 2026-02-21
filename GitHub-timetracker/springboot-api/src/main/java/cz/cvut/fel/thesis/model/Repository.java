package cz.cvut.fel.thesis.model;

import jakarta.persistence.*;

import java.util.Set;

@Entity
public class Repository {
    @Id
    private Long id;

    @Column
    private String title;

    @Column(unique=true)
    private long GitHub_ID;

    @OneToMany(mappedBy = "repository", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Issue> issues;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
