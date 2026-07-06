package MineOpsBackend.controller;

import MineOpsBackend.model.SiteSubscription;
import MineOpsBackend.model.SubscriptionPayment;
import MineOpsBackend.model.SubscriptionTier;
import MineOpsBackend.repository.SiteSubscriptionRepository;
import MineOpsBackend.repository.SubscriptionPaymentRepository;
import MineOpsBackend.repository.SubscriptionTierRepository;
import MineOpsBackend.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionTierRepository tierRepo;
    private final SiteSubscriptionRepository subscriptionRepo;
    private final SubscriptionPaymentRepository paymentRepo;

    public SubscriptionController(
        SubscriptionTierRepository tierRepo,
        SiteSubscriptionRepository subscriptionRepo,
        SubscriptionPaymentRepository paymentRepo
    ) {
        this.tierRepo = tierRepo;
        this.subscriptionRepo = subscriptionRepo;
        this.paymentRepo = paymentRepo;
    }

    @GetMapping("/tiers")
    public List<SubscriptionTier> getActiveTiers() {
        return tierRepo.findByActiveTrue();
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public SiteSubscription getMySiteSubscription(@AuthenticationPrincipal AuthenticatedUser user) {
        return subscriptionRepo.findBySiteIgnoreCase(user.assignedSite())
            .orElseGet(() -> {
                SiteSubscription blank = new SiteSubscription();
                blank.setSite(user.assignedSite());
                return blank;
            });
    }

    @PostMapping("/mine/record-payment")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public Map<String, Object> recordPayment(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody Map<String, Object> body
    ) {
        String amtStr = body.get("amountGhs") != null ? body.get("amountGhs").toString() : null;
        if (amtStr == null || amtStr.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amountGhs is required");
        }
        BigDecimal amount;
        try { amount = new BigDecimal(amtStr); }
        catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid amountGhs");
        }

        String periodEndStr = (String) body.get("periodCoveredEnd");
        if (periodEndStr == null || periodEndStr.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "periodCoveredEnd is required (YYYY-MM-DD)");
        }
        LocalDate periodEnd;
        try { periodEnd = LocalDate.parse(periodEndStr); }
        catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid periodCoveredEnd — use YYYY-MM-DD");
        }

        SubscriptionPayment payment = new SubscriptionPayment();
        payment.setSite(user.assignedSite());
        payment.setAmountGhs(amount);
        payment.setMethod((String) body.get("method"));
        payment.setReference((String) body.get("reference"));
        payment.setRecordedByEmail(user.email());
        payment.setPeriodCoveredEnd(periodEnd);
        String startStr = (String) body.get("periodCoveredStart");
        if (startStr != null && !startStr.isBlank()) {
            try { payment.setPeriodCoveredStart(LocalDate.parse(startStr)); }
            catch (DateTimeParseException ignored) {}
        }
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepo.save(payment);

        SiteSubscription sub = subscriptionRepo.findBySiteIgnoreCase(user.assignedSite())
            .orElseGet(() -> {
                SiteSubscription s = new SiteSubscription();
                s.setSite(user.assignedSite());
                s.setCreatedAt(LocalDateTime.now());
                return s;
            });
        sub.setStatus("ACTIVE");
        sub.setCurrentPeriodEndsAt(periodEnd.atStartOfDay());
        Object tierIdObj = body.get("tierId");
        if (tierIdObj != null) {
            try { sub.setTierId(Long.parseLong(tierIdObj.toString())); }
            catch (NumberFormatException ignored) {}
        }
        subscriptionRepo.save(sub);

        return Map.of("subscription", sub, "payment", payment);
    }
}
