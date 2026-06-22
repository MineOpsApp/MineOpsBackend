package MineOpsBackend.controller;

import MineOpsBackend.dto.RenewGuestSessionRequest;
import MineOpsBackend.model.AppUser;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class AdminController {

    private final AppUserRepository appUserRepository;
    private final AuditLogService auditLogService;

    public AdminController(AppUserRepository appUserRepository, AuditLogService auditLogService) {
        this.appUserRepository = appUserRepository;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/api/admin/guests/renew")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public Map<String, Object> renewGuestSession(
        @AuthenticationPrincipal AuthenticatedUser admin,
        @Valid @RequestBody RenewGuestSessionRequest request
    ) {
        AppUser guest = appUserRepository.findByEmailIgnoreCase(request.email().trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No account found with that email"));

        if (!"guest".equals(guest.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account is not a guest — only guest sessions can be renewed");
        }

        int hours = (request.hours() != null && request.hours() > 0) ? request.hours() : 24;
        LocalDateTime newExpiry = LocalDateTime.now().plusHours(hours);
        guest.setSessionExpiresAt(newExpiry);
        appUserRepository.save(guest);

        auditLogService.record(
            "GUEST_SESSION_RENEWED",
            admin.role(),
            admin.fullName(),
            admin.email(),
            "AppUser",
            guest.getId(),
            guest.getEmail() + " extended by " + hours + "h"
        );

        return Map.of(
            "email", guest.getEmail(),
            "fullName", guest.getFullName(),
            "sessionExpiresAt", newExpiry.toString(),
            "hoursGranted", hours
        );
    }
}