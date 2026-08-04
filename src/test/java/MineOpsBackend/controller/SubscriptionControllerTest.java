package MineOpsBackend.controller;

import MineOpsBackend.model.SiteSubscription;
import MineOpsBackend.model.SubscriptionPayment;
import MineOpsBackend.model.SubscriptionTier;
import MineOpsBackend.repository.SiteSubscriptionRepository;
import MineOpsBackend.repository.SubscriptionPaymentRepository;
import MineOpsBackend.repository.SubscriptionTierRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    @Mock SubscriptionTierRepository tierRepo;
    @Mock SiteSubscriptionRepository subscriptionRepo;
    @Mock SubscriptionPaymentRepository paymentRepo;
    @Mock AuditLogService auditLogService;

    @InjectMocks SubscriptionController controller;

    private static final String SITE = "Obuasi Mine";

    // ── getActiveTiers ────────────────────────────────────────────────────────────

    @Test
    void getActiveTiers_callsFindByActiveTrue_andReturnsResult() {
        SubscriptionTier t = new SubscriptionTier();
        t.setName("Standard");
        t.setMonthlyPriceGhs(BigDecimal.valueOf(500));
        t.setActive(true);
        when(tierRepo.findByActiveTrue()).thenReturn(List.of(t));

        List<SubscriptionTier> result = controller.getActiveTiers();

        verify(tierRepo).findByActiveTrue();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Standard");
    }

    // ── getMySiteSubscription — security ──────────────────────────────────────────

    @Test
    void getMySiteSubscription_requiresSupervisorAuthority() throws NoSuchMethodException {
        PreAuthorize ann = SubscriptionController.class
            .getMethod("getMySiteSubscription", AuthenticatedUser.class)
            .getAnnotation(PreAuthorize.class);
        assertThat(ann).isNotNull();
        assertThat(ann.value()).contains("ROLE_SUPERVISOR");
    }

    // ── getMySiteSubscription — business logic ────────────────────────────────────

    @Test
    void getMySiteSubscription_returnsBlankShell_whenNoneExists() {
        when(subscriptionRepo.findBySiteIgnoreCase(SITE)).thenReturn(Optional.empty());

        SiteSubscription result = controller.getMySiteSubscription(supervisorAt(SITE));

        assertThat(result.getSite()).isEqualTo(SITE);
        assertThat(result.getId()).isNull(); // not persisted — blank shell only
        verify(subscriptionRepo, never()).save(any());
    }

    @Test
    void getMySiteSubscription_returnsExistingRow_whenFound() {
        SiteSubscription existing = new SiteSubscription();
        existing.setSite(SITE);
        existing.setStatus("ACTIVE");
        when(subscriptionRepo.findBySiteIgnoreCase(SITE)).thenReturn(Optional.of(existing));

        SiteSubscription result = controller.getMySiteSubscription(supervisorAt(SITE));

        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        verify(subscriptionRepo, never()).save(any());
    }

    @Test
    void getMySiteSubscription_neverQueriesOtherSite() {
        when(subscriptionRepo.findBySiteIgnoreCase("Site A")).thenReturn(Optional.empty());

        controller.getMySiteSubscription(supervisorAt("Site A"));

        verify(subscriptionRepo, never()).findBySiteIgnoreCase("Site B");
    }

    // ── recordPayment — security ───────────────────────────────────────────────────

    @Test
    void recordPayment_requiresSupervisorAuthority() throws NoSuchMethodException {
        PreAuthorize ann = SubscriptionController.class
            .getMethod("recordPayment", AuthenticatedUser.class, Map.class)
            .getAnnotation(PreAuthorize.class);
        assertThat(ann).isNotNull();
        assertThat(ann.value()).contains("ROLE_SUPERVISOR");
    }

    // ── recordPayment — 400: amountGhs ────────────────────────────────────────────

    @Test
    void recordPayment_throws400_whenAmountGhsMissing() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.recordPayment(supervisorAt(SITE),
                Map.of("periodCoveredEnd", "2026-07-31")));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void recordPayment_throws400_whenAmountGhsBlank() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.recordPayment(supervisorAt(SITE),
                Map.of("amountGhs", "", "periodCoveredEnd", "2026-07-31")));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void recordPayment_throws400_whenAmountGhsNonNumeric() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.recordPayment(supervisorAt(SITE),
                Map.of("amountGhs", "five-hundred", "periodCoveredEnd", "2026-07-31")));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── recordPayment — 400: periodCoveredEnd ─────────────────────────────────────

    @Test
    void recordPayment_throws400_whenPeriodCoveredEndMissing() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.recordPayment(supervisorAt(SITE),
                Map.of("amountGhs", "500")));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void recordPayment_throws400_whenPeriodCoveredEndBlank() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.recordPayment(supervisorAt(SITE),
                Map.of("amountGhs", "500", "periodCoveredEnd", "")));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void recordPayment_throws400_whenPeriodCoveredEndInvalidDateFormat() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.recordPayment(supervisorAt(SITE),
                Map.of("amountGhs", "500", "periodCoveredEnd", "31-07-2026")));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── recordPayment — silent ignores ────────────────────────────────────────────

    @Test
    void recordPayment_ignoresInvalidPeriodCoveredStart_noError() {
        when(subscriptionRepo.findBySiteIgnoreCase(SITE)).thenReturn(Optional.empty());

        // Invalid start date silently swallowed — matches catch (DateTimeParseException ignored) {}
        Map<String, Object> result = controller.recordPayment(supervisorAt(SITE), Map.of(
            "amountGhs", "500",
            "periodCoveredEnd", "2026-07-31",
            "periodCoveredStart", "not-a-date"
        ));

        assertThat(result).containsKey("subscription");

        ArgumentCaptor<SubscriptionPayment> cap = ArgumentCaptor.forClass(SubscriptionPayment.class);
        verify(paymentRepo).save(cap.capture());
        assertThat(cap.getValue().getPeriodCoveredStart()).isNull();
    }

    @Test
    void recordPayment_ignoresNonNumericTierId_noError() {
        when(subscriptionRepo.findBySiteIgnoreCase(SITE)).thenReturn(Optional.empty());

        // Non-numeric tierId silently swallowed — matches catch (NumberFormatException ignored) {}
        Map<String, Object> result = controller.recordPayment(supervisorAt(SITE), Map.of(
            "amountGhs", "500",
            "periodCoveredEnd", "2026-07-31",
            "tierId", "GOLD"
        ));

        ArgumentCaptor<SiteSubscription> cap = ArgumentCaptor.forClass(SiteSubscription.class);
        verify(subscriptionRepo).save(cap.capture());
        assertThat(cap.getValue().getTierId()).isNull();
        assertThat(result).containsKey("subscription");
    }

    // ── recordPayment — principal stamping ────────────────────────────────────────

    @Test
    void recordPayment_setsPaymentSiteAndEmailFromPrincipal() {
        when(subscriptionRepo.findBySiteIgnoreCase(SITE)).thenReturn(Optional.empty());

        controller.recordPayment(supervisorAt(SITE),
            Map.of("amountGhs", "750", "periodCoveredEnd", "2026-07-31"));

        ArgumentCaptor<SubscriptionPayment> cap = ArgumentCaptor.forClass(SubscriptionPayment.class);
        verify(paymentRepo).save(cap.capture());
        assertThat(cap.getValue().getSite()).isEqualTo(SITE);
        assertThat(cap.getValue().getRecordedByEmail()).isEqualTo("kwame@mine.com");
        assertThat(cap.getValue().getAmountGhs()).isEqualByComparingTo("750");
    }

    // ── recordPayment — upsert: new row ───────────────────────────────────────────

    @Test
    void recordPayment_createsNewSubscription_setsActiveAndPeriodEnd_whenNoneExists() {
        when(subscriptionRepo.findBySiteIgnoreCase(SITE)).thenReturn(Optional.empty());

        controller.recordPayment(supervisorAt(SITE),
            Map.of("amountGhs", "500", "periodCoveredEnd", "2026-07-31"));

        ArgumentCaptor<SiteSubscription> cap = ArgumentCaptor.forClass(SiteSubscription.class);
        verify(subscriptionRepo).save(cap.capture());
        assertThat(cap.getValue().getSite()).isEqualTo(SITE);
        assertThat(cap.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(cap.getValue().getCurrentPeriodEndsAt()).isNotNull();
    }

    // ── recordPayment — upsert: status transitions ────────────────────────────────

    @Test
    void recordPayment_setsStatusActive_fromTrial() {
        SiteSubscription trial = new SiteSubscription();
        trial.setSite(SITE);
        trial.setStatus("TRIAL");
        when(subscriptionRepo.findBySiteIgnoreCase(SITE)).thenReturn(Optional.of(trial));

        controller.recordPayment(supervisorAt(SITE),
            Map.of("amountGhs", "500", "periodCoveredEnd", "2026-07-31"));

        ArgumentCaptor<SiteSubscription> cap = ArgumentCaptor.forClass(SiteSubscription.class);
        verify(subscriptionRepo).save(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(cap.getValue().getCurrentPeriodEndsAt()).isNotNull();
    }

    @Test
    void recordPayment_setsStatusActive_fromPastDue() {
        SiteSubscription pastDue = new SiteSubscription();
        pastDue.setSite(SITE);
        pastDue.setStatus("PAST_DUE");
        when(subscriptionRepo.findBySiteIgnoreCase(SITE)).thenReturn(Optional.of(pastDue));

        controller.recordPayment(supervisorAt(SITE),
            Map.of("amountGhs", "500", "periodCoveredEnd", "2026-08-31"));

        ArgumentCaptor<SiteSubscription> cap = ArgumentCaptor.forClass(SiteSubscription.class);
        verify(subscriptionRepo).save(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(cap.getValue().getCurrentPeriodEndsAt()).isNotNull();
    }

    // ── recordPayment — tierId ────────────────────────────────────────────────────

    @Test
    void recordPayment_setsTierId_whenValidTierIdInBody() {
        when(subscriptionRepo.findBySiteIgnoreCase(SITE)).thenReturn(Optional.empty());

        controller.recordPayment(supervisorAt(SITE), Map.of(
            "amountGhs", "500",
            "periodCoveredEnd", "2026-07-31",
            "tierId", "3"
        ));

        ArgumentCaptor<SiteSubscription> cap = ArgumentCaptor.forClass(SiteSubscription.class);
        verify(subscriptionRepo).save(cap.capture());
        assertThat(cap.getValue().getTierId()).isEqualTo(3L);
    }

    // ── recordPayment — response shape ────────────────────────────────────────────

    @Test
    void recordPayment_responseContainsBothSubscriptionAndPaymentKeys() {
        when(subscriptionRepo.findBySiteIgnoreCase(SITE)).thenReturn(Optional.empty());

        Map<String, Object> result = controller.recordPayment(supervisorAt(SITE),
            Map.of("amountGhs", "500", "periodCoveredEnd", "2026-07-31"));

        assertThat(result).containsKey("subscription");
        assertThat(result).containsKey("payment");
        assertThat(result.get("subscription")).isInstanceOf(SiteSubscription.class);
        assertThat(result.get("payment")).isInstanceOf(SubscriptionPayment.class);
    }

    // ── recordPayment — site-scoping regression ───────────────────────────────────

    @Test
    void recordPayment_neverWritesToOtherSite() {
        // Site A supervisor: both the saved payment and the saved subscription must carry Site A.
        // Site B's subscription row must never be touched.
        when(subscriptionRepo.findBySiteIgnoreCase("Site A")).thenReturn(Optional.empty());

        controller.recordPayment(supervisorAt("Site A"),
            Map.of("amountGhs", "500", "periodCoveredEnd", "2026-07-31"));

        ArgumentCaptor<SubscriptionPayment> paymentCap = ArgumentCaptor.forClass(SubscriptionPayment.class);
        verify(paymentRepo).save(paymentCap.capture());
        assertThat(paymentCap.getValue().getSite()).isEqualTo("Site A");

        ArgumentCaptor<SiteSubscription> subCap = ArgumentCaptor.forClass(SiteSubscription.class);
        verify(subscriptionRepo).save(subCap.capture());
        assertThat(subCap.getValue().getSite()).isEqualTo("Site A");

        verify(subscriptionRepo, never()).findBySiteIgnoreCase("Site B");
    }

    // ── helpers ───────────────────────────────────────────────────────────────────

    private AuthenticatedUser supervisorAt(String site) {
        return new AuthenticatedUser(1L, "Kwame", "kwame@mine.com", "supervisor", site, null);
    }
}
