package MineOpsBackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role;

    private String assignedSite;

    private LocalDateTime createdAt;

    private LocalDateTime sessionExpiresAt;

    private String guestSubRole;

    private String pushToken;

    private Boolean active = true;

    private Boolean pending = false;

    @Column(columnDefinition = "TEXT")
    private String profilePhoto;

    @Column(length = 500)
    private String bio;

    private String momoNumber;
    private String momoNetwork;

    private Long redeemedCodeId;
    private LocalDateTime inductionCompletedAt;
    private String phone;

    private String insuranceStatus = "NOT_INSURED";
    private LocalDateTime insuranceEnrolledAt;

    private String businessName;
    private String buyerVerificationStatus;
    @Column(columnDefinition = "TEXT")
    private String verificationDocument;

    public AppUser() {
    }

    public AppUser(String fullName, String email, String passwordHash, String role) {
        this(fullName, email, passwordHash, role, "Obuasi Mine");
    }

    public AppUser(String fullName, String email, String passwordHash, String role, String assignedSite) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.assignedSite = assignedSite;
        this.createdAt = LocalDateTime.now();
        if ("guest".equals(role)) {
            this.sessionExpiresAt = LocalDateTime.now().plusHours(24);
        }
    }

    public Long getId() { return id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getAssignedSite() { return assignedSite; }
    public void setAssignedSite(String assignedSite) { this.assignedSite = assignedSite; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getSessionExpiresAt() { return sessionExpiresAt; }
    public void setSessionExpiresAt(LocalDateTime sessionExpiresAt) { this.sessionExpiresAt = sessionExpiresAt; }

    public String getGuestSubRole() { return guestSubRole; }
    public void setGuestSubRole(String guestSubRole) { this.guestSubRole = guestSubRole; }

    public String getPushToken() { return pushToken; }
    public void setPushToken(String pushToken) { this.pushToken = pushToken; }


       public boolean isExpired() {
    return sessionExpiresAt != null && LocalDateTime.now(java.time.ZoneOffset.UTC).isAfter(sessionExpiresAt);
} 

public Boolean getActive() { return active; }
public void setActive(Boolean v) { this.active = v; }

public Boolean getPending() { return pending; }
public void setPending(Boolean pending) { this.pending = pending; }

public String getProfilePhoto() { return profilePhoto; }
public void setProfilePhoto(String profilePhoto) { this.profilePhoto = profilePhoto; }

public String getBio() { return bio; }
public void setBio(String bio) { this.bio = bio; }

public String getMomoNumber() { return momoNumber; }
public void setMomoNumber(String v) { this.momoNumber = v; }

public String getMomoNetwork() { return momoNetwork; }
public void setMomoNetwork(String v) { this.momoNetwork = v; }

public Long getRedeemedCodeId() { return redeemedCodeId; }
public void setRedeemedCodeId(Long v) { this.redeemedCodeId = v; }

public LocalDateTime getInductionCompletedAt() { return inductionCompletedAt; }
public void setInductionCompletedAt(LocalDateTime v) { this.inductionCompletedAt = v; }

public String getPhone() { return phone; }
public void setPhone(String v) { this.phone = v; }

public String getInsuranceStatus() { return insuranceStatus; }
public void setInsuranceStatus(String v) { this.insuranceStatus = v; }

public LocalDateTime getInsuranceEnrolledAt() { return insuranceEnrolledAt; }
public void setInsuranceEnrolledAt(LocalDateTime v) { this.insuranceEnrolledAt = v; }

public String getBusinessName() { return businessName; }
public void setBusinessName(String v) { this.businessName = v; }

public String getBuyerVerificationStatus() { return buyerVerificationStatus; }
public void setBuyerVerificationStatus(String v) { this.buyerVerificationStatus = v; }

public String getVerificationDocument() { return verificationDocument; }
public void setVerificationDocument(String v) { this.verificationDocument = v; }
}