package MineOpsBackend.controller;

import MineOpsBackend.dto.CreateListingRequest;
import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.MineralListing;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.MineralListingRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MineralListingControllerTest {

    @Mock MineralListingRepository listingRepo;
    @Mock AppUserRepository userRepo;
    @Mock AuditLogService auditLogService;

    @InjectMocks MineralListingController controller;

    private static final String SITE = "Obuasi Mine";

    private AuthenticatedUser supervisor() {
        return new AuthenticatedUser(1L, "Kwame", "kwame@mine.com", "supervisor", SITE, null);
    }

    private AuthenticatedUser buyer(Long id) {
        return new AuthenticatedUser(id, "Ama", "ama@buyer.com", "buyer", null, null);
    }

    private AppUser verifiedBuyer() {
        AppUser u = new AppUser("Ama", "ama@buyer.com", "hash", "buyer", null);
        u.setBuyerVerificationStatus("VERIFIED");
        return u;
    }

    private AppUser pendingBuyer() {
        AppUser u = new AppUser("Ama", "ama@buyer.com", "hash", "buyer", null);
        u.setBuyerVerificationStatus("PENDING_VERIFICATION");
        return u;
    }

    private MineralListing activeListing(String site) {
        MineralListing l = new MineralListing();
        l.setSite(site);
        l.setMineralType("Gold");
        l.setQuantity(BigDecimal.valueOf(500));
        l.setUnit("oz");
        l.setAskingPrice(BigDecimal.valueOf(250000));
        l.setStatus("ACTIVE");
        return l;
    }

    // ── createListing ──────────────────────────────────────────────

    @Test
    void createListing_persistsWithSupervisorSiteAndAudits() {
        when(listingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateListingRequest request = new CreateListingRequest(
            "Gold", BigDecimal.valueOf(500), "oz", "22 carat",
            BigDecimal.valueOf(250000), "Block 3", null, null, null
        );

        MineralListing result = controller.createListing(supervisor(), request);

        ArgumentCaptor<MineralListing> cap = ArgumentCaptor.forClass(MineralListing.class);
        verify(listingRepo).save(cap.capture());
        assertThat(cap.getValue().getSite()).isEqualTo(SITE);
        assertThat(cap.getValue().getMineralType()).isEqualTo("Gold");
        assertThat(cap.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getSite()).isEqualTo(SITE);

        verify(auditLogService).record(
            "LISTING_CREATED", "supervisor", "Kwame", "kwame@mine.com",
            "MINERAL_LISTING", result.getId(), "Gold @ " + SITE
        );
    }

    // ── getListings (buyer) ────────────────────────────────────────

    @Test
    void getListings_throws404_whenBuyerUserNotFound() {
        when(userRepo.findById(10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.getListings(buyer(10L)));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getListings_throws403_whenBuyerNotVerified() {
        when(userRepo.findById(10L)).thenReturn(Optional.of(pendingBuyer()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.getListings(buyer(10L)));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(listingRepo, never()).findByStatusOrderByCreatedAtDesc(any());
    }

    @Test
    void getListings_returnsAllActive_whenBuyerVerified() {
        when(userRepo.findById(10L)).thenReturn(Optional.of(verifiedBuyer()));
        MineralListing l = activeListing(SITE);
        when(listingRepo.findByStatusOrderByCreatedAtDesc("ACTIVE")).thenReturn(List.of(l));

        List<MineralListing> result = controller.getListings(buyer(10L));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMineralType()).isEqualTo("Gold");
    }

    // ── getListings (supervisor) ───────────────────────────────────

    @Test
    void getListings_returnsSiteListings_forSupervisor() {
        MineralListing l = activeListing(SITE);
        when(listingRepo.findBySiteIgnoreCaseOrderByCreatedAtDesc(SITE)).thenReturn(List.of(l));

        List<MineralListing> result = controller.getListings(supervisor());

        assertThat(result).hasSize(1);
        verify(userRepo, never()).findById(any());
    }

    // ── withdrawListing ────────────────────────────────────────────

    @Test
    void withdrawListing_throws404_whenListingNotFound() {
        when(listingRepo.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.withdrawListing(supervisor(), 99L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void withdrawListing_throws403_whenListingBelongsToDifferentSite() {
        MineralListing l = activeListing("Tarkwa Mine");
        when(listingRepo.findById(1L)).thenReturn(Optional.of(l));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.withdrawListing(supervisor(), 1L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(listingRepo, never()).save(any());
    }

    @Test
    void withdrawListing_throws409_whenListingNotActive() {
        MineralListing l = activeListing(SITE);
        l.setStatus("SOLD");
        when(listingRepo.findById(1L)).thenReturn(Optional.of(l));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.withdrawListing(supervisor(), 1L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(listingRepo, never()).save(any());
    }

    @Test
    void createListing_throws400_whenMinOrderQuantityExceedsQuantity() {
        CreateListingRequest request = new CreateListingRequest(
            "Gold", BigDecimal.valueOf(10), "oz", null,
            BigDecimal.valueOf(250000), null, null, BigDecimal.valueOf(1000), null
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.createListing(supervisor(), request));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(listingRepo, never()).save(any());
    }

    @Test
    void withdrawListing_setsWithdrawnAndAudits() {
        MineralListing l = activeListing(SITE);
        when(listingRepo.findById(1L)).thenReturn(Optional.of(l));
        when(listingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MineralListing result = controller.withdrawListing(supervisor(), 1L);

        assertThat(result.getStatus()).isEqualTo("WITHDRAWN");
        verify(auditLogService).record(
            "LISTING_WITHDRAWN", "supervisor", "Kwame", "kwame@mine.com",
            "MINERAL_LISTING", result.getId(), "Gold @ " + SITE
        );
    }
}
