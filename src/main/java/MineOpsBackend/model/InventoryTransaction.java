package MineOpsBackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_transactions")
public class InventoryTransaction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String site;
    @Column(nullable = false) private String mineralType;
    @Column(nullable = false) private String unit;
    @Column(nullable = false, precision = 15, scale = 4) private BigDecimal volumeAdded;
    @Column(nullable = false) private Long shiftLogId;
    @Column(nullable = false) private String workerName;
    @Column(nullable = false) private String workerEmail;
    private String zone;
    private String approvedBy;
    @Column(nullable = false) private LocalDateTime createdAt;

    public InventoryTransaction() {}

    public InventoryTransaction(
        String site, String mineralType, String unit,
        BigDecimal volumeAdded, Long shiftLogId,
        String workerName, String workerEmail,
        String zone, String approvedBy
    ) {
        this.site = site;
        this.mineralType = mineralType;
        this.unit = unit;
        this.volumeAdded = volumeAdded;
        this.shiftLogId = shiftLogId;
        this.workerName = workerName;
        this.workerEmail = workerEmail;
        this.zone = zone;
        this.approvedBy = approvedBy;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getSite() { return site; }
    public String getMineralType() { return mineralType; }
    public String getUnit() { return unit; }
    public BigDecimal getVolumeAdded() { return volumeAdded; }
    public Long getShiftLogId() { return shiftLogId; }
    public String getWorkerName() { return workerName; }
    public String getWorkerEmail() { return workerEmail; }
    public String getZone() { return zone; }
    public String getApprovedBy() { return approvedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
