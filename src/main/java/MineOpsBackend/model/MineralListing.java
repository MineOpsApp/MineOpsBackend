package MineOpsBackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "mineral_listing")
public class MineralListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String site;
    private String mineralType;

    @Column(precision = 14, scale = 3)
    private BigDecimal quantity;

    private String unit;
    private String grade;

    @Column(precision = 14, scale = 2)
    private BigDecimal askingPrice;

    private String location;
    private LocalDate availableFrom;

    @Column(precision = 14, scale = 3)
    private BigDecimal minOrderQuantity;

    @Column(columnDefinition = "TEXT")
    private String photoData;

    private String status;
    private String createdBy;
    private LocalDateTime createdAt;

    public MineralListing() {}

    public Long getId() { return id; }
    public String getSite() { return site; }
    public void setSite(String v) { this.site = v; }
    public String getMineralType() { return mineralType; }
    public void setMineralType(String v) { this.mineralType = v; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }
    public String getUnit() { return unit; }
    public void setUnit(String v) { this.unit = v; }
    public String getGrade() { return grade; }
    public void setGrade(String v) { this.grade = v; }
    public BigDecimal getAskingPrice() { return askingPrice; }
    public void setAskingPrice(BigDecimal v) { this.askingPrice = v; }
    public String getLocation() { return location; }
    public void setLocation(String v) { this.location = v; }
    public LocalDate getAvailableFrom() { return availableFrom; }
    public void setAvailableFrom(LocalDate v) { this.availableFrom = v; }
    public BigDecimal getMinOrderQuantity() { return minOrderQuantity; }
    public void setMinOrderQuantity(BigDecimal v) { this.minOrderQuantity = v; }
    public String getPhotoData() { return photoData; }
    public void setPhotoData(String v) { this.photoData = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String v) { this.createdBy = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
