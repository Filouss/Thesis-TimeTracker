package cz.cvut.fel.thesis.model;

import jakarta.persistence.*;

import java.util.Set;

@Entity
public class Session {
    @Id
    private Long id;

    @Column
    private boolean active;

    @Column
    private int duration;

    @Column
    private long issueID;

    @Column
    private String notes;

    @Column
    private boolean synced;

    @ManyToOne
    @JoinColumn(name = "issue_id")
    private Issue issue;

    @OneToMany(mappedBy = "session")
    private Set<TimeBlock> timeBlocks;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
