package MineOpsBackend.controller;

import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.Site;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.SiteRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import MineOpsBackend.service.InsuranceProviderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsuranceControllerTest {

    @Mock AppUserRepository userRepo;
    @Mock SiteRepository siteRepo;
    @Mock InsuranceProviderService insuranceProviderService;
    @Mock AuditLogService auditLogService;

    @InjectMocks InsuranceController controller;

    private static final String SITE = "Obuasi Mine";

    private AuthenticatedUser worker() {
        return new AuthenticatedUser(5L, "Alice", "alice@mine.com", "worker", SITE, null);
    }

    private Site enabledSite() {
        Site s = new Site(SITE, "ACTIVE");
        s.setInsuranceEnabled(true);
        s.setInsuranceProviderName("SafeMine");
        return s;
    }

    private AppUser notInsuredUser() {
        AppUser u = new AppUser("Alice", "alice@mine.com", "hash", "worker", SITE);
        // insuranceStatus defaults to NOT_INSURED
        return u;
    }

    // ── getStatus ─────────────────────────────────────────────────

    @Test
    void getStatus_returns_NOT_AVAILABLE_whenSiteNotFound() {
        when(siteRepo.findByNameIgnoreCase(SITE)).thenReturn(Optional.empty());

        Map<String, Object> result = controller.getStatus(worker());

        assertThat(result.get("status")).isEqualTo("NOT_AVAILABLE");
    }

    @Test
    void getStatus_returns_NOT_AVAILABLE_whenInsuranceDisabled() {
        Site s = new Site(SITE, "ACTIVE"); // insuranceEnabled defaults to false
        when(siteRepo.findByNameIgnoreCase(SITE)).thenReturn(Optional.of(s));

        Map<String, Object> result = controller.getStatus(worker());

        assertThat(result.get("status")).isEqualTo("NOT_AVAILABLE");
    }

    @Test
    void getStatus_returns_workerStatus_whenInsuranceEnabled() {
        when(siteRepo.findByNameIgnoreCase(SITE)).thenReturn(Optional.of(enabledSite()));
        AppUser u = notInsuredUser();
        when(userRepo.findById(5L)).thenReturn(Optional.of(u));

        Map<String, Object> result = controller.getStatus(worker());

        assertThat(result.get("status")).isEqualTo("NOT_INSURED");
    }

    // ── apply ─────────────────────────────────────────────────────

    @Test
    void apply_enrollsWorker_andAudits() {
        when(siteRepo.findByNameIgnoreCase(SITE)).thenReturn(Optional.of(enabledSite()));
        AppUser u = notInsuredUser();
        when(userRepo.findById(5L)).thenReturn(Optional.of(u));
        when(userRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        controller.apply(worker());

        verify(insuranceProviderService).enroll(u);
        verify(userRepo).save(u);
        verify(auditLogService).record(
            "INSURANCE_ENROLLED", "worker", "Alice", "alice@mine.com",
            "APP_USER", u.getId(), "site=" + SITE
        );
    }

    @Test
    void apply_throws403_whenInsuranceDisabled() {
        Site s = new Site(SITE, "ACTIVE");
        when(siteRepo.findByNameIgnoreCase(SITE)).thenReturn(Optional.of(s));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.apply(worker()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(insuranceProviderService, never()).enroll(any());
    }

    @Test
    void apply_throws409_whenAlreadyInsured() {
        when(siteRepo.findByNameIgnoreCase(SITE)).thenReturn(Optional.of(enabledSite()));
        AppUser u = notInsuredUser();
        u.setInsuranceStatus("INSURED");
        when(userRepo.findById(5L)).thenReturn(Optional.of(u));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.apply(worker()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(insuranceProviderService, never()).enroll(any());
    }
}
