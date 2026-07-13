package MineOpsBackend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "illegal_mine_report")
public class IllegalMineReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String reporterEmail;

    @Column(nullable = false)
    private String reporterRole;

    @Column(nullable = false, length = 500)
    private String locationDescription;

    @Column(length = 1500)
    private String details;

    @Column(nullable = false)
    private String status = "SUBMITTED";

    private String reviewedByEmail;

    @Column(length = 1000)
    private String reviewNotes;

    @Column(columnDefinition = "TEXT")
    private String photoData;

    private Double latitude;

    private Double longitude;

    @Column(name = "client_request_id")
    private String clientRequestId;

    private LocalDateTime createdAt;

    public IllegalMineReport() {}

    public Long getId() { return id; }
    public String getReporterEmail() { return reporterEmail; }
    public void setReporterEmail(String v) { this.reporterEmail = v; }
    public String getReporterRole() { return reporterRole; }
    public void setReporterRole(String v) { this.reporterRole = v; }
    public String getLocationDescription() { return locationDescription; }
    public void setLocationDescription(String v) { this.locationDescription = v; }
    public String getDetails() { return details; }
    public void setDetails(String v) { this.details = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getReviewedByEmail() { return reviewedByEmail; }
    public void setReviewedByEmail(String v) { this.reviewedByEmail = v; }
    public String getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(String v) { this.reviewNotes = v; }
    public String getPhotoData() { return photoData; }
    public void setPhotoData(String v) { this.photoData = v; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double v) { this.latitude = v; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double v) { this.longitude = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public String getClientRequestId() { return clientRequestId; }
    public void setClientRequestId(String v) { this.clientRequestId = v; }
}
