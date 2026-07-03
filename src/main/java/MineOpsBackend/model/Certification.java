package MineOpsBackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "certifications")
public class Certification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private Long workerId;
    @Column(nullable = false) private String workerName;
    @Column(nullable = false) private String workerEmail;
    @Column(nullable = false) private String site;
    @Column(nullable = false) private String certificationName;
    @Column(nullable = false) private String issuingAuthority;
    @Column(nullable = false) private LocalDate issueDate;
    @Column(nullable = false) private LocalDate expiryDate;
    private String notes;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    @Column(nullable = false) private String createdBy;

    public Certification() {}

    public Certification(
        Long workerId, String workerName, String workerEmail, String site,
        String certificationName, String issuingAuthority,
        LocalDate issueDate, LocalDate expiryDate,
        String notes, String createdBy
    ) {
        this.workerId = workerId;
        this.workerName = workerName;
        this.workerEmail = workerEmail;
        this.site = site;
        this.certificationName = certificationName;
        this.issuingAuthority = issuingAuthority;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.notes = notes;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Computed — not stored in DB, always fresh
    public String getStatus() {
        if (expiryDate == null) return "UNKNOWN";
        LocalDate now = LocalDate.now();
        if (expiryDate.isBefore(now)) return "EXPIRED";
        if (expiryDate.isBefore(now.plusDays(30))) return "EXPIRING_SOON";
        return "VALID";
    }

    public long getDaysUntilExpiry() {
        if (expiryDate == null) return 0;
        return ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }

    public Long getId() { return id; }
    public Long getWorkerId() { return workerId; }
    public String getWorkerName() { return workerName; }
    public String getWorkerEmail() { return workerEmail; }
    public String getSite() { return site; }
    public String getCertificationName() { return certificationName; }
    public void setCertificationName(String v) { this.certificationName = v; }
    public String getIssuingAuthority() { return issuingAuthority; }
    public void setIssuingAuthority(String v) { this.issuingAuthority = v; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate v) { this.issueDate = v; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate v) { this.expiryDate = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
    public String getCreatedBy() { return createdBy; }
}
