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
import MineOpsBackend.service.AuditLogService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/government")
@PreAuthorize("hasAuthority('ROLE_GOVERNMENT')")
public class GovernmentDashboardController {

    private final MineralInventoryRepository inventoryRepository;
    private final BulkPurchaseRequestRepository bulkPurchaseRepo;
    private final IllegalMineReportRepository illegalMineReportRepo;
    private final MiningPermitStatusRepository permitRepo;
    private final AuditLogService auditLogService;

    @PersistenceContext
    private EntityManager entityManager;

    public GovernmentDashboardController(
        MineralInventoryRepository inventoryRepository,
        BulkPurchaseRequestRepository bulkPurchaseRepo,
        IllegalMineReportRepository illegalMineReportRepo,
        MiningPermitStatusRepository permitRepo,
        AuditLogService auditLogService
    ) {
        this.inventoryRepository = inventoryRepository;
        this.bulkPurchaseRepo = bulkPurchaseRepo;
        this.illegalMineReportRepo = illegalMineReportRepo;
        this.permitRepo = permitRepo;
        this.auditLogService = auditLogService;
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
        BulkPurchaseRequest saved = bulkPurchaseRepo.save(req);
        auditLogService.record("BULK_PURCHASE_REQUEST_FULFILLED", user.role(), user.fullName(), user.email(),
            "BulkPurchaseRequest", saved.getId(), saved.getMineralType() + " @ " + saved.getSite());
        return saved;
    }

    @GetMapping("/illegal-mine-reports")
    public List<IllegalMineReport> getIllegalMineReports() {
        List<IllegalMineReport> reports = illegalMineReportRepo.findAll();
        // Photo is a large base64 payload — don't load every report's photo into memory on a list call.
        reports.forEach(r -> { entityManager.detach(r); r.stripPhotoDataForList(); });
        return reports;
    }

    // Fetch a single report's photo on demand — never returned in bulk from the list endpoint above.
    @GetMapping("/illegal-mine-reports/{id}/photo")
    public Map<String, String> getIllegalMineReportPhoto(@PathVariable Long id) {
        IllegalMineReport report = illegalMineReportRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        return Map.of("photoData", report.getPhotoData() != null ? report.getPhotoData() : "");
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
        if (status != null && !status.isBlank()) {
            if (!Set.of("UNDER_REVIEW", "ACTIONED", "DISMISSED").contains(status)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid status. Must be UNDER_REVIEW, ACTIONED, or DISMISSED");
            }
            report.setStatus(status);
        }
        String notes = body.get("reviewNotes");
        if (notes != null) report.setReviewNotes(notes);
        report.setReviewedByEmail(user.email());
        IllegalMineReport saved = illegalMineReportRepo.save(report);
        auditLogService.record("ILLEGAL_MINE_REPORT_REVIEWED", user.role(), user.fullName(), user.email(),
            "IllegalMineReport", saved.getId(), saved.getStatus() + " — " + saved.getLocationDescription());
        return saved;
    }

    @GetMapping("/permits")
    public List<MiningPermitStatus> getAllPermits() {
        return permitRepo.findAll();
    }
}
