package MineOpsBackend.controller;

import MineOpsBackend.model.MarketplaceTransaction;
import MineOpsBackend.model.TransactionDispute;
import MineOpsBackend.repository.MarketplaceTransactionRepository;
import MineOpsBackend.repository.TransactionDisputeRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/community")
public class TransactionDisputeController {

    private static final String COMMUNITY_ROLES =
            "hasAnyAuthority('ROLE_WORKER', 'ROLE_SUPERVISOR', 'ROLE_SAFETY_OFFICER', 'ROLE_BUYER')";

    private final MarketplaceTransactionRepository txRepo;
    private final TransactionDisputeRepository disputeRepo;
    private final AuditLogService auditLogService;

    public TransactionDisputeController(MarketplaceTransactionRepository txRepo,
                                        TransactionDisputeRepository disputeRepo,
                                        AuditLogService auditLogService) {
        this.txRepo = txRepo;
        this.disputeRepo = disputeRepo;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/transactions/{id}/dispute")
    @PreAuthorize(COMMUNITY_ROLES)
    public TransactionDispute raiseDispute(@PathVariable Long id,
                                           @AuthenticationPrincipal AuthenticatedUser user,
                                           @RequestBody Map<String, String> body) {
        MarketplaceTransaction tx = txRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        boolean isBuyer = user.email().equalsIgnoreCase(tx.getBuyerEmail());
        boolean isSite = user.assignedSite() != null && user.assignedSite().equalsIgnoreCase(tx.getSite());
        if (!isBuyer && !isSite) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a party to this transaction");
        }
        if (disputeRepo.existsByTransactionId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Dispute already raised for this transaction");
        }

        String reason = body.get("reason");
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reason is required");
        }

        TransactionDispute dispute = new TransactionDispute();
        dispute.setTransactionId(id);
        dispute.setRaisedByEmail(user.email());
        dispute.setRaisedByRole(user.role());
        dispute.setReason(reason.trim());
        dispute.setStatus("OPEN");
        dispute.setCreatedAt(LocalDateTime.now());
        TransactionDispute saved = disputeRepo.save(dispute);
        auditLogService.record("TRANSACTION_DISPUTE_RAISED", user.role(), user.fullName(), user.email(),
            "TransactionDispute", saved.getId(), "Transaction #" + id + " — " + reason.trim());
        return saved;
    }

    @GetMapping("/transactions/{id}/dispute")
    @PreAuthorize(COMMUNITY_ROLES)
    public TransactionDispute getDispute(@PathVariable Long id,
                                         @AuthenticationPrincipal AuthenticatedUser user) {
        MarketplaceTransaction tx = txRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        boolean isBuyer = user.email().equalsIgnoreCase(tx.getBuyerEmail());
        boolean isSite = user.assignedSite() != null && user.assignedSite().equalsIgnoreCase(tx.getSite());
        if (!isBuyer && !isSite) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a party to this transaction");
        }

        return disputeRepo.findByTransactionId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No dispute found"));
    }

    @PatchMapping("/disputes/{disputeId}/resolve")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR', 'ROLE_SAFETY_OFFICER')")
    public TransactionDispute resolveDispute(@PathVariable Long disputeId,
                                              @AuthenticationPrincipal AuthenticatedUser user,
                                              @RequestBody Map<String, String> body) {
        TransactionDispute dispute = disputeRepo.findById(disputeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dispute not found"));

        MarketplaceTransaction tx = txRepo.findById(dispute.getTransactionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
        if (user.assignedSite() == null || !user.assignedSite().equalsIgnoreCase(tx.getSite())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Can only resolve disputes for your site's transactions");
        }

        if (!"OPEN".equals(dispute.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Dispute is already resolved");
        }

        String notes = body.get("resolutionNotes");
        if (notes == null || notes.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resolutionNotes is required");
        }

        dispute.setStatus("RESOLVED");
        dispute.setResolutionNotes(notes.trim());
        dispute.setResolvedAt(LocalDateTime.now());
        TransactionDispute saved = disputeRepo.save(dispute);
        auditLogService.record("TRANSACTION_DISPUTE_RESOLVED", user.role(), user.fullName(), user.email(),
            "TransactionDispute", saved.getId(), notes.trim());
        return saved;
    }
}
