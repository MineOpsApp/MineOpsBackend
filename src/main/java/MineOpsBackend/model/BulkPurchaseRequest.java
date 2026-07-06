package MineOpsBackend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bulk_purchase_request")
public class BulkPurchaseRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String site;

    @Column(nullable = false)
    private String mineralType;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantityAvailable;

    @Column(nullable = false)
    private String unit;

    @Column(nullable = false)
    private String status = "AVAILABLE";

    @Column(nullable = false)
    private String flaggedByEmail;

    private LocalDateTime createdAt;

    public BulkPurchaseRequest() {}

    public Long getId() { return id; }
    public String getSite() { return site; }
    public void setSite(String v) { this.site = v; }
    public String getMineralType() { return mineralType; }
    public void setMineralType(String v) { this.mineralType = v; }
    public BigDecimal getQuantityAvailable() { return quantityAvailable; }
    public void setQuantityAvailable(BigDecimal v) { this.quantityAvailable = v; }
    public String getUnit() { return unit; }
    public void setUnit(String v) { this.unit = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getFlaggedByEmail() { return flaggedByEmail; }
    public void setFlaggedByEmail(String v) { this.flaggedByEmail = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
