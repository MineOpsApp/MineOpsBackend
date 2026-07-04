package MineOpsBackend.controller;

import MineOpsBackend.model.MarketplaceTransaction;
import MineOpsBackend.repository.MarketplaceTransactionRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
public class MarketplaceTransactionController {

    private static final Set<String> VALID_BATCH_STATUSES =
        Set.of("PREPARING", "DISPATCHED", "IN_TRANSIT", "DELIVERED");

    private final MarketplaceTransactionRepository transactionRepo;
    private final AuditLogService auditLogService;

    public MarketplaceTransactionController(
        MarketplaceTransactionRepository transactionRepo,
        AuditLogService auditLogService
    ) {
        this.transactionRepo = transactionRepo;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/api/marketplace/transactions/mine")
    @PreAuthorize("hasAuthority('ROLE_BUYER')")
    public List<MarketplaceTransaction> getMyTransactions(@AuthenticationPrincipal AuthenticatedUser user) {
        return transactionRepo.findByBuyerEmailOrderByCreatedAtDesc(user.email());
    }

    @GetMapping("/api/marketplace/transactions/site")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public List<MarketplaceTransaction> getSiteTransactions(@AuthenticationPrincipal AuthenticatedUser user) {
        return transactionRepo.findBySiteIgnoreCaseOrderByCreatedAtDesc(user.assignedSite());
    }

    @PatchMapping("/api/marketplace/transactions/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public MarketplaceTransaction updateStatus(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id,
        @RequestBody Map<String, String> body
    ) {
        MarketplaceTransaction tx = transactionRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
        if (!tx.getSite().equalsIgnoreCase(user.assignedSite())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Transaction belongs to a different site");
        }
        String newStatus = body.get("batchStatus");
        if (!VALID_BATCH_STATUSES.contains(newStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid batch status");
        }
        tx.setBatchStatus(newStatus);
        tx.setUpdatedBy(user.email());
        tx.setUpdatedAt(LocalDateTime.now());
        MarketplaceTransaction saved = transactionRepo.save(tx);
        auditLogService.record("TRANSACTION_STATUS_UPDATED", user.role(), user.fullName(), user.email(),
            "MARKETPLACE_TRANSACTION", id, "status=" + newStatus);
        return saved;
    }
}
