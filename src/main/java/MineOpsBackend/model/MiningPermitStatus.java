package MineOpsBackend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mining_permit_status")
public class MiningPermitStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String site;

    private Boolean applicationSubmitted = false;
    private Boolean communityNotificationDone = false;
    private String ministerialReviewStatus;
    private Boolean epaPermitObtained = false;
    private String updatedByEmail;
    private LocalDateTime updatedAt;

    @Column(columnDefinition = "TEXT")
    private String photoData;

    public MiningPermitStatus() {}

    public Long getId() { return id; }
    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }
    public Boolean getApplicationSubmitted() { return applicationSubmitted; }
    public void setApplicationSubmitted(Boolean v) { this.applicationSubmitted = v; }
    public Boolean getCommunityNotificationDone() { return communityNotificationDone; }
    public void setCommunityNotificationDone(Boolean v) { this.communityNotificationDone = v; }
    public String getMinisterialReviewStatus() { return ministerialReviewStatus; }
    public void setMinisterialReviewStatus(String v) { this.ministerialReviewStatus = v; }
    public Boolean getEpaPermitObtained() { return epaPermitObtained; }
    public void setEpaPermitObtained(Boolean v) { this.epaPermitObtained = v; }
    public String getUpdatedByEmail() { return updatedByEmail; }
    public void setUpdatedByEmail(String v) { this.updatedByEmail = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
    public String getPhotoData() { return photoData; }
    public void setPhotoData(String v) { this.photoData = v; }
}
