package MineOpsBackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "first_aid_kits", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"site", "zone"})
})
public class FirstAidKit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String site;
    @Column(nullable = false) private String zone;
    @Column(nullable = false) private String location;

    @Column(nullable = false) private boolean hasBandages;
    @Column(nullable = false) private boolean hasGloves;
    @Column(nullable = false) private boolean hasAntiseptic;
    @Column(nullable = false) private boolean hasOxygen;
    @Column(nullable = false) private boolean hasStretcher;

    private String notes;
    private String lastCheckedBy;
    private LocalDateTime lastCheckedAt;

    @Column(nullable = false) private LocalDateTime createdAt;

    public FirstAidKit() {}

    public FirstAidKit(String site, String zone, String location) {
        this.site = site;
        this.zone = zone;
        this.location = location;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getSite() { return site; }
    public String getZone() { return zone; }
    public String getLocation() { return location; }
    public void setLocation(String v) { this.location = v; }
    public boolean isHasBandages() { return hasBandages; }
    public void setHasBandages(boolean v) { this.hasBandages = v; }
    public boolean isHasGloves() { return hasGloves; }
    public void setHasGloves(boolean v) { this.hasGloves = v; }
    public boolean isHasAntiseptic() { return hasAntiseptic; }
    public void setHasAntiseptic(boolean v) { this.hasAntiseptic = v; }
    public boolean isHasOxygen() { return hasOxygen; }
    public void setHasOxygen(boolean v) { this.hasOxygen = v; }
    public boolean isHasStretcher() { return hasStretcher; }
    public void setHasStretcher(boolean v) { this.hasStretcher = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
    public String getLastCheckedBy() { return lastCheckedBy; }
    public void setLastCheckedBy(String v) { this.lastCheckedBy = v; }
    public LocalDateTime getLastCheckedAt() { return lastCheckedAt; }
    public void setLastCheckedAt(LocalDateTime v) { this.lastCheckedAt = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public boolean isFullyStocked() {
        return hasBandages && hasGloves && hasAntiseptic && hasOxygen && hasStretcher;
    }
}
