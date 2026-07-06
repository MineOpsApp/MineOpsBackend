package MineOpsBackend.controller;

import MineOpsBackend.model.MiningPermitStatus;
import MineOpsBackend.repository.MiningPermitStatusRepository;
import MineOpsBackend.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MiningPermitControllerTest {

    @Mock MiningPermitStatusRepository permitRepo;
    @InjectMocks MiningPermitController controller;

    // ── Security annotations ──────────────────────────────────────────────────────

    @Test
    void getMyPermit_requiresSupervisorAuthority() throws NoSuchMethodException {
        PreAuthorize ann = MiningPermitController.class
            .getMethod("getMyPermit", AuthenticatedUser.class)
            .getAnnotation(PreAuthorize.class);
        assertThat(ann).isNotNull();
        assertThat(ann.value()).contains("ROLE_SUPERVISOR");
    }

    @Test
    void updateMyPermit_requiresSupervisorAuthority() throws NoSuchMethodException {
        PreAuthorize ann = MiningPermitController.class
            .getMethod("updateMyPermit", AuthenticatedUser.class, Map.class)
            .getAnnotation(PreAuthorize.class);
        assertThat(ann).isNotNull();
        assertThat(ann.value()).contains("ROLE_SUPERVISOR");
    }

    // ── getMyPermit ───────────────────────────────────────────────────────────────

    @Test
    void getMyPermit_returnsExistingPermit_whenFound() {
        MiningPermitStatus existing = new MiningPermitStatus();
        existing.setSite("Obuasi Mine");
        existing.setApplicationSubmitted(true);
        when(permitRepo.findBySiteIgnoreCase("Obuasi Mine")).thenReturn(Optional.of(existing));

        MiningPermitStatus result = controller.getMyPermit(supervisorAt("Obuasi Mine"));

        assertThat(result.getSite()).isEqualTo("Obuasi Mine");
        assertThat(result.getApplicationSubmitted()).isTrue();
    }

    @Test
    void getMyPermit_returnsBlankScopedToCallerSite_whenNoneExists() {
        when(permitRepo.findBySiteIgnoreCase("Obuasi Mine")).thenReturn(Optional.empty());

        MiningPermitStatus result = controller.getMyPermit(supervisorAt("Obuasi Mine"));

        assertThat(result.getSite()).isEqualTo("Obuasi Mine");
        assertThat(result.getId()).isNull(); // blank shell — not persisted, not another site's row
    }

    @Test
    void getMyPermit_neverQueriesOtherSite() {
        // Site A supervisor always gets Site A's permit (or a blank for Site A).
        // Site B's row is never fetched — enforced by always querying user.assignedSite().
        when(permitRepo.findBySiteIgnoreCase("Site A")).thenReturn(Optional.empty());

        controller.getMyPermit(supervisorAt("Site A"));

        verify(permitRepo, never()).findBySiteIgnoreCase("Site B");
    }

    // ── updateMyPermit ────────────────────────────────────────────────────────────

    @Test
    void updateMyPermit_partialUpdate_onlyChangesProvidedFields() {
        MiningPermitStatus existing = new MiningPermitStatus();
        existing.setSite("Obuasi Mine");
        existing.setApplicationSubmitted(false);
        existing.setEpaPermitObtained(false);
        when(permitRepo.findBySiteIgnoreCase("Obuasi Mine")).thenReturn(Optional.of(existing));
        when(permitRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Body contains only applicationSubmitted — epaPermitObtained must stay false
        MiningPermitStatus result = controller.updateMyPermit(
            supervisorAt("Obuasi Mine"), Map.of("applicationSubmitted", "true"));

        assertThat(result.getApplicationSubmitted()).isTrue();
        assertThat(result.getEpaPermitObtained()).isFalse();
    }

    @Test
    void updateMyPermit_setsUpdatedByEmailFromPrincipal() {
        when(permitRepo.findBySiteIgnoreCase("Obuasi Mine")).thenReturn(Optional.empty());
        when(permitRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MiningPermitStatus result = controller.updateMyPermit(
            supervisorAt("Obuasi Mine"), Map.of());

        assertThat(result.getUpdatedByEmail()).isEqualTo("kwame@mine.com");
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    void updateMyPermit_neverWritesToOtherSite() {
        // Site A supervisor's update must be saved with Site A — never Site B.
        when(permitRepo.findBySiteIgnoreCase("Site A")).thenReturn(Optional.empty());
        when(permitRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MiningPermitStatus result = controller.updateMyPermit(
            supervisorAt("Site A"), Map.of("applicationSubmitted", "true"));

        verify(permitRepo).save(any());
        assertThat(result.getSite()).isEqualTo("Site A");
        verify(permitRepo, never()).findBySiteIgnoreCase("Site B");
    }

    @Test
    void updateMyPermit_upsertsNewPermit_whenNoneExists() {
        when(permitRepo.findBySiteIgnoreCase("New Site")).thenReturn(Optional.empty());
        when(permitRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MiningPermitStatus result = controller.updateMyPermit(
            supervisorAt("New Site"), Map.of("epaPermitObtained", "true"));

        verify(permitRepo).save(any());
        assertThat(result.getSite()).isEqualTo("New Site");
        assertThat(result.getEpaPermitObtained()).isTrue();
    }

    // ── helpers ───────────────────────────────────────────────────────────────────

    private AuthenticatedUser supervisorAt(String site) {
        return new AuthenticatedUser(1L, "Kwame", "kwame@mine.com", "supervisor", site, null);
    }
}
