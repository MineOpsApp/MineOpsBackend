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
    private String reportedByName;
    private String reportedByEmail;
    private String hazardType;
    private String site;
    private String location;
    private String description;
    private String severity;
    private String status;
    private String reviewedByRole;
    private String reviewedByName;
    private String reviewedByEmail;
    private String closedByRole;
    private String closedByName;
    private String closedByEmail;
    private String actionTaken;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime closedAt;

    public HazardReport() {
    }

    public HazardReport(
        String reportedByRole,
        String reportedByName,
        String reportedByEmail,
        String hazardType,
        String site,
        String location,
        String description,
        String severity
    ) {
        this.reportedByRole = reportedByRole;
        this.reportedByName = reportedByName;
        this.reportedByEmail = reportedByEmail;
        this.hazardType = hazardType;
        this.site = site;
        this.location = location;
        this.description = description;
        this.severity = (severity != null && !severity.isBlank()) ? severity : "Medium";
        this.status = "OPEN";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public String getReportedByRole() { return reportedByRole; }
    public void setReportedByRole(String reportedByRole) { this.reportedByRole = reportedByRole; }

    public String getReportedByName() { return reportedByName; }
    public void setReportedByName(String reportedByName) { this.reportedByName = reportedByName; }

    public String getReportedByEmail() { return reportedByEmail; }
    public void setReportedByEmail(String reportedByEmail) { this.reportedByEmail = reportedByEmail; }

    public String getHazardType() { return hazardType; }
    public void setHazardType(String hazardType) { this.hazardType = hazardType; }

    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReviewedByRole() { return reviewedByRole; }
    public void setReviewedByRole(String reviewedByRole) { this.reviewedByRole = reviewedByRole; }

    public String getReviewedByName() { return reviewedByName; }
    public void setReviewedByName(String reviewedByName) { this.reviewedByName = reviewedByName; }

    public String getReviewedByEmail() { return reviewedByEmail; }
    public void setReviewedByEmail(String reviewedByEmail) { this.reviewedByEmail = reviewedByEmail; }

    public String getClosedByRole() { return closedByRole; }
    public void setClosedByRole(String closedByRole) { this.closedByRole = closedByRole; }

    public String getClosedByName() { return closedByName; }
    public void setClosedByName(String closedByName) { this.closedByName = closedByName; }

    public String getClosedByEmail() { return closedByEmail; }
    public void setClosedByEmail(String closedByEmail) { this.closedByEmail = closedByEmail; }

    public String getActionTaken() { return actionTaken; }
    public void setActionTaken(String actionTaken) { this.actionTaken = actionTaken; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
}