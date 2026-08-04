package MineOpsBackend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "visitor_visits")
public class VisitorVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long guestUserId;
    private String hostName;

    // Only set when the supervisor picked the host from the site personnel directory (rather
    // than typing a free-text name) — that's what lets create() actually notify them.
    private String hostEmail;

    @Column(length = 500)
    private String purposeOfVisit;

    private String assignedSite;

    @Column(unique = true, length = 50)
    private String visitorPassNumber;

    private LocalDateTime visitStart;
    private LocalDateTime visitEnd;

    @Column(columnDefinition = "TEXT")
    private String approvedZones;

    private Boolean inductionCompleted = false;
    private LocalDateTime inductionCompletedAt;
    private String inductionSignOff;

    private String emergencyContactName;
    private String emergencyContactPhone;

    private Boolean ppeIssued = false;

    @Column(length = 500)
    private String ppeItems;

    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;

    @Column(columnDefinition = "TEXT")
    private String zonesVisited;

    @Column(length = 50)
    private String status = "PENDING";

    private String visitingOrganisation;
    private String relationshipToHost;

    @Column(length = 100)
    private String visitReason;

    @Column(length = 50)
    private String vehicleRegistrationNumber;

    private Integer groupSize;

    @Column(length = 500)
    private String medicalConditionsNote;

    private LocalDateTime createdAt = LocalDateTime.now();

    // Not a column — VisitorVisit only stores guestUserId. The controller resolves this from
    // AppUser at read/write time and sets it here purely so it rides along in the JSON response;
    // no migration needed to show the supervisor who VIS-000123 actually is.
    @Transient
    private String guestFullName;

    public String getGuestFullName() { return guestFullName; }
    public void setGuestFullName(String v) { this.guestFullName = v; }

    public Long getId() { return id; }

    public Long getGuestUserId() { return guestUserId; }
    public void setGuestUserId(Long v) { this.guestUserId = v; }

    public String getHostName() { return hostName; }
    public void setHostName(String v) { this.hostName = v; }

    public String getHostEmail() { return hostEmail; }
    public void setHostEmail(String v) { this.hostEmail = v; }

    public String getPurposeOfVisit() { return purposeOfVisit; }
    public void setPurposeOfVisit(String v) { this.purposeOfVisit = v; }

    public String getAssignedSite() { return assignedSite; }
    public void setAssignedSite(String v) { this.assignedSite = v; }

    public String getVisitorPassNumber() { return visitorPassNumber; }
    public void setVisitorPassNumber(String v) { this.visitorPassNumber = v; }

    public LocalDateTime getVisitStart() { return visitStart; }
    public void setVisitStart(LocalDateTime v) { this.visitStart = v; }

    public LocalDateTime getVisitEnd() { return visitEnd; }
    public void setVisitEnd(LocalDateTime v) { this.visitEnd = v; }

    public String getApprovedZones() { return approvedZones; }
    public void setApprovedZones(String v) { this.approvedZones = v; }

    public Boolean getInductionCompleted() { return inductionCompleted; }
    public void setInductionCompleted(Boolean v) { this.inductionCompleted = v; }

    public LocalDateTime getInductionCompletedAt() { return inductionCompletedAt; }
    public void setInductionCompletedAt(LocalDateTime v) { this.inductionCompletedAt = v; }

    public String getInductionSignOff() { return inductionSignOff; }
    public void setInductionSignOff(String v) { this.inductionSignOff = v; }

    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String v) { this.emergencyContactName = v; }

    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String v) { this.emergencyContactPhone = v; }

    public Boolean getPpeIssued() { return ppeIssued; }
    public void setPpeIssued(Boolean v) { this.ppeIssued = v; }

    public String getPpeItems() { return ppeItems; }
    public void setPpeItems(String v) { this.ppeItems = v; }

    public LocalDateTime getCheckInAt() { return checkInAt; }
    public void setCheckInAt(LocalDateTime v) { this.checkInAt = v; }

    public LocalDateTime getCheckOutAt() { return checkOutAt; }
    public void setCheckOutAt(LocalDateTime v) { this.checkOutAt = v; }

    public String getZonesVisited() { return zonesVisited; }
    public void setZonesVisited(String v) { this.zonesVisited = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public String getVisitingOrganisation() { return visitingOrganisation; }
    public void setVisitingOrganisation(String v) { this.visitingOrganisation = v; }

    public String getRelationshipToHost() { return relationshipToHost; }
    public void setRelationshipToHost(String v) { this.relationshipToHost = v; }

    public String getVisitReason() { return visitReason; }
    public void setVisitReason(String v) { this.visitReason = v; }

    public String getVehicleRegistrationNumber() { return vehicleRegistrationNumber; }
    public void setVehicleRegistrationNumber(String v) { this.vehicleRegistrationNumber = v; }

    public Integer getGroupSize() { return groupSize; }
    public void setGroupSize(Integer v) { this.groupSize = v; }

    public String getMedicalConditionsNote() { return medicalConditionsNote; }
    public void setMedicalConditionsNote(String v) { this.medicalConditionsNote = v; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
