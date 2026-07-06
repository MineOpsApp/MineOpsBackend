package MineOpsBackend.controller;

import MineOpsBackend.model.BulkPurchaseRequest;
import MineOpsBackend.model.IllegalMineReport;
import MineOpsBackend.model.MineralInventory;
import MineOpsBackend.model.MiningPermitStatus;
import MineOpsBackend.repository.BulkPurchaseRequestRepository;
import MineOpsBackend.repository.IllegalMineReportRepository;
import MineOpsBackend.repository.MineralInventoryRepository;
import MineOpsBackend.repository.MiningPermitStatusRepository;
import MineOpsBackend.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/government")
@PreAuthorize("hasAuthority('ROLE_GOVERNMENT')")
public class GovernmentDashboardController {

    private final MineralInventoryRepository inventoryRepository;
    private final BulkPurchaseRequestRepository bulkPurchaseRepo;
    private final IllegalMineReportRepository illegalMineReportRepo;
    private final MiningPermitStatusRepository permitRepo;

    public GovernmentDashboardController(
        MineralInventoryRepository inventoryRepository,
        BulkPurchaseRequestRepository bulkPurchaseRepo,
        IllegalMineReportRepository illegalMineReportRepo,
        MiningPermitStatusRepository permitRepo
    ) {
        this.inventoryRepository = inventoryRepository;
        this.bulkPurchaseRepo = bulkPurchaseRepo;
        this.illegalMineReportRepo = illegalMineReportRepo;
        this.permitRepo = permitRepo;
    }

    @GetMapping("/inventory")
    public List<MineralInventory> getAllInventory() {
        return inventoryRepository.findAll();
    }

    @GetMapping("/bulk-purchase-requests")
    public List<BulkPurchaseRequest> getBulkPurchaseRequests() {
        return bulkPurchaseRepo.findAll();
    }

    @PatchMapping("/bulk-purchase-requests/{id}/fulfill")
    public BulkPurchaseRequest fulfillRequest(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id
    ) {
        BulkPurchaseRequest req = bulkPurchaseRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        if (!"AVAILABLE".equals(req.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request is not in AVAILABLE status");
        }
        req.setStatus("FULFILLED");
        return bulkPurchaseRepo.save(req);
    }

    @GetMapping("/illegal-mine-reports")
    public List<IllegalMineReport> getIllegalMineReports() {
        return illegalMineReportRepo.findAll();
    }

    @PatchMapping("/illegal-mine-reports/{id}/review")
    public IllegalMineReport reviewReport(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id,
        @RequestBody Map<String, String> body
    ) {
        IllegalMineReport report = illegalMineReportRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        String status = body.get("status");
        if (status != null && !status.isBlank()) report.setStatus(status);
        String notes = body.get("reviewNotes");
        if (notes != null) report.setReviewNotes(notes);
        report.setReviewedByEmail(user.email());
        return illegalMineReportRepo.save(report);
    }

    @GetMapping("/permits")
    public List<MiningPermitStatus> getAllPermits() {
        return permitRepo.findAll();
    }
}
