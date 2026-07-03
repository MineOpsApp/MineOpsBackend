package MineOpsBackend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "shift_logs")
public class ShiftLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String workerEmail;
    private String workerName;
    private String site;
    private String zone;
    private String shiftType;
    private String mineralType;
    private java.math.BigDecimal volumeExtracted;
    private String unit;
    private String equipmentCode;
    private String equipmentName;
    private String notes;
    private String status;
    private String shiftDate;
    private LocalDateTime submittedAt;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String rejectedBy;
    private LocalDateTime rejectedAt;
    private Long payCycleId;

    public ShiftLog() {}

    public ShiftLog(
        String workerEmail,
        String workerName,
        String site,
        String zone,
        String shiftType,
        String mineralType,
        java.math.BigDecimal volumeExtracted,
        String unit,
        String equipmentCode,
        String equipmentName,
        String notes
    ) {
        this.workerEmail = workerEmail;
        this.workerName = workerName;
        this.site = site;
        this.zone = zone;
        this.shiftType = shiftType;
        this.mineralType = mineralType;
        this.volumeExtracted = volumeExtracted;
        this.unit = unit;
        this.equipmentCode = equipmentCode;
        this.equipmentName = equipmentName;
        this.notes = notes;
        this.status = "SUBMITTED";
        this.submittedAt = LocalDateTime.now();
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
    public String getShiftType() { return shiftType; }
    public void setShiftType(String v) { this.shiftType = v; }
    public String getMineralType() { return mineralType; }
    public void setMineralType(String v) { this.mineralType = v; }
    public java.math.BigDecimal getVolumeExtracted() { return volumeExtracted; }
    public void setVolumeExtracted(java.math.BigDecimal v) { this.volumeExtracted = v; }
    public String getUnit() { return unit; }
    public void setUnit(String v) { this.unit = v; }
    public String getEquipmentCode() { return equipmentCode; }
    public void setEquipmentCode(String v) { this.equipmentCode = v; }
    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String v) { this.equipmentName = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getShiftDate() { return shiftDate; }
    public void setShiftDate(String v) { this.shiftDate = v; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime v) { this.submittedAt = v; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String v) { this.approvedBy = v; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime v) { this.approvedAt = v; }
    public String getRejectedBy() { return rejectedBy; }
    public void setRejectedBy(String v) { this.rejectedBy = v; }
    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(LocalDateTime v) { this.rejectedAt = v; }
    public Long getPayCycleId() { return payCycleId; }
    public void setPayCycleId(Long v) { this.payCycleId = v; }
}