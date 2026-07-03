package MineOpsBackend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lone_worker_sessions")
public class LoneWorkerSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long workerId;

    @Column(nullable = false)
    private String workerEmail;

    @Column(nullable = false)
    private String workerName;

    @Column(nullable = false)
    private String site;

    @Column(nullable = false)
    private int intervalMinutes;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column(nullable = false)
    private LocalDateTime lastCheckedInAt;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private boolean alerted;

    @PrePersist
    void prePersist() {
        if (startedAt == null) startedAt = LocalDateTime.now();
        if (lastCheckedInAt == null) lastCheckedInAt = LocalDateTime.now();
        if (deadline == null) deadline = LocalDateTime.now().plusMinutes(intervalMinutes);
    }

    public Long getId() { return id; }
    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long workerId) { this.workerId = workerId; }
    public String getWorkerEmail() { return workerEmail; }
    public void setWorkerEmail(String workerEmail) { this.workerEmail = workerEmail; }
    public String getWorkerName() { return workerName; }
    public void setWorkerName(String workerName) { this.workerName = workerName; }
    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }
    public int getIntervalMinutes() { return intervalMinutes; }
    public void setIntervalMinutes(int intervalMinutes) { this.intervalMinutes = intervalMinutes; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getLastCheckedInAt() { return lastCheckedInAt; }
    public LocalDateTime getDeadline() { return deadline; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isAlerted() { return alerted; }
    public void setAlerted(boolean alerted) { this.alerted = alerted; }

    public void recordCheckIn() {
        this.lastCheckedInAt = LocalDateTime.now();
        this.deadline = LocalDateTime.now().plusMinutes(intervalMinutes);
        this.alerted = false;
    }
}
