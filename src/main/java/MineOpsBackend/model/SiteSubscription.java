package MineOpsBackend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "site_subscription")
public class SiteSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String site;

    private Long tierId;

    @Column(nullable = false)
    private String status = "TRIAL";

    private LocalDateTime trialEndsAt;
    private LocalDateTime currentPeriodEndsAt;
    private LocalDateTime createdAt;

    public SiteSubscription() {}

    public Long getId() { return id; }
    public String getSite() { return site; }
    public void setSite(String v) { this.site = v; }
    public Long getTierId() { return tierId; }
    public void setTierId(Long v) { this.tierId = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public LocalDateTime getTrialEndsAt() { return trialEndsAt; }
    public void setTrialEndsAt(LocalDateTime v) { this.trialEndsAt = v; }
    public LocalDateTime getCurrentPeriodEndsAt() { return currentPeriodEndsAt; }
    public void setCurrentPeriodEndsAt(LocalDateTime v) { this.currentPeriodEndsAt = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
