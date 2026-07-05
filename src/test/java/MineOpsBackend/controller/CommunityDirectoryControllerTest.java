package MineOpsBackend.controller;

import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.Site;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.SiteRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.SafetyScoreService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
class CommunityDirectoryControllerTest {

    @Mock SiteRepository siteRepo;
    @Mock AppUserRepository userRepo;
    @Mock SafetyScoreService safetyScoreService;

    @InjectMocks CommunityDirectoryController controller;

    private static final String SITE_NAME = "Obuasi Mine";

    private AuthenticatedUser supervisor() {
        return new AuthenticatedUser(1L, "Kwame", "kwame@mine.com", "supervisor", SITE_NAME, null);
    }

    private Site site() {
        Site s = new Site();
        s.setName(SITE_NAME);
        return s;
    }

    private AppUser verifiedBuyer(String email) {
        AppUser u = new AppUser("Ama", email, "hash", "buyer", null);
        u.setBuyerVerificationStatus("VERIFIED");
        return u;
    }

    // ── getVerifiedBuyers ─────────────────────────────────────────────────────

    @Test
    void getVerifiedBuyers_callsRepoWithLowercaseBuyerRole() {
        when(userRepo.findByRoleAndBuyerVerificationStatus("buyer", "VERIFIED"))
                .thenReturn(List.of());

        controller.getVerifiedBuyers();

        verify(userRepo).findByRoleAndBuyerVerificationStatus("buyer", "VERIFIED");
    }

    @Test
    void getVerifiedBuyers_mapsBusinessNameToCompanyName() {
        AppUser u = verifiedBuyer("ama@buyer.com");
        u.setBusinessName("AmaGold Ltd");
        when(userRepo.findByRoleAndBuyerVerificationStatus("buyer", "VERIFIED"))
                .thenReturn(List.of(u));

        List<Map<String, Object>> result = controller.getVerifiedBuyers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("companyName")).isEqualTo("AmaGold Ltd");
        assertThat(result.get(0).get("email")).isEqualTo("ama@buyer.com");
    }

    // ── getMines ──────────────────────────────────────────────────────────────

    @Test
    void getMines_returnsSafetyScoreForEachSite() {
        when(siteRepo.findAll()).thenReturn(List.of(site()));
        when(safetyScoreService.computeScore(SITE_NAME)).thenReturn(85);

        List<Map<String, Object>> result = controller.getMines();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("safetyScore")).isEqualTo(85);
        assertThat(result.get(0).get("name")).isEqualTo(SITE_NAME);
    }

    // ── getMine ───────────────────────────────────────────────────────────────

    @Test
    void getMine_throws404_whenSiteNotFound() {
        when(siteRepo.findByNameIgnoreCase("Ghost Mine")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.getMine("Ghost Mine"));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getMine_returnsProfile_whenFound() {
        Site s = site();
        s.setMineralsProduced("Gold, Silver");
        when(siteRepo.findByNameIgnoreCase(SITE_NAME)).thenReturn(Optional.of(s));
        when(safetyScoreService.computeScore(SITE_NAME)).thenReturn(70);

        Map<String, Object> result = controller.getMine(SITE_NAME);

        assertThat(result.get("mineralsProduced")).isEqualTo("Gold, Silver");
        assertThat(result.get("safetyScore")).isEqualTo(70);
    }

    // ── updateMineProfile ─────────────────────────────────────────────────────

    @Test
    void updateMineProfile_throws404_whenSiteNotFound() {
        AuthenticatedUser user = supervisor();
        when(siteRepo.findByNameIgnoreCase(SITE_NAME)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.updateMineProfile(user, Map.of("contactEmail", "info@mine.com")));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(siteRepo, never()).save(any());
    }

    @Test
    void updateMineProfile_updatesFieldsAndSaves() {
        Site s = site();
        when(siteRepo.findByNameIgnoreCase(SITE_NAME)).thenReturn(Optional.of(s));
        when(siteRepo.save(any())).thenReturn(s);
        when(safetyScoreService.computeScore(SITE_NAME)).thenReturn(90);

        Map<String, Object> body = Map.of(
                "mineralsProduced", "Gold",
                "contactEmail", "info@obuasi.com",
                "establishedYear", 1995
        );
        controller.updateMineProfile(supervisor(), body);

        assertThat(s.getMineralsProduced()).isEqualTo("Gold");
        assertThat(s.getContactEmail()).isEqualTo("info@obuasi.com");
        assertThat(s.getEstablishedYear()).isEqualTo(1995);
        verify(siteRepo).save(s);
    }
}
