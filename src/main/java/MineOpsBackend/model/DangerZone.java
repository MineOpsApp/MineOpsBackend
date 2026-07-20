package MineOpsBackend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "danger_zones")
public class DangerZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String site;

    private String zoneName;

    private String riskLevel;

    private String status;

    private LocalDateTime createdAt;

    @jakarta.persistence.Column(columnDefinition = "TEXT")
    private String polygonPoints;

    private Double latitude;
    private Double longitude;
    private Integer radiusMeters;

    public DangerZone() {
    }

    public DangerZone(String site, String zoneName, String riskLevel) {
        this.site = site;
        this.zoneName = zoneName;
        this.riskLevel = riskLevel;
        this.status = "Active";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getPolygonPoints() {
        return polygonPoints;
    }

    public void setPolygonPoints(String polygonPoints) {
        this.polygonPoints = polygonPoints;
    }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double v) { this.latitude = v; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double v) { this.longitude = v; }

    public Integer getRadiusMeters() { return radiusMeters; }
    public void setRadiusMeters(Integer v) { this.radiusMeters = v; }
}
