package MineOpsBackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mineral_inventory", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"site", "mineral_type", "unit"})
})
public class MineralInventory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String site;
    @Column(nullable = false) private String mineralType;
    @Column(nullable = false) private String unit;
    @Column(nullable = false, precision = 15, scale = 4) private BigDecimal totalVolume = BigDecimal.ZERO;

    private LocalDateTime lastUpdatedAt;
    private Long lastShiftLogId;
    private String lastWorkerName;
    private String lastZone;

    public MineralInventory() {}

    public MineralInventory(String site, String mineralType, String unit) {
        this.site = site;
        this.mineralType = mineralType;
        this.unit = unit;
        this.totalVolume = BigDecimal.ZERO;
    }

    public Long getId() { return id; }
    public String getSite() { return site; }
    public String getMineralType() { return mineralType; }
    public String getUnit() { return unit; }
    public BigDecimal getTotalVolume() { return totalVolume; }
    public void setTotalVolume(BigDecimal v) { this.totalVolume = v; }
    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(LocalDateTime v) { this.lastUpdatedAt = v; }
    public Long getLastShiftLogId() { return lastShiftLogId; }
    public void setLastShiftLogId(Long v) { this.lastShiftLogId = v; }
    public String getLastWorkerName() { return lastWorkerName; }
    public void setLastWorkerName(String v) { this.lastWorkerName = v; }
    public String getLastZone() { return lastZone; }
    public void setLastZone(String v) { this.lastZone = v; }
}
