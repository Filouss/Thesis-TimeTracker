package cz.cvut.fel.thesis.model;

import jakarta.persistence.*;

import java.util.Set;

@Entity
public class Label {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String title;

    @ManyToMany(mappedBy = "labels")
    private Set<Issue> issues;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
