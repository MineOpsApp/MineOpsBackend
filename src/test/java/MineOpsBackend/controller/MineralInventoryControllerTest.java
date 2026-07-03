package MineOpsBackend.controller;

import MineOpsBackend.model.MineralInventory;
import MineOpsBackend.model.Site;
import MineOpsBackend.repository.InventoryTransactionRepository;
import MineOpsBackend.repository.MineralInventoryRepository;
import MineOpsBackend.repository.SiteRepository;
import MineOpsBackend.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MineralInventoryControllerTest {

    @Mock MineralInventoryRepository inventoryRepository;
    @Mock InventoryTransactionRepository transactionRepository;
    @Mock SiteRepository siteRepository;

    @InjectMocks MineralInventoryController controller;

    private static final String SITE = "Obuasi Mine";

    private AuthenticatedUser guest() {
        return new AuthenticatedUser(10L, "Investor A", "inv@guest.com", "guest", SITE, "investor");
    }

    private Site siteWith(boolean visible) {
        Site s = new Site(SITE, "ACTIVE");
        s.setInventoryVisibleToGuests(visible);
        return s;
    }

    @Test
    void getPublicInventory_returnsInventory_whenVisibilityOn() {
        when(siteRepository.findByNameIgnoreCase(SITE)).thenReturn(Optional.of(siteWith(true)));
        MineralInventory item = new MineralInventory();
        when(inventoryRepository.findBySiteIgnoreCaseOrderByMineralTypeAsc(SITE))
            .thenReturn(List.of(item));

        List<MineralInventory> result = controller.getPublicInventory(guest());

        assertThat(result).containsExactly(item);
    }

    @Test
    void getPublicInventory_throws403_whenVisibilityOff() {
        when(siteRepository.findByNameIgnoreCase(SITE)).thenReturn(Optional.of(siteWith(false)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.getPublicInventory(guest()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getReason()).containsIgnoringCase("not opted in");
    }

    @Test
    void getPublicInventory_throws404_whenSiteNotFound() {
        when(siteRepository.findByNameIgnoreCase(SITE)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.getPublicInventory(guest()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
