package MineOpsBackend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "pay_split_config")
public class PaySplitConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String site;
    private String formulaType; // EQUAL_PER_HEAD | WEIGHTED_BY_HOURS
    private String updatedBy;
    private LocalDateTime updatedAt;

    public PaySplitConfig() {}

    public PaySplitConfig(String site, String formulaType, String updatedBy) {
        this.site = site;
        this.formulaType = formulaType;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getSite() { return site; }
    public void setSite(String v) { this.site = v; }
    public String getFormulaType() { return formulaType; }
    public void setFormulaType(String v) { this.formulaType = v; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String v) { this.updatedBy = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}
