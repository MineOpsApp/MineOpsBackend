package MineOpsBackend.controller;

import MineOpsBackend.model.SiteMap;
import MineOpsBackend.repository.SiteMapRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/site-map")
public class SiteMapController {

    private final SiteMapRepository siteMapRepository;
    private final AuditLogService auditLogService;

    public SiteMapController(SiteMapRepository siteMapRepository, AuditLogService auditLogService) {
        this.siteMapRepository = siteMapRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public SiteMap getSiteMap(@AuthenticationPrincipal AuthenticatedUser user) {
        return siteMapRepository.findBySiteIgnoreCase(user.assignedSite())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No site map uploaded yet"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public SiteMap uploadSiteMap(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody Map<String, String> body
    ) {
        String imageData = body.get("imageData");
        if (imageData == null || imageData.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "imageData is required");
        }
        if (imageData.length() > 2_000_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image is too large. Use a smaller image.");
        }

        SiteMap map = siteMapRepository.findBySiteIgnoreCase(user.assignedSite())
            .orElse(new SiteMap(user.assignedSite(), imageData, user.email()));

        map.setImageData(imageData);
        map.setUploadedBy(user.email());
        map.setUploadedAt(java.time.LocalDateTime.now());

        SiteMap saved = siteMapRepository.save(map);
        auditLogService.record(
            "SITE_MAP_UPLOADED",
            user.role(),
            user.fullName(),
            user.email(),
            "SiteMap",
            saved.getId(),
            user.assignedSite()
        );
        return saved;
    }
}
