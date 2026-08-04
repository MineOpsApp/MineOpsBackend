package MineOpsBackend.controller;

import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.MarketplaceTransaction;
import MineOpsBackend.repository.MarketplaceTransactionRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.service.NotificationService;
import MineOpsBackend.service.PaystackService;
import MineOpsBackend.service.PushNotificationService;
import MineOpsBackend.util.CsvExportUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class MarketplaceTransactionController {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceTransactionController.class);

    private static final Set<String> VALID_BATCH_STATUSES =
        Set.of("PREPARING", "DISPATCHED", "IN_TRANSIT", "DELIVERED");

    private final MarketplaceTransactionRepository transactionRepo;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final PushNotificationService pushNotificationService;
    private final AppUserRepository userRepo;
    private final PaystackService paystackService;
    private final com.fasterxml.jackson.databind.ObjectMapper webhookObjectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    public MarketplaceTransactionController(
        MarketplaceTransactionRepository transactionRepo,
        AuditLogService auditLogService,
        NotificationService notificationService,
        PushNotificationService pushNotificationService,
        AppUserRepository userRepo,
        PaystackService paystackService
    ) {
        this.transactionRepo = transactionRepo;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.pushNotificationService = pushNotificationService;
        this.userRepo = userRepo;
        this.paystackService = paystackService;
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

    // Replaces the old "rate to close" flow. Once a batch is DELIVERED, buyer and supervisor
    // each confirm independently; once both have, the transaction is flagged closed and drops
    // out of the pending/ongoing views on both sides.
    @PostMapping("/api/marketplace/transactions/{id}/confirm")
    @PreAuthorize("hasAnyAuthority('ROLE_BUYER','ROLE_SUPERVISOR')")
    public MarketplaceTransaction confirmTransaction(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id
    ) {
        MarketplaceTransaction tx = transactionRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        boolean isBuyer = "buyer".equals(user.role()) && tx.getBuyerEmail().equalsIgnoreCase(user.email());
        boolean isSupervisor = "supervisor".equals(user.role())
            && user.assignedSite() != null && user.assignedSite().equalsIgnoreCase(tx.getSite());
        if (!isBuyer && !isSupervisor) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a party to this transaction");
        }
        if (!"DELIVERED".equals(tx.getBatchStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Can only confirm delivered transactions");
        }
        if (Boolean.TRUE.equals(tx.getClosed())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This transaction is already closed");
        }

        LocalDateTime now = LocalDateTime.now();
        if (isBuyer) {
            if (tx.getBuyerConfirmedAt() != null)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "You've already confirmed this transaction");
            tx.setBuyerConfirmedAt(now);
        } else {
            if (tx.getSupervisorConfirmedAt() != null)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "You've already confirmed this transaction");
            tx.setSupervisorConfirmedAt(now);
        }

        boolean nowClosed = tx.getBuyerConfirmedAt() != null && tx.getSupervisorConfirmedAt() != null;
        if (nowClosed) tx.setClosed(true);

        MarketplaceTransaction saved = transactionRepo.save(tx);

        auditLogService.record("TRANSACTION_CONFIRMED", user.role(), user.fullName(), user.email(),
            "MARKETPLACE_TRANSACTION", id, isBuyer ? "buyer confirmed" : "supervisor confirmed");

        if (nowClosed) {
            auditLogService.record("TRANSACTION_CLOSED", user.role(), user.fullName(), user.email(),
                "MARKETPLACE_TRANSACTION", id, "both parties confirmed");
            String title = "Transaction Complete";
            String body = saved.getMineralType() + " (Txn #" + saved.getId() + ") is now complete — both parties confirmed delivery.";
            notificationService.notify(saved.getBuyerEmail(), "TRANSACTION", title, body, "MarketplaceTransaction", saved.getId());
            userRepo.findByEmailIgnoreCase(saved.getBuyerEmail()).ifPresent(u -> pushToken(u, title, body));
            for (AppUser recipient : userRepo.findByRoleInAndAssignedSiteIgnoreCase(List.of("supervisor"), saved.getSite())) {
                notificationService.notify(recipient.getEmail(), "TRANSACTION", title, body, "MarketplaceTransaction", saved.getId());
                pushToken(recipient, title, body);
            }
        } else {
            // Only one side has confirmed so far — nudge the other party.
            String title = "Confirmation Needed";
            String confirmerLabel = isBuyer ? "The buyer" : "Your supervisor";
            String body = confirmerLabel + " confirmed delivery of " + saved.getMineralType() + " (Txn #" + saved.getId() + "). Confirm on your side to close it out.";
            if (isBuyer) {
                for (AppUser recipient : userRepo.findByRoleInAndAssignedSiteIgnoreCase(List.of("supervisor"), saved.getSite())) {
                    notificationService.notify(recipient.getEmail(), "TRANSACTION", title, body, "MarketplaceTransaction", saved.getId());
                    pushToken(recipient, title, body);
                }
            } else {
                notificationService.notify(saved.getBuyerEmail(), "TRANSACTION", title, body, "MarketplaceTransaction", saved.getId());
                userRepo.findByEmailIgnoreCase(saved.getBuyerEmail()).ifPresent(u -> pushToken(u, title, body));
            }
        }

        return saved;
    }

    @PostMapping("/api/marketplace/transactions/{id}/initiate-payment")
    @PreAuthorize("hasAuthority('ROLE_BUYER')")
    public Map<String, Object> initiatePayment(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id
    ) {
        MarketplaceTransaction tx = transactionRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
        if (!tx.getBuyerEmail().equalsIgnoreCase(user.email())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This isn't your transaction");
        }
        if ("PAID".equals(tx.getPaymentStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This transaction is already paid");
        }
        if (tx.getAgreedPrice() == null || tx.getAgreedPrice().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction has no payable amount");
        }

        // Paystack expects amount in the lowest currency unit — pesewas for GHS, i.e. cedis * 100.
        // agreedPrice is already scale-2 cedis, so this multiply is exact (no fractional pesewas).
        long amountPesewas = tx.getAgreedPrice()
            .multiply(BigDecimal.valueOf(100))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact();

        // Fresh reference per attempt — lets a buyer retry after a FAILED/abandoned checkout
        // without colliding with the previous attempt's reference at Paystack's end.
        String reference = "mops-tx" + tx.getId() + "-" + System.currentTimeMillis();

        PaystackService.InitResult result = paystackService.initializeTransaction(user.email(), amountPesewas, reference);

        tx.setPaystackReference(result.reference());
        tx.setPaymentStatus("PENDING");
        transactionRepo.save(tx);

        auditLogService.record("PAYMENT_INITIATED", user.role(), user.fullName(), user.email(),
            "MARKETPLACE_TRANSACTION", id, "reference=" + result.reference() + " amountPesewas=" + amountPesewas);

        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("authorizationUrl", result.authorizationUrl());
        response.put("reference", result.reference());
        return response;
    }

    @GetMapping("/api/marketplace/transactions/{id}/payment-status")
    @PreAuthorize("hasAnyAuthority('ROLE_BUYER','ROLE_SUPERVISOR')")
    public MarketplaceTransaction getPaymentStatus(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id
    ) {
        MarketplaceTransaction tx = transactionRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
        boolean isOwner = "buyer".equals(user.role()) && tx.getBuyerEmail().equalsIgnoreCase(user.email());
        boolean isSiteSupervisor = "supervisor".equals(user.role())
            && user.assignedSite() != null && user.assignedSite().equalsIgnoreCase(tx.getSite());
        if (!isOwner && !isSiteSupervisor) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your transaction");
        }

        // Manual fallback for whenever the webhook hasn't fired yet (e.g. Paystack webhook URL not
        // configured in the dashboard, or delayed delivery) — re-check with Paystack directly on
        // every poll while still PENDING. No-ops (returns as-is) if PaystackService isn't configured.
        if ("PENDING".equals(tx.getPaymentStatus()) && tx.getPaystackReference() != null && paystackService.isConfigured()) {
            try {
                PaystackService.VerifyResult verified = paystackService.verifyTransaction(tx.getPaystackReference());
                applyVerifiedPayment(tx, verified, "manual-check by " + user.email());
            } catch (ResponseStatusException e) {
                // Paystack unreachable or reference not found yet — leave status as PENDING, don't 500 the poll.
                log.warn("Payment status check couldn't reach Paystack for tx {}: {}", id, e.getReason());
            }
        }
        return tx;
    }

    /** Shared by the manual status-check above and the webhook receiver — applies a Paystack
     *  verify/webhook result to the transaction row exactly once (idempotent: does nothing if
     *  already PAID) and fires the buyer/supervisor notifications on the transition to PAID. */
    void applyVerifiedPayment(MarketplaceTransaction tx, PaystackService.VerifyResult verified, String source) {
        if ("PAID".equals(tx.getPaymentStatus())) return; // already applied — avoid double notifications

        if (verified.success()) {
            tx.setPaymentStatus("PAID");
            tx.setPaidAmount(BigDecimal.valueOf(verified.amountPesewas()).divide(BigDecimal.valueOf(100)));
            tx.setPaidAt(LocalDateTime.now());
            tx.setPaymentChannel(verified.channel());
            transactionRepo.save(tx);

            auditLogService.record("PAYMENT_CONFIRMED", "system", "Paystack", "paystack",
                "MARKETPLACE_TRANSACTION", tx.getId(), source + " — reference=" + tx.getPaystackReference());

            String title = "Payment Received";
            String body = "Payment of GHS " + tx.getAgreedPrice() + " confirmed for " + tx.getMineralType() + " (Txn #" + tx.getId() + ").";
            notificationService.notify(tx.getBuyerEmail(), "PAYMENT", title, body, "MarketplaceTransaction", tx.getId());
            userRepo.findByEmailIgnoreCase(tx.getBuyerEmail()).ifPresent(u -> pushToken(u, title, body));

            for (AppUser recipient : userRepo.findByRoleInAndAssignedSiteIgnoreCase(List.of("supervisor"), tx.getSite())) {
                notificationService.notify(recipient.getEmail(), "PAYMENT", title, body, "MarketplaceTransaction", tx.getId());
                pushToken(recipient, title, body);
            }
        } else if ("failed".equals(verified.status()) || "abandoned".equals(verified.status())) {
            tx.setPaymentStatus("FAILED");
            transactionRepo.save(tx);
        }
    }

    private void pushToken(AppUser u, String title, String body) {
        String token = u.getPushToken();
        if (token != null && !token.isBlank()) {
            pushNotificationService.sendToToken(token, title, body, "default");
        }
    }

    // Public — see SecurityConfig's permitAll matcher for this exact path. No @PreAuthorize: this
    // is called by Paystack's servers, not a logged-in MineOps user, so there's no JWT to check.
    // Authenticity instead comes entirely from the HMAC signature check below. Takes the body as a
    // raw String (not a parsed DTO) because the signature must be verified against the exact raw
    // bytes Paystack sent — re-serializing a parsed object can produce a different byte string and
    // fail a legitimate signature.
    @PostMapping("/api/webhooks/paystack")
    public ResponseEntity<String> paystackWebhook(
        @RequestBody String rawBody,
        @org.springframework.web.bind.annotation.RequestHeader(value = "x-paystack-signature", required = false) String signature
    ) {
        if (!paystackService.verifyWebhookSignature(rawBody, signature)) {
            log.warn("Rejected Paystack webhook with invalid/missing signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid signature");
        }

        try {
            com.fasterxml.jackson.databind.JsonNode root = webhookObjectMapper.readTree(rawBody);
            String event = root.path("event").asText("");
            String reference = root.path("data").path("reference").asText(null);

            if (!"charge.success".equals(event) || reference == null) {
                // Other event types (e.g. charge.failed) or malformed payloads — ack and ignore.
                return ResponseEntity.ok("ignored");
            }

            transactionRepo.findByPaystackReference(reference).ifPresentOrElse(tx -> {
                // Re-verify against Paystack directly rather than trusting the webhook body's amount
                // outright — defense in depth against a spoofed-but-signature-valid replay scenario
                // and against acting on stale/partial webhook payloads.
                PaystackService.VerifyResult verified = paystackService.verifyTransaction(reference);
                applyVerifiedPayment(tx, verified, "webhook");
            }, () -> log.warn("Paystack webhook for unknown reference: {}", reference));
        } catch (Exception e) {
            log.error("Failed processing Paystack webhook: {}", e.getMessage());
            // Still 200 — Paystack retries on non-2xx, and a parsing bug on our end shouldn't cause
            // it to hammer this endpoint repeatedly. The manual payment-status poll is the fallback.
        }
        return ResponseEntity.ok("ok");
    }

    // Landing page after Paystack's checkout redirects the buyer's browser back — not used to
    // detect payment completion (the webhook + payment-status poll handle that independently), just
    // a friendly confirmation so the buyer isn't left looking at a raw JSON response or a 404.
    @GetMapping("/api/webhooks/paystack/callback")
    public ResponseEntity<String> paystackCallbackPage() {
        String html = "<html><body style=\"font-family:sans-serif;text-align:center;padding:60px 20px;\">"
            + "<h2>Payment received</h2><p>You can close this window and return to the MineOps app.</p>"
            + "</body></html>";
        return ResponseEntity.ok().header("Content-Type", "text/html; charset=UTF-8").body(html);
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
