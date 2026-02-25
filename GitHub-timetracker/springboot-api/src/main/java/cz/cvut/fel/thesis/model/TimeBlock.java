package cz.cvut.fel.thesis.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Date;

@Entity
public class TimeBlock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Instant endDate;

    @Column
    private Instant startDate;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private Session session;

    public Instant getEndDate() {
        return endDate;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
