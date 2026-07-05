package MineOpsBackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "sites")
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String status;

    private boolean inventoryVisibleToGuests = false;

    private boolean insuranceEnabled = false;
    private String insuranceProviderName;

    @Column(precision = 10, scale = 2)
    private BigDecimal insurancePremium;

    private String insuranceDeductionMode = "DEDUCT_FROM_PAY";

    @Column(length = 500)
    private String mineralsProduced;
    private String productionCapacity;
    private Integer establishedYear;
    @Column(length = 1000)
    private String profileDescription;
    private String contactEmail;

    public Site() {
    }

    public Site(String name, String status) {
        this.name = name;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isInventoryVisibleToGuests() {
        return inventoryVisibleToGuests;
    }

    public void setInventoryVisibleToGuests(boolean inventoryVisibleToGuests) {
        this.inventoryVisibleToGuests = inventoryVisibleToGuests;
    }

    public boolean isInsuranceEnabled() { return insuranceEnabled; }
    public void setInsuranceEnabled(boolean insuranceEnabled) { this.insuranceEnabled = insuranceEnabled; }

    public String getInsuranceProviderName() { return insuranceProviderName; }
    public void setInsuranceProviderName(String insuranceProviderName) { this.insuranceProviderName = insuranceProviderName; }

    public BigDecimal getInsurancePremium() { return insurancePremium; }
    public void setInsurancePremium(BigDecimal insurancePremium) { this.insurancePremium = insurancePremium; }

    public String getInsuranceDeductionMode() { return insuranceDeductionMode; }
    public void setInsuranceDeductionMode(String insuranceDeductionMode) { this.insuranceDeductionMode = insuranceDeductionMode; }

    public String getMineralsProduced() { return mineralsProduced; }
    public void setMineralsProduced(String v) { this.mineralsProduced = v; }

    public String getProductionCapacity() { return productionCapacity; }
    public void setProductionCapacity(String v) { this.productionCapacity = v; }

    public Integer getEstablishedYear() { return establishedYear; }
    public void setEstablishedYear(Integer v) { this.establishedYear = v; }

    public String getProfileDescription() { return profileDescription; }
    public void setProfileDescription(String v) { this.profileDescription = v; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String v) { this.contactEmail = v; }
}
