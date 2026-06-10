package MineOpsBackend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "equipment_faults")
public class EquipmentFault {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String workerEmail;

    private String equipmentCode;

    private String description;

    private String status;

    private LocalDateTime createdAt;

    public EquipmentFault() {
    }

    public EquipmentFault(String workerEmail, String equipmentCode, String description) {
        this.workerEmail = workerEmail;
        this.equipmentCode = equipmentCode;
        this.description = description;
        this.status = "Open";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getWorkerEmail() {
        return workerEmail;
    }

    public String getEquipmentCode() {
        return equipmentCode;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
