package MineOpsBackend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "insurance_enrollment_history")
public class InsuranceEnrollmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String workerEmail;
    private String site;
    private String action; // ENROLLED | LAPSED
    private LocalDateTime createdAt;

    public InsuranceEnrollmentHistory() {}

    public InsuranceEnrollmentHistory(String workerEmail, String site, String action) {
        this.workerEmail = workerEmail;
        this.site = site;
        this.action = action;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getWorkerEmail() { return workerEmail; }
    public String getSite() { return site; }
    public String getAction() { return action; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
