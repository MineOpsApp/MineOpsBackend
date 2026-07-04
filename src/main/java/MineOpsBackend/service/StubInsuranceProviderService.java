package MineOpsBackend.service;

import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.InsuranceEnrollmentHistory;
import MineOpsBackend.repository.InsuranceEnrollmentHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class StubInsuranceProviderService implements InsuranceProviderService {

    private final InsuranceEnrollmentHistoryRepository historyRepo;

    public StubInsuranceProviderService(InsuranceEnrollmentHistoryRepository historyRepo) {
        this.historyRepo = historyRepo;
    }

    @Override
    public void enroll(AppUser worker) {
        worker.setInsuranceStatus("INSURED");
        worker.setInsuranceEnrolledAt(LocalDateTime.now());
        historyRepo.save(new InsuranceEnrollmentHistory(
            worker.getEmail(), worker.getAssignedSite(), "ENROLLED"
        ));
        System.out.printf("[STUB Insurance] Enrolled %s (%s) on site %s%n",
            worker.getFullName(), worker.getEmail(), worker.getAssignedSite());
    }

    @Override
    public boolean checkStatus(AppUser worker) {
        return true;
    }
}
