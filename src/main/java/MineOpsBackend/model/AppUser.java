package MineOpsBackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
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

    private String homeSite;

    private LocalDateTime createdAt;

    private LocalDateTime sessionExpiresAt;

    private String guestSubRole;

    private String pushToken;

    private Boolean active = true;

    private Boolean pending = false;

    private Boolean notifyHazard = true;
    private Boolean notifyNotice = true;

    private Integer failedLoginAttempts = 0;
    private LocalDateTime lockedUntil;
    private LocalDateTime termsAcceptedAt;
    private LocalDateTime deletedAt;

    private Boolean mustChangePassword = false;

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
    private String goldbodLicenseNumber;
    private String governmentAgency;
    private String buyerVerificationStatus;
    @Column(columnDefinition = "TEXT")
    private String verificationDocument;

    private LocalDate dateOfBirth;

    @Column(length = 10)
    private String bloodType;

    @Column(columnDefinition = "TEXT")
    private String medicalNotes;

    @Column(length = 500)
    private String homeAddress;

    @Column(length = 50)
    private String nationalIdNumber;

    @Column(length = 50)
    private String ssnitNumber;

    @Column(length = 50)
    private String tinNumber;

    @Column(length = 100)
    private String jobTitle;

    @Column(length = 20)
    private String employmentType;

    private Boolean fitForDuty = true;

    // Common identity
    @Column(length = 100)
    private String nationality;

    // Buyer profile fields
    @Column(length = 100)
    private String companyRegistrationNumber;
    @Column(length = 100)
    private String countryOfIncorporation;
    @Column(length = 50)
    private String businessType;
    @Column(length = 100)
    private String positionTitle;
    @Column(columnDefinition = "TEXT")
    private String governmentIdDocument;
    @Column(length = 100)
    private String exportLicenceNumber;
    @Column(length = 255)
    private String operatingJurisdiction;
    @Column(length = 500)
    private String mineralsOfInterest;
    @Column(length = 255)
    private String typicalOrderVolume;
    @Column(length = 50)
    private String preferredTransactionMethod;
    private Boolean ndaSigned = false;

    // Government account fields
    @Column(length = 255)
    private String regulatoryBodyName;
    @Column(length = 100)
    private String officialIdBadgeNumber;
    @Column(length = 255)
    private String issuingAuthority;
    @Column(columnDefinition = "TEXT")
    private String officialIdDocument;
    @Column(length = 255)
    private String jurisdictionOfAuthority;

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
        this.homeSite = assignedSite;
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

    public String getHomeSite() { return homeSite; }
    public void setHomeSite(String homeSite) { this.homeSite = homeSite; }

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

public Boolean getNotifyHazard() { return notifyHazard; }
public void setNotifyHazard(Boolean v) { this.notifyHazard = v; }
public Boolean getNotifyNotice() { return notifyNotice; }
public void setNotifyNotice(Boolean v) { this.notifyNotice = v; }

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

public String getGoldbodLicenseNumber() { return goldbodLicenseNumber; }
public void setGoldbodLicenseNumber(String v) { this.goldbodLicenseNumber = v; }

public String getGovernmentAgency() { return governmentAgency; }
public void setGovernmentAgency(String v) { this.governmentAgency = v; }

public String getBuyerVerificationStatus() { return buyerVerificationStatus; }
public void setBuyerVerificationStatus(String v) { this.buyerVerificationStatus = v; }

public String getVerificationDocument() { return verificationDocument; }
public void setVerificationDocument(String v) { this.verificationDocument = v; }

public Integer getFailedLoginAttempts() { return failedLoginAttempts; }
public void setFailedLoginAttempts(Integer v) { this.failedLoginAttempts = v; }

public LocalDateTime getLockedUntil() { return lockedUntil; }
public void setLockedUntil(LocalDateTime v) { this.lockedUntil = v; }

public LocalDateTime getTermsAcceptedAt() { return termsAcceptedAt; }
public void setTermsAcceptedAt(LocalDateTime v) { this.termsAcceptedAt = v; }

public LocalDateTime getDeletedAt() { return deletedAt; }
public void setDeletedAt(LocalDateTime v) { this.deletedAt = v; }

public Boolean getMustChangePassword() { return mustChangePassword; }
public void setMustChangePassword(Boolean v) { this.mustChangePassword = v; }

public LocalDate getDateOfBirth() { return dateOfBirth; }
public void setDateOfBirth(LocalDate v) { this.dateOfBirth = v; }

public String getBloodType() { return bloodType; }
public void setBloodType(String v) { this.bloodType = v; }

public String getMedicalNotes() { return medicalNotes; }
public void setMedicalNotes(String v) { this.medicalNotes = v; }

public String getHomeAddress() { return homeAddress; }
public void setHomeAddress(String v) { this.homeAddress = v; }

public String getNationalIdNumber() { return nationalIdNumber; }
public void setNationalIdNumber(String v) { this.nationalIdNumber = v; }

public String getSsnitNumber() { return ssnitNumber; }
public void setSsnitNumber(String v) { this.ssnitNumber = v; }

public String getTinNumber() { return tinNumber; }
public void setTinNumber(String v) { this.tinNumber = v; }

public String getJobTitle() { return jobTitle; }
public void setJobTitle(String v) { this.jobTitle = v; }

public String getEmploymentType() { return employmentType; }
public void setEmploymentType(String v) { this.employmentType = v; }

public Boolean getFitForDuty() { return fitForDuty; }
public void setFitForDuty(Boolean v) { this.fitForDuty = v; }

public String getNationality() { return nationality; }
public void setNationality(String v) { this.nationality = v; }

public String getCompanyRegistrationNumber() { return companyRegistrationNumber; }
public void setCompanyRegistrationNumber(String v) { this.companyRegistrationNumber = v; }

public String getCountryOfIncorporation() { return countryOfIncorporation; }
public void setCountryOfIncorporation(String v) { this.countryOfIncorporation = v; }

public String getBusinessType() { return businessType; }
public void setBusinessType(String v) { this.businessType = v; }

public String getPositionTitle() { return positionTitle; }
public void setPositionTitle(String v) { this.positionTitle = v; }

public String getGovernmentIdDocument() { return governmentIdDocument; }
public void setGovernmentIdDocument(String v) { this.governmentIdDocument = v; }

public String getExportLicenceNumber() { return exportLicenceNumber; }
public void setExportLicenceNumber(String v) { this.exportLicenceNumber = v; }

public String getOperatingJurisdiction() { return operatingJurisdiction; }
public void setOperatingJurisdiction(String v) { this.operatingJurisdiction = v; }

public String getMineralsOfInterest() { return mineralsOfInterest; }
public void setMineralsOfInterest(String v) { this.mineralsOfInterest = v; }

public String getTypicalOrderVolume() { return typicalOrderVolume; }
public void setTypicalOrderVolume(String v) { this.typicalOrderVolume = v; }

public String getPreferredTransactionMethod() { return preferredTransactionMethod; }
public void setPreferredTransactionMethod(String v) { this.preferredTransactionMethod = v; }

public Boolean getNdaSigned() { return ndaSigned; }
public void setNdaSigned(Boolean v) { this.ndaSigned = v; }

public String getRegulatoryBodyName() { return regulatoryBodyName; }
public void setRegulatoryBodyName(String v) { this.regulatoryBodyName = v; }

public String getOfficialIdBadgeNumber() { return officialIdBadgeNumber; }
public void setOfficialIdBadgeNumber(String v) { this.officialIdBadgeNumber = v; }

public String getIssuingAuthority() { return issuingAuthority; }
public void setIssuingAuthority(String v) { this.issuingAuthority = v; }

public String getOfficialIdDocument() { return officialIdDocument; }
public void setOfficialIdDocument(String v) { this.officialIdDocument = v; }

public String getJurisdictionOfAuthority() { return jurisdictionOfAuthority; }
public void setJurisdictionOfAuthority(String v) { this.jurisdictionOfAuthority = v; }
}