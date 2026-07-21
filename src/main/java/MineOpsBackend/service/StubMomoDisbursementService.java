package MineOpsBackend.service;

import MineOpsBackend.model.WorkerPayRecord;
import MineOpsBackend.repository.WorkerPayRecordRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class StubMomoDisbursementService implements MomoDisbursementService {

    private final WorkerPayRecordRepository recordRepo;
    private final EmailService emailService;

    public StubMomoDisbursementService(WorkerPayRecordRepository recordRepo, EmailService emailService) {
        this.recordRepo = recordRepo;
        this.emailService = emailService;
    }

    @Override
    public void disburse(WorkerPayRecord record) {
        if (record.getMomoNumber() == null || record.getMomoNumber().isBlank()) {
            throw new IllegalStateException(
                "Cannot disburse: no MoMo number on file for " + record.getWorkerEmail());
        }

        System.out.printf("[STUB MoMo] Disbursing GHS %.2f to %s (%s) on %s%n",
            record.getNetPay(), record.getWorkerName(),
            record.getMomoNumber(), record.getMomoNetwork());

        record.setDisbursementStatus("SENT");
        record.setMomoTransactionRef("SIMULATED-" + UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 12));
        record.setDisbursedAt(LocalDateTime.now());
        recordRepo.save(record);

        emailService.sendPayDisbursed(record.getWorkerEmail(), record.getWorkerName(), record.getNetPay(), record.getMomoNumber());
    }
}
