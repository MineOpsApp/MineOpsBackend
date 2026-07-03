package MineOpsBackend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pay_cycle")
public class PayCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String site;
    private String payDate; // stored as "YYYY-MM-DD" string, matches shift_logs.shift_date
    private String mineralType;
    private String unit;
    private BigDecimal totalVolume;
    private BigDecimal pricePerUnit;
    private BigDecimal grossTotal;
    private String formulaType;
    private String status; // DRAFT | MANAGER_APPROVED | DISBURSED | FAILED
    private String createdBy;
    private LocalDateTime createdAt;
    private String managerApprovedBy;
    private LocalDateTime managerApprovedAt;
    private String supervisorApprovedBy;
    private LocalDateTime supervisorApprovedAt;

    public PayCycle() {}

    public PayCycle(String site, String payDate, String mineralType, String unit,
                    BigDecimal totalVolume, BigDecimal pricePerUnit, BigDecimal grossTotal,
                    String formulaType, String createdBy) {
        this.site = site;
        this.payDate = payDate;
        this.mineralType = mineralType;
        this.unit = unit;
        this.totalVolume = totalVolume;
        this.pricePerUnit = pricePerUnit;
        this.grossTotal = grossTotal;
        this.formulaType = formulaType;
        this.status = "DRAFT";
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getSite() { return site; }
    public void setSite(String v) { this.site = v; }
    public String getPayDate() { return payDate; }
    public void setPayDate(String v) { this.payDate = v; }
    public String getMineralType() { return mineralType; }
    public void setMineralType(String v) { this.mineralType = v; }
    public String getUnit() { return unit; }
    public void setUnit(String v) { this.unit = v; }
    public BigDecimal getTotalVolume() { return totalVolume; }
    public void setTotalVolume(BigDecimal v) { this.totalVolume = v; }
    public BigDecimal getPricePerUnit() { return pricePerUnit; }
    public void setPricePerUnit(BigDecimal v) { this.pricePerUnit = v; }
    public BigDecimal getGrossTotal() { return grossTotal; }
    public void setGrossTotal(BigDecimal v) { this.grossTotal = v; }
    public String getFormulaType() { return formulaType; }
    public void setFormulaType(String v) { this.formulaType = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String v) { this.createdBy = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public String getManagerApprovedBy() { return managerApprovedBy; }
    public void setManagerApprovedBy(String v) { this.managerApprovedBy = v; }
    public LocalDateTime getManagerApprovedAt() { return managerApprovedAt; }
    public void setManagerApprovedAt(LocalDateTime v) { this.managerApprovedAt = v; }
    public String getSupervisorApprovedBy() { return supervisorApprovedBy; }
    public void setSupervisorApprovedBy(String v) { this.supervisorApprovedBy = v; }
    public LocalDateTime getSupervisorApprovedAt() { return supervisorApprovedAt; }
    public void setSupervisorApprovedAt(LocalDateTime v) { this.supervisorApprovedAt = v; }
}
