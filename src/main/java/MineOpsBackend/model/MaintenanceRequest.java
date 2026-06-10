package MineOpsBackend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "maintenance_requests")
public class MaintenanceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String workerEmail;

    private String equipmentCode;

    private String requestDetails;

    private String status;

    private LocalDateTime createdAt;

    public MaintenanceRequest() {
    }

    public MaintenanceRequest(String workerEmail, String equipmentCode, String requestDetails) {
        this.workerEmail = workerEmail;
        this.equipmentCode = equipmentCode;
        this.requestDetails = requestDetails;
        this.status = "Requested";
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

    public String getRequestDetails() {
        return requestDetails;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
