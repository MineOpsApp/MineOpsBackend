package MineOpsBackend.service;

import MineOpsBackend.model.WorkerPayRecord;

public interface MomoDisbursementService {
    void disburse(WorkerPayRecord record);
}
