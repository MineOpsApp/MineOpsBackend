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
    private LocalDateTime shiftDate;
    private LocalDateTime submittedAt;

    public ShiftLog() {
    }

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
        this.shiftDate = LocalDateTime.now();
        this.submittedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getWorkerEmail() { return workerEmail; }
    public void setWorkerEmail(String workerEmail) { this.workerEmail = workerEmail; }
    public String getWorkerName() { return workerName; }
    public void setWorkerName(String workerName) { this.workerName = workerName; }
    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }
    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }
    public String getShiftType() { return shiftType; }
    public void setShiftType(String shiftType) { this.shiftType = shiftType; }
    public String getMineralType() { return mineralType; }
    public void setMineralType(String mineralType) { this.mineralType = mineralType; }
    public java.math.BigDecimal getVolumeExtracted() { return volumeExtracted; }
public void setVolumeExtracted(java.math.BigDecimal volumeExtracted) { this.volumeExtracted = volumeExtracted; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getEquipmentCode() { return equipmentCode; }
    public void setEquipmentCode(String equipmentCode) { this.equipmentCode = equipmentCode; }
    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getShiftDate() { return shiftDate; }
    public void setShiftDate(LocalDateTime shiftDate) { this.shiftDate = shiftDate; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}