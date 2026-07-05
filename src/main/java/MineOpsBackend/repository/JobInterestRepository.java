package MineOpsBackend.repository;

import MineOpsBackend.model.JobInterest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobInterestRepository extends JpaRepository<JobInterest, Long> {
    List<JobInterest> findByJobPostingIdOrderByCreatedAtDesc(Long jobPostingId);
    boolean existsByJobPostingIdAndApplicantEmail(Long jobPostingId, String applicantEmail);
}
