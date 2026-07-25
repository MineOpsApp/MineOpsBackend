package MineOpsBackend.controller;

import MineOpsBackend.model.MarketplaceTransaction;
import MineOpsBackend.repository.MarketplaceTransactionRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.service.NotificationService;
import MineOpsBackend.service.PushNotificationService;
import MineOpsBackend.util.CsvExportUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final NotificationService notificationService;
    private final PushNotificationService pushNotificationService;
    private final AppUserRepository userRepo;

    public MarketplaceTransactionController(
        MarketplaceTransactionRepository transactionRepo,
        AuditLogService auditLogService,
        NotificationService notificationService,
        PushNotificationService pushNotificationService,
        AppUserRepository userRepo
    ) {
        this.transactionRepo = transactionRepo;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.pushNotificationService = pushNotificationService;
        this.userRepo = userRepo;
    }

    private static final Map<String, String> STATUS_LABELS = Map.of(
        "PREPARING", "is being prepared",
        "DISPATCHED", "has been dispatched",
        "IN_TRANSIT", "is now in transit",
        "DELIVERED", "has been delivered"
    );

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
        if (!user.assignedSite().equalsIgnoreCase(tx.getSite())) {
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
        String statusPhrase = STATUS_LABELS.getOrDefault(newStatus, "was updated to " + newStatus);
        String notifTitle = "Order Update";
        String notifBody = "Your order of " + tx.getMineralType() + " " + statusPhrase + ".";
        notificationService.notify(tx.getBuyerEmail(), "TRANSACTION", notifTitle, notifBody, "MarketplaceTransaction", saved.getId());
        userRepo.findByEmailIgnoreCase(tx.getBuyerEmail()).ifPresent(u -> {
            String token = u.getPushToken();
            if (token != null && !token.isBlank()) {
                pushNotificationService.sendToToken(token, notifTitle, notifBody, "default");
            }
        });
        auditLogService.record("TRANSACTION_STATUS_UPDATED", user.role(), user.fullName(), user.email(),
            "MARKETPLACE_TRANSACTION", id, "status=" + newStatus);
        return saved;
    }

    @GetMapping("/api/marketplace/transactions/export/csv")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public ResponseEntity<String> exportCsv(@AuthenticationPrincipal AuthenticatedUser user) {
        List<MarketplaceTransaction> rows =
                transactionRepo.findBySiteIgnoreCaseOrderByCreatedAtDesc(user.assignedSite());
        StringBuilder csv = new StringBuilder();
        csv.append(CsvExportUtil.row("buyer", "mineralType", "quantity", "agreedPrice",
                "batchStatus", "createdAt"));
        for (MarketplaceTransaction t : rows) {
            csv.append(CsvExportUtil.row(
                    t.getBuyerName(), t.getMineralType(), t.getQuantity(),
                    t.getAgreedPrice(), t.getBatchStatus(), t.getCreatedAt()));
        }
        return CsvExportUtil.response("marketplace-transactions.csv", csv.toString());
    }
}
