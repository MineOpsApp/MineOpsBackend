package MineOpsBackend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_payment")
public class SubscriptionPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String site;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amountGhs;

    @Column(length = 30)
    private String method;

    @Column(length = 255)
    private String reference;

    @Column(nullable = false)
    private String recordedByEmail;

    private LocalDate periodCoveredStart;
    private LocalDate periodCoveredEnd;
    private LocalDateTime createdAt;

    public SubscriptionPayment() {}

    public Long getId() { return id; }
    public String getSite() { return site; }
    public void setSite(String v) { this.site = v; }
    public BigDecimal getAmountGhs() { return amountGhs; }
    public void setAmountGhs(BigDecimal v) { this.amountGhs = v; }
    public String getMethod() { return method; }
    public void setMethod(String v) { this.method = v; }
    public String getReference() { return reference; }
    public void setReference(String v) { this.reference = v; }
    public String getRecordedByEmail() { return recordedByEmail; }
    public void setRecordedByEmail(String v) { this.recordedByEmail = v; }
    public LocalDate getPeriodCoveredStart() { return periodCoveredStart; }
    public void setPeriodCoveredStart(LocalDate v) { this.periodCoveredStart = v; }
    public LocalDate getPeriodCoveredEnd() { return periodCoveredEnd; }
    public void setPeriodCoveredEnd(LocalDate v) { this.periodCoveredEnd = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
