package MineOpsBackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "incident_reports")
public class IncidentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String reportedByEmail;
    private String reportedByName;
    private String reportedByRole;
    private String site;
    private String zone;
    private String category; // Injury, Near Miss, Equipment Damage, Environmental
    private String severity; // Minor, Serious, Critical
    private String description;
    private String involvedPersons;
    private Boolean firstAidGiven;
    private Boolean hospitalRequired;
    private String immediateAction;
    private String status; // Open, Under Investigation, Closed
    private Double latitude;
    private Double longitude;

    @Column(columnDefinition = "TEXT")
    private String photoData;

    @Column(columnDefinition = "TEXT")
    private String investigationNotes;

    @Column(name = "client_request_id")
    private String clientRequestId;

    private LocalDateTime incidentAt;
    private LocalDateTime reportedAt;
    private LocalDateTime updatedAt;

    public IncidentReport() {}

    public IncidentReport(
        String reportedByEmail, String reportedByName, String reportedByRole,
        String site, String zone, String category, String severity,
        String description, String involvedPersons,
        Boolean firstAidGiven, Boolean hospitalRequired,
        String immediateAction, Double latitude, Double longitude,
        String photoData, LocalDateTime incidentAt
    ) {
        this.reportedByEmail = reportedByEmail;
        this.reportedByName = reportedByName;
        this.reportedByRole = reportedByRole;
        this.site = site;
        this.zone = zone;
        this.category = category;
        this.severity = severity;
        this.description = description;
        this.involvedPersons = involvedPersons;
        this.firstAidGiven = firstAidGiven;
        this.hospitalRequired = hospitalRequired;
        this.immediateAction = immediateAction;
        this.latitude = latitude;
        this.longitude = longitude;
        this.photoData = photoData;
        this.incidentAt = incidentAt != null ? incidentAt : LocalDateTime.now();
        this.reportedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = "Open";
    }

    public Long getId() { return id; }
    public String getReportedByEmail() { return reportedByEmail; }
    public void setReportedByEmail(String v) { this.reportedByEmail = v; }
    public String getReportedByName() { return reportedByName; }
    public void setReportedByName(String v) { this.reportedByName = v; }
    public String getReportedByRole() { return reportedByRole; }
    public void setReportedByRole(String v) { this.reportedByRole = v; }
    public String getSite() { return site; }
    public void setSite(String v) { this.site = v; }
    public String getZone() { return zone; }
    public void setZone(String v) { this.zone = v; }
    public String getCategory() { return category; }
    public void setCategory(String v) { this.category = v; }
    public String getSeverity() { return severity; }
    public void setSeverity(String v) { this.severity = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getInvolvedPersons() { return involvedPersons; }
    public void setInvolvedPersons(String v) { this.involvedPersons = v; }
    public Boolean getFirstAidGiven() { return firstAidGiven; }
    public void setFirstAidGiven(Boolean v) { this.firstAidGiven = v; }
    public Boolean getHospitalRequired() { return hospitalRequired; }
    public void setHospitalRequired(Boolean v) { this.hospitalRequired = v; }
    public String getImmediateAction() { return immediateAction; }
    public void setImmediateAction(String v) { this.immediateAction = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double v) { this.latitude = v; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double v) { this.longitude = v; }
    public String getPhotoData() { return photoData; }
    public void setPhotoData(String v) { this.photoData = v; }

    // Snapshot of "did this have a photo" taken at the moment the list endpoint strips
    // photoData for the response — see stripPhotoDataForList().
    @jakarta.persistence.Transient
    private Boolean hasPhotoOverride;

    // Computed — not stored in DB. Lets list views show a "has photo" indicator without
    // pulling the (potentially large) base64 photoData into every response.
    public boolean isHasPhoto() {
        if (hasPhotoOverride != null) return hasPhotoOverride;
        return photoData != null && !photoData.isBlank();
    }

    // Called by list endpoints in place of setPhotoData(null) directly — captures whether a
    // photo existed before nulling the field, so isHasPhoto() stays correct in the response.
    public void stripPhotoDataForList() {
        this.hasPhotoOverride = isHasPhoto();
        this.photoData = null;
    }
    public LocalDateTime getIncidentAt() { return incidentAt; }
    public void setIncidentAt(LocalDateTime v) { this.incidentAt = v; }
    public LocalDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(LocalDateTime v) { this.reportedAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }

    public String getInvestigationNotes() { return investigationNotes; }
    public void setInvestigationNotes(String v) { this.investigationNotes = v; }
    public String getClientRequestId() { return clientRequestId; }
    public void setClientRequestId(String v) { this.clientRequestId = v; }
}