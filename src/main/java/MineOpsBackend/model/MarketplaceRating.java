package MineOpsBackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "marketplace_rating")
public class MarketplaceRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long transactionId;
    private String raterEmail;
    private String raterRole;
    private int reliability;
    private int communication;
    private Integer productQuality;
    private Integer listingAccuracy;

    @Column(length = 500)
    private String comment;

    private LocalDateTime createdAt;

    public MarketplaceRating() {}

    public Long getId() { return id; }
    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long v) { this.transactionId = v; }
    public String getRaterEmail() { return raterEmail; }
    public void setRaterEmail(String v) { this.raterEmail = v; }
    public String getRaterRole() { return raterRole; }
    public void setRaterRole(String v) { this.raterRole = v; }
    public int getReliability() { return reliability; }
    public void setReliability(int v) { this.reliability = v; }
    public int getCommunication() { return communication; }
    public void setCommunication(int v) { this.communication = v; }
    public Integer getProductQuality() { return productQuality; }
    public void setProductQuality(Integer v) { this.productQuality = v; }
    public Integer getListingAccuracy() { return listingAccuracy; }
    public void setListingAccuracy(Integer v) { this.listingAccuracy = v; }
    public String getComment() { return comment; }
    public void setComment(String v) { this.comment = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
