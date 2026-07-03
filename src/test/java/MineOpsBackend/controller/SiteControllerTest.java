package MineOpsBackend.controller;

import MineOpsBackend.model.Site;
import MineOpsBackend.repository.SiteRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteControllerTest {

    @Mock SiteRepository siteRepository;
    @Mock AuditLogService auditLogService;

    @InjectMocks SiteController controller;

    private static final String SITE = "Obuasi Mine";

    private AuthenticatedUser supervisor() {
        return new AuthenticatedUser(1L, "Super", "sup@mine.com", "supervisor", SITE, null);
    }

    private Site siteEntity() {
        return new Site(SITE, "ACTIVE");
    }

    @Test
    void updateVisibility_setsTrue_andAudits() {
        Site entity = siteEntity();
        when(siteRepository.findByNameIgnoreCase(SITE)).thenReturn(Optional.of(entity));
        when(siteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Site result = controller.updateVisibility(supervisor(), Map.of("visible", true));

        assertThat(result.isInventoryVisibleToGuests()).isTrue();
        verify(auditLogService).record(
            "INVENTORY_VISIBILITY_CHANGED",
            "supervisor", "Super", "sup@mine.com",
            "SITE", result.getId(),
            "inventoryVisibleToGuests=true"
        );
    }

    @Test
    void updateVisibility_setsFalse() {
        Site entity = siteEntity();
        entity.setInventoryVisibleToGuests(true);
        when(siteRepository.findByNameIgnoreCase(SITE)).thenReturn(Optional.of(entity));
        when(siteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Site result = controller.updateVisibility(supervisor(), Map.of("visible", false));

        assertThat(result.isInventoryVisibleToGuests()).isFalse();
    }

    @Test
    void updateVisibility_throws404_whenSiteNotFound() {
        when(siteRepository.findByNameIgnoreCase(SITE)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.updateVisibility(supervisor(), Map.of("visible", true)));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
