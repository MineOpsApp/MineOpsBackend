package MineOpsBackend.service;

import MineOpsBackend.model.InventoryTransaction;
import MineOpsBackend.model.MineralInventory;
import MineOpsBackend.model.ShiftLog;
import MineOpsBackend.repository.InventoryTransactionRepository;
import MineOpsBackend.repository.MineralInventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MineralInventoryService {

    private final MineralInventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;

    public MineralInventoryService(
        MineralInventoryRepository inventoryRepository,
        InventoryTransactionRepository transactionRepository
    ) {
        this.inventoryRepository = inventoryRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public void applyApprovedShiftLog(ShiftLog log, String approvedBy) {
        String site = log.getSite() != null ? log.getSite() : "Unassigned";

        MineralInventory inv = inventoryRepository
            .findBySiteIgnoreCaseAndMineralTypeIgnoreCaseAndUnitIgnoreCase(
                site, log.getMineralType(), log.getUnit())
            .orElse(new MineralInventory(site, log.getMineralType(), log.getUnit()));

        inv.setTotalVolume(inv.getTotalVolume().add(log.getVolumeExtracted()));
        inv.setLastUpdatedAt(LocalDateTime.now());
        inv.setLastShiftLogId(log.getId());
        inv.setLastWorkerName(log.getWorkerName());
        inv.setLastZone(log.getZone());
        inventoryRepository.save(inv);

        transactionRepository.save(new InventoryTransaction(
            site,
            log.getMineralType(),
            log.getUnit(),
            log.getVolumeExtracted(),
            log.getId(),
            log.getWorkerName(),
            log.getWorkerEmail(),
            log.getZone(),
            approvedBy
        ));
    }
}
