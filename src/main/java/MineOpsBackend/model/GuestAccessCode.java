package MineOpsBackend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "guest_access_code")
public class GuestAccessCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String site;
    private String guestSubRole;
    private String code;
    private Integer sessionHours;
    private Integer maxRedemptions;
    private Integer redemptionCount;
    private Boolean active;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public GuestAccessCode() {}

    public GuestAccessCode(String site, String guestSubRole, String code,
                            int sessionHours, int maxRedemptions,
                            String createdBy, LocalDateTime expiresAt) {
        this.site = site;
        this.guestSubRole = guestSubRole;
        this.code = code;
        this.sessionHours = sessionHours;
        this.maxRedemptions = maxRedemptions;
        this.redemptionCount = 0;
        this.active = true;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public String getSite() { return site; }
    public void setSite(String v) { this.site = v; }
    public String getGuestSubRole() { return guestSubRole; }
    public void setGuestSubRole(String v) { this.guestSubRole = v; }
    public String getCode() { return code; }
    public void setCode(String v) { this.code = v; }
    public Integer getSessionHours() { return sessionHours; }
    public void setSessionHours(Integer v) { this.sessionHours = v; }
    public Integer getMaxRedemptions() { return maxRedemptions; }
    public void setMaxRedemptions(Integer v) { this.maxRedemptions = v; }
    public Integer getRedemptionCount() { return redemptionCount; }
    public void setRedemptionCount(Integer v) { this.redemptionCount = v; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean v) { this.active = v; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String v) { this.createdBy = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime v) { this.expiresAt = v; }
}
