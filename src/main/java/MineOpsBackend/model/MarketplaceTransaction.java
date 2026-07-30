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
@Table(name = "marketplace_transaction")
public class MarketplaceTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long listingId;
    private Long offerId;
    private String site;
    private String buyerEmail;
    private String buyerName;
    private String mineralType;

    @Column(precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(precision = 14, scale = 2)
    private BigDecimal agreedPrice;

    private String batchStatus;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;

    // Payment tracking (Paystack) — deliberately independent of batchStatus above. A transaction
    // can be DISPATCHED/DELIVERED while still UNPAID or vice versa; this app doesn't gate logistics
    // on payment today. UNPAID/PENDING/PAID/FAILED. Defaults to UNPAID so every existing row (and
    // every new one created before a buyer starts checkout) has a well-defined value, never null.
    @Column(length = 20)
    private String paymentStatus = "UNPAID";

    // The reference we generate and hand to Paystack on each initialize call — how the webhook
    // and the manual status-check endpoint find their way back to this row. Regenerated on every
    // retry after a FAILED attempt, so this is nullable (no payment attempted yet) but not unique
    // at the DB level once retries are considered — see migration comment.
    @Column(length = 100)
    private String paystackReference;

    @Column(precision = 14, scale = 2)
    private BigDecimal paidAmount;

    private LocalDateTime paidAt;

    // e.g. "mobile_money", "card" — from Paystack's transaction response, informational only.
    @Column(length = 30)
    private String paymentChannel;

    public MarketplaceTransaction() {}

    public Long getId() { return id; }
    public Long getListingId() { return listingId; }
    public void setListingId(Long v) { this.listingId = v; }
    public Long getOfferId() { return offerId; }
    public void setOfferId(Long v) { this.offerId = v; }
    public String getSite() { return site; }
    public void setSite(String v) { this.site = v; }
    public String getBuyerEmail() { return buyerEmail; }
    public void setBuyerEmail(String v) { this.buyerEmail = v; }
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String v) { this.buyerName = v; }
    public String getMineralType() { return mineralType; }
    public void setMineralType(String v) { this.mineralType = v; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }
    public BigDecimal getAgreedPrice() { return agreedPrice; }
    public void setAgreedPrice(BigDecimal v) { this.agreedPrice = v; }
    public String getBatchStatus() { return batchStatus; }
    public void setBatchStatus(String v) { this.batchStatus = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String v) { this.updatedBy = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String v) { this.paymentStatus = v; }
    public String getPaystackReference() { return paystackReference; }
    public void setPaystackReference(String v) { this.paystackReference = v; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal v) { this.paidAmount = v; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime v) { this.paidAt = v; }
    public String getPaymentChannel() { return paymentChannel; }
    public void setPaymentChannel(String v) { this.paymentChannel = v; }
}
