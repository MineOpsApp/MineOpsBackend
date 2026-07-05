package MineOpsBackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_interest")
public class JobInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long jobPostingId;
    private String applicantEmail;
    private String applicantName;
    private String applicantRole;

    @Column(length = 500)
    private String message;

    private LocalDateTime createdAt;

    public JobInterest() {}

    public Long getId() { return id; }
    public Long getJobPostingId() { return jobPostingId; }
    public void setJobPostingId(Long v) { this.jobPostingId = v; }
    public String getApplicantEmail() { return applicantEmail; }
    public void setApplicantEmail(String v) { this.applicantEmail = v; }
    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String v) { this.applicantName = v; }
    public String getApplicantRole() { return applicantRole; }
    public void setApplicantRole(String v) { this.applicantRole = v; }
    public String getMessage() { return message; }
    public void setMessage(String v) { this.message = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
