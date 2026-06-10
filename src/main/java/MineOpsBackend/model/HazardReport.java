package MineOpsBackend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "hazard_reports")
public class HazardReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String reportedByRole;

    private String site;

    private String description;

    private String status;

    private LocalDateTime createdAt;

    public HazardReport() {
    }

    public HazardReport(String reportedByRole, String site, String description) {
        this.reportedByRole = reportedByRole;
        this.site = site;
        this.description = description;
        this.status = "Open";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getReportedByRole() {
        return reportedByRole;
    }

    public void setReportedByRole(String reportedByRole) {
        this.reportedByRole = reportedByRole;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
