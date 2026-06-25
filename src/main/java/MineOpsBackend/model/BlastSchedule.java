package MineOpsBackend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "blast_schedules")
public class BlastSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String site;
    private String zone;
    private String scheduledBy;
    private String scheduledByName;
    private String notes;
    private String status; // SCHEDULED, EXECUTED, CANCELLED
    private LocalDateTime blastTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BlastSchedule() {}

    public BlastSchedule(String site, String zone, String scheduledBy, String scheduledByName, String notes, LocalDateTime blastTime) {
        this.site = site;
        this.zone = zone;
        this.scheduledBy = scheduledBy;
        this.scheduledByName = scheduledByName;
        this.notes = notes;
        this.blastTime = blastTime;
        this.status = "SCHEDULED";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getSite() { return site; }
    public void setSite(String v) { this.site = v; }
    public String getZone() { return zone; }
    public void setZone(String v) { this.zone = v; }
    public String getScheduledBy() { return scheduledBy; }
    public void setScheduledBy(String v) { this.scheduledBy = v; }
    public String getScheduledByName() { return scheduledByName; }
    public void setScheduledByName(String v) { this.scheduledByName = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public LocalDateTime getBlastTime() { return blastTime; }
    public void setBlastTime(LocalDateTime v) { this.blastTime = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}