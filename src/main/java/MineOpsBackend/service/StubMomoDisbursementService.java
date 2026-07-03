package MineOpsBackend.service;

import MineOpsBackend.model.WorkerPayRecord;
import MineOpsBackend.repository.WorkerPayRecordRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class StubMomoDisbursementService implements MomoDisbursementService {

    private final WorkerPayRecordRepository recordRepo;

    public StubMomoDisbursementService(WorkerPayRecordRepository recordRepo) {
        this.recordRepo = recordRepo;
    }

    @Override
    public void disburse(WorkerPayRecord record) {
        System.out.printf("[STUB MoMo] Disbursing GHS %.2f to %s (%s) on %s%n",
            record.getNetPay(), record.getWorkerName(),
            record.getMomoNumber() != null ? record.getMomoNumber() : "no number on file",
            record.getMomoNetwork() != null ? record.getMomoNetwork() : "unknown network");

        record.setDisbursementStatus("SENT");
        record.setMomoTransactionRef("SIMULATED-" + UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 12));
        record.setDisbursedAt(LocalDateTime.now());
        recordRepo.save(record);
    }
}
