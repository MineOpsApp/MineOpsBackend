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

/**
 * Internal record of a site's regulatory permits/licenses (mining lease, environmental permit,
 * explosives license, etc.) with an optional scanned document attached. Not shown to workers —
 * this is a supervisor/safety-officer compliance record, not a worker-facing feature.
 */
@Entity
@Table(name = "site_permits")
public class SitePermit {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String site;
    @Column(nullable = false) private String permitName;
    private String permitNumber;
    @Column(nullable = false) private String issuingAuthority;
    @Column(nullable = false) private LocalDate issueDate;
    @Column(nullable = false) private LocalDate expiryDate;
    private String notes;
    @Column(columnDefinition = "TEXT") private String documentData;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    @Column(nullable = false) private String createdBy;

    public SitePermit() {}

    public SitePermit(
        String site, String permitName, String permitNumber, String issuingAuthority,
        LocalDate issueDate, LocalDate expiryDate, String notes, String createdBy
    ) {
        this.site = site;
        this.permitName = permitName;
        this.permitNumber = permitNumber;
        this.issuingAuthority = issuingAuthority;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.notes = notes;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Computed — not stored, always fresh
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

    // Snapshot of "did this have a document" taken at the moment the list endpoint strips
    // documentData for the response. Without this, isHasDocument() would read the already-nulled
    // documentData field at JSON-serialization time (which happens after the controller method
    // returns) and would always report false.
    @jakarta.persistence.Transient
    private Boolean hasDocumentOverride;

    // Computed — list views show a "has document" indicator without pulling the
    // (potentially large) base64 payload into every response.
    public boolean isHasDocument() {
        if (hasDocumentOverride != null) return hasDocumentOverride;
        return documentData != null && !documentData.isBlank();
    }

    // Called by list endpoints in place of setDocumentData(null) directly — captures whether a
    // document existed before nulling the field, so isHasDocument() stays correct in the response.
    public void stripDocumentDataForList() {
        this.hasDocumentOverride = isHasDocument();
        this.documentData = null;
    }

    public Long getId() { return id; }
    public String getSite() { return site; }
    public String getPermitName() { return permitName; }
    public void setPermitName(String v) { this.permitName = v; }
    public String getPermitNumber() { return permitNumber; }
    public void setPermitNumber(String v) { this.permitNumber = v; }
    public String getIssuingAuthority() { return issuingAuthority; }
    public void setIssuingAuthority(String v) { this.issuingAuthority = v; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate v) { this.issueDate = v; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate v) { this.expiryDate = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
    public String getDocumentData() { return documentData; }
    public void setDocumentData(String v) { this.documentData = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
    public String getCreatedBy() { return createdBy; }
}
