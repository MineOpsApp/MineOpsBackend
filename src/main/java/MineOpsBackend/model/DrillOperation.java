package MineOpsBackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "drill_operations")
public class DrillOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String workerEmail;
    private String workerName;
    private String site;
    private String zone;
    private String equipmentCode;
    private String drillType;
    private String status;

    // Step sign-offs
    private Boolean stepSetupComplete;
    private LocalDateTime stepSetupAt;
    private String stepSetupNotes;

    private Boolean stepDrillingComplete;
    private LocalDateTime stepDrillingAt;
    private String stepDrillingNotes;

    private Boolean stepBlastingComplete;
    private LocalDateTime stepBlastingAt;
    private String stepBlastingNotes;

    private Boolean stepCleanupComplete;
    private LocalDateTime stepCleanupAt;
    private String stepCleanupNotes;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public DrillOperation() {}

    public DrillOperation(String workerEmail, String workerName, String site, String zone, String equipmentCode, String drillType) {
        this.workerEmail = workerEmail;
        this.workerName = workerName;
        this.site = site;
        this.zone = zone;
        this.equipmentCode = equipmentCode;
        this.drillType = drillType;
        this.status = "IN_PROGRESS";
        this.stepSetupComplete = false;
        this.stepDrillingComplete = false;
        this.stepBlastingComplete = false;
        this.stepCleanupComplete = false;
        this.startedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getWorkerEmail() { return workerEmail; }
    public void setWorkerEmail(String v) { this.workerEmail = v; }
    public String getWorkerName() { return workerName; }
    public void setWorkerName(String v) { this.workerName = v; }
    public String getSite() { return site; }
    public void setSite(String v) { this.site = v; }
    public String getZone() { return zone; }
    public void setZone(String v) { this.zone = v; }
    public String getEquipmentCode() { return equipmentCode; }
    public void setEquipmentCode(String v) { this.equipmentCode = v; }
    public String getDrillType() { return drillType; }
    public void setDrillType(String v) { this.drillType = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public Boolean getStepSetupComplete() { return stepSetupComplete; }
    public void setStepSetupComplete(Boolean v) { this.stepSetupComplete = v; }
    public LocalDateTime getStepSetupAt() { return stepSetupAt; }
    public void setStepSetupAt(LocalDateTime v) { this.stepSetupAt = v; }
    public String getStepSetupNotes() { return stepSetupNotes; }
    public void setStepSetupNotes(String v) { this.stepSetupNotes = v; }
    public Boolean getStepDrillingComplete() { return stepDrillingComplete; }
    public void setStepDrillingComplete(Boolean v) { this.stepDrillingComplete = v; }
    public LocalDateTime getStepDrillingAt() { return stepDrillingAt; }
    public void setStepDrillingAt(LocalDateTime v) { this.stepDrillingAt = v; }
    public String getStepDrillingNotes() { return stepDrillingNotes; }
    public void setStepDrillingNotes(String v) { this.stepDrillingNotes = v; }
    public Boolean getStepBlastingComplete() { return stepBlastingComplete; }
    public void setStepBlastingComplete(Boolean v) { this.stepBlastingComplete = v; }
    public LocalDateTime getStepBlastingAt() { return stepBlastingAt; }
    public void setStepBlastingAt(LocalDateTime v) { this.stepBlastingAt = v; }
    public String getStepBlastingNotes() { return stepBlastingNotes; }
    public void setStepBlastingNotes(String v) { this.stepBlastingNotes = v; }
    public Boolean getStepCleanupComplete() { return stepCleanupComplete; }
    public void setStepCleanupComplete(Boolean v) { this.stepCleanupComplete = v; }
    public LocalDateTime getStepCleanupAt() { return stepCleanupAt; }
    public void setStepCleanupAt(LocalDateTime v) { this.stepCleanupAt = v; }
    public String getStepCleanupNotes() { return stepCleanupNotes; }
    public void setStepCleanupNotes(String v) { this.stepCleanupNotes = v; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime v) { this.startedAt = v; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime v) { this.completedAt = v; }
}