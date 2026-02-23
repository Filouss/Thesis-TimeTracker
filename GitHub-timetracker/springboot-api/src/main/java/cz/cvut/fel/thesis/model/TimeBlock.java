package cz.cvut.fel.thesis.model;

import jakarta.persistence.*;

import java.util.Date;

@Entity
public class TimeBlock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Date endDate;

    @Column
    private Date startDate;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private Session session;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
