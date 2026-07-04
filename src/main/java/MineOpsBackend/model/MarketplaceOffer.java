package MineOpsBackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "marketplace_offer")
public class MarketplaceOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long listingId;
    private Long parentOfferId;
    private String buyerEmail;
    private String buyerName;

    @Column(precision = 14, scale = 2)
    private BigDecimal offerPrice;

    @Column(precision = 14, scale = 3)
    private BigDecimal offerQuantity;

    @Column(length = 500)
    private String message;

    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
    private String respondedBy;

    public MarketplaceOffer() {}

    public Long getId() { return id; }
    public Long getListingId() { return listingId; }
    public void setListingId(Long v) { this.listingId = v; }
    public Long getParentOfferId() { return parentOfferId; }
    public void setParentOfferId(Long v) { this.parentOfferId = v; }
    public String getBuyerEmail() { return buyerEmail; }
    public void setBuyerEmail(String v) { this.buyerEmail = v; }
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String v) { this.buyerName = v; }
    public BigDecimal getOfferPrice() { return offerPrice; }
    public void setOfferPrice(BigDecimal v) { this.offerPrice = v; }
    public BigDecimal getOfferQuantity() { return offerQuantity; }
    public void setOfferQuantity(BigDecimal v) { this.offerQuantity = v; }
    public String getMessage() { return message; }
    public void setMessage(String v) { this.message = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime v) { this.respondedAt = v; }
    public String getRespondedBy() { return respondedBy; }
    public void setRespondedBy(String v) { this.respondedBy = v; }
}
