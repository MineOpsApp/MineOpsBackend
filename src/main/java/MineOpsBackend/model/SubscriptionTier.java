package MineOpsBackend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "subscription_tier")
public class SubscriptionTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyPriceGhs;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Boolean active = true;

    public SubscriptionTier() {}

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public BigDecimal getMonthlyPriceGhs() { return monthlyPriceGhs; }
    public void setMonthlyPriceGhs(BigDecimal v) { this.monthlyPriceGhs = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean v) { this.active = v; }
}
