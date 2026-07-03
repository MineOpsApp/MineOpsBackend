package MineOpsBackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "certification_history")
public class CertificationHistory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private Long certificationId;
    private LocalDate previousExpiry;
    @Column(nullable = false) private LocalDate newExpiry;
    private String previousAuthority;
    private String newAuthority;
    @Column(nullable = false) private String renewedBy;
    @Column(nullable = false) private LocalDateTime renewedAt;
    private String notes;

    public CertificationHistory() {}

    public CertificationHistory(
        Long certificationId,
        LocalDate previousExpiry, LocalDate newExpiry,
        String previousAuthority, String newAuthority,
        String renewedBy, String notes
    ) {
        this.certificationId = certificationId;
        this.previousExpiry = previousExpiry;
        this.newExpiry = newExpiry;
        this.previousAuthority = previousAuthority;
        this.newAuthority = newAuthority;
        this.renewedBy = renewedBy;
        this.renewedAt = LocalDateTime.now();
        this.notes = notes;
    }

    public Long getId() { return id; }
    public Long getCertificationId() { return certificationId; }
    public LocalDate getPreviousExpiry() { return previousExpiry; }
    public LocalDate getNewExpiry() { return newExpiry; }
    public String getPreviousAuthority() { return previousAuthority; }
    public String getNewAuthority() { return newAuthority; }
    public String getRenewedBy() { return renewedBy; }
    public LocalDateTime getRenewedAt() { return renewedAt; }
    public String getNotes() { return notes; }
}
