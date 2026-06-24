package MineOpsBackend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_records")
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String workerEmail;
    private String workerName;
    private String workerRole;
    private String site;
    private String zone;
    private String status; // ON_SITE, OFF_SITE
    private LocalDateTime clockInAt;
    private LocalDateTime clockOutAt;
    private String notes;

    public AttendanceRecord() {}

    public AttendanceRecord(String workerEmail, String workerName, String workerRole, String site, String zone) {
        this.workerEmail = workerEmail;
        this.workerName = workerName;
        this.workerRole = workerRole;
        this.site = site;
        this.zone = zone;
        this.status = "ON_SITE";
        this.clockInAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getWorkerEmail() { return workerEmail; }
    public void setWorkerEmail(String v) { this.workerEmail = v; }
    public String getWorkerName() { return workerName; }
    public void setWorkerName(String v) { this.workerName = v; }
    public String getWorkerRole() { return workerRole; }
    public void setWorkerRole(String v) { this.workerRole = v; }
    public String getSite() { return site; }
    public void setSite(String v) { this.site = v; }
    public String getZone() { return zone; }
    public void setZone(String v) { this.zone = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public LocalDateTime getClockInAt() { return clockInAt; }
    public void setClockInAt(LocalDateTime v) { this.clockInAt = v; }
    public LocalDateTime getClockOutAt() { return clockOutAt; }
    public void setClockOutAt(LocalDateTime v) { this.clockOutAt = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
}