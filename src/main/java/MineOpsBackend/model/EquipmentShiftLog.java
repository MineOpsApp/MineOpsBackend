package MineOpsBackend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "equipment_shift_logs")
public class EquipmentShiftLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String equipmentCode;
    private String equipmentName;
    private String workerEmail;
    private String workerName;
    private String status;
    private String checkType;
    private String notes;
    private LocalDateTime loggedAt;

    public EquipmentShiftLog() {
    }

    public EquipmentShiftLog(
        String equipmentCode,
        String equipmentName,
        String workerEmail,
        String workerName,
        String status,
        String checkType,
        String notes
    ) {
        this.equipmentCode = equipmentCode;
        this.equipmentName = equipmentName;
        this.workerEmail = workerEmail;
        this.workerName = workerName;
        this.status = status;
        this.checkType = checkType;
        this.notes = notes;
        this.loggedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getEquipmentCode() { return equipmentCode; }
    public void setEquipmentCode(String equipmentCode) { this.equipmentCode = equipmentCode; }
    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }
    public String getWorkerEmail() { return workerEmail; }
    public void setWorkerEmail(String workerEmail) { this.workerEmail = workerEmail; }
    public String getWorkerName() { return workerName; }
    public void setWorkerName(String workerName) { this.workerName = workerName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCheckType() { return checkType; }
    public void setCheckType(String checkType) { this.checkType = checkType; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
}
