package cz.cvut.fel.thesis.model;

import jakarta.persistence.*;

import java.util.Set;

@Entity
public class Session {



    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private boolean active;

    @Column
    private int duration;

//    tohle nepotrebuju asi kdyz je vztah s issue
//    @Column
//    private long issueID;

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
