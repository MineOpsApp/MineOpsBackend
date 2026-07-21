package MineOpsBackend.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inspection_records")
public class InspectionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long inspectorUserId;
    private String site;

    @Column(length = 50)
    private String inspectionType;

    private String inspectionReferenceNumber;

    @Column(length = 50)
    private String scope;

    private String legalAuthorityReference;
    private String expectedDuration;

    private LocalDateTime inspectionStartAt;
    private LocalDateTime inspectionEndAt;

    @Column(columnDefinition = "TEXT")
    private String zonesInspected;

    @Column(columnDefinition = "TEXT")
    private String findingsSummary;

    @Column(length = 50)
    private String complianceStatus;

    private Boolean followUpRequired = false;
    private Boolean reportSubmitted = false;
    private LocalDate nextInspectionDate;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }

    public Long getInspectorUserId() { return inspectorUserId; }
    public void setInspectorUserId(Long v) { this.inspectorUserId = v; }

    public String getSite() { return site; }
    public void setSite(String v) { this.site = v; }

    public String getInspectionType() { return inspectionType; }
    public void setInspectionType(String v) { this.inspectionType = v; }

    public String getInspectionReferenceNumber() { return inspectionReferenceNumber; }
    public void setInspectionReferenceNumber(String v) { this.inspectionReferenceNumber = v; }

    public String getScope() { return scope; }
    public void setScope(String v) { this.scope = v; }

    public String getLegalAuthorityReference() { return legalAuthorityReference; }
    public void setLegalAuthorityReference(String v) { this.legalAuthorityReference = v; }

    public String getExpectedDuration() { return expectedDuration; }
    public void setExpectedDuration(String v) { this.expectedDuration = v; }

    public LocalDateTime getInspectionStartAt() { return inspectionStartAt; }
    public void setInspectionStartAt(LocalDateTime v) { this.inspectionStartAt = v; }

    public LocalDateTime getInspectionEndAt() { return inspectionEndAt; }
    public void setInspectionEndAt(LocalDateTime v) { this.inspectionEndAt = v; }

    public String getZonesInspected() { return zonesInspected; }
    public void setZonesInspected(String v) { this.zonesInspected = v; }

    public String getFindingsSummary() { return findingsSummary; }
    public void setFindingsSummary(String v) { this.findingsSummary = v; }

    public String getComplianceStatus() { return complianceStatus; }
    public void setComplianceStatus(String v) { this.complianceStatus = v; }

    public Boolean getFollowUpRequired() { return followUpRequired; }
    public void setFollowUpRequired(Boolean v) { this.followUpRequired = v; }

    public Boolean getReportSubmitted() { return reportSubmitted; }
    public void setReportSubmitted(Boolean v) { this.reportSubmitted = v; }

    public LocalDate getNextInspectionDate() { return nextInspectionDate; }
    public void setNextInspectionDate(LocalDate v) { this.nextInspectionDate = v; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
