package cz.cvut.fel.thesis.model;

import jakarta.persistence.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@Entity
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private boolean active;

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

    @Transient
    public Duration getDuration(){
        if (timeBlocks == null || timeBlocks.isEmpty()) return Duration.ZERO;

        Instant now = Instant.now();

        return timeBlocks.stream()
                .filter(tb -> tb.getStartDate() != null)
                .map(tb -> {
                    Instant endDate = tb.getEndDate() != null ? tb.getEndDate() : now;
                    return Duration.between(tb.getStartDate(), endDate);
                })
                .reduce(Duration.ZERO, Duration::plus);
    }

    public TimeBlock getMostRecentTimeBlock(){
        if (timeBlocks == null || timeBlocks.isEmpty()) return null;
        TimeBlock mostRecent = null;
        for (TimeBlock tb : timeBlocks) {
            if (mostRecent == null ||
                    tb.getStartDate().isAfter(mostRecent.getStartDate())) {
                mostRecent = tb;
            }
        }
        return mostRecent;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }

    public Issue getIssue() {
        return issue;
    }

    public void setIssue(Issue issue) {
        this.issue = issue;
    }

    public Set<TimeBlock> getTimeBlocks() {
        return timeBlocks;
    }

    public void setTimeBlocks(Set<TimeBlock> timeBlocks) {
        this.timeBlocks = timeBlocks;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
