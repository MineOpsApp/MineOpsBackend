package MineOpsBackend.controller;

import MineOpsBackend.model.SiteMap;
import MineOpsBackend.repository.SiteMapRepository;
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
class SiteMapControllerTest {

    @Mock SiteMapRepository siteMapRepo;
    @Mock AuditLogService auditLogService;

    @InjectMocks SiteMapController controller;

    private static final String SITE = "TestMine";

    private AuthenticatedUser supervisor() {
        return new AuthenticatedUser(1L, "Supervisor", "sup@mine.com", "supervisor", SITE, null);
    }

    @Test
    void uploadSiteMap_throws400_whenImageDataIsTooLarge() {
        String oversized = "x".repeat(2_000_001);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.uploadSiteMap(supervisor(), Map.of("imageData", oversized)));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getReason()).containsIgnoringCase("too large");
    }

    @Test
    void uploadSiteMap_throws400_whenImageDataIsBlank() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.uploadSiteMap(supervisor(), Map.of("imageData", "")));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void uploadSiteMap_createsNewRow_whenNoneExistsForSite() {
        when(siteMapRepo.findBySiteIgnoreCase(SITE)).thenReturn(Optional.empty());
        when(siteMapRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SiteMap saved = controller.uploadSiteMap(supervisor(), Map.of("imageData", "abc123"));

        assertThat(saved.getSite()).isEqualTo(SITE);
        assertThat(saved.getImageData()).isEqualTo("abc123");
        assertThat(saved.getUploadedBy()).isEqualTo("sup@mine.com");
        verify(siteMapRepo).save(any());
    }

    @Test
    void uploadSiteMap_replacesExistingRow_whenMapAlreadyUploaded() {
        SiteMap existing = new SiteMap(SITE, "old_data", "old@mine.com");
        when(siteMapRepo.findBySiteIgnoreCase(SITE)).thenReturn(Optional.of(existing));
        when(siteMapRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SiteMap saved = controller.uploadSiteMap(supervisor(), Map.of("imageData", "new_data"));

        assertThat(saved.getImageData()).isEqualTo("new_data");
        assertThat(saved.getUploadedBy()).isEqualTo("sup@mine.com");
    }

    @Test
    void getSiteMap_throws404_whenNoMapForSite() {
        AuthenticatedUser user = new AuthenticatedUser(1L, "Worker", "w@mine.com", "worker", SITE, null);
        when(siteMapRepo.findBySiteIgnoreCase(SITE)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.getSiteMap(user));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
