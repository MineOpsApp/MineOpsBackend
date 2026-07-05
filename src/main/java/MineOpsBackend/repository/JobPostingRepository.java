package MineOpsBackend.repository;

import MineOpsBackend.model.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {
    List<JobPosting> findByStatusOrderByCreatedAtDesc(String status);
    List<JobPosting> findBySiteAndStatusOrderByCreatedAtDesc(String site, String status);
}
