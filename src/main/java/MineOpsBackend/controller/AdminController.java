package MineOpsBackend.controller;

import MineOpsBackend.dto.CreateGuestRequest;
import MineOpsBackend.dto.RenewGuestSessionRequest;
import MineOpsBackend.model.AppUser;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class AdminController {

    private final AppUserRepository appUserRepository;
    private final AuditLogService auditLogService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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

    @PostMapping("/api/admin/guests/create")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public Map<String, Object> createGuestAccount(
        @AuthenticationPrincipal AuthenticatedUser admin,
        @Valid @RequestBody CreateGuestRequest request
    ) {
        String email = request.email().trim().toLowerCase();
        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        AppUser guest = new AppUser(
            request.fullName().trim(),
            email,
            passwordEncoder.encode(request.password()),
            "guest",
            request.assignedSite()
        );
        guest.setGuestSubRole(request.guestSubRole());
        guest.setSessionExpiresAt(LocalDateTime.now().plusHours(
            request.sessionHours() != null ? request.sessionHours() : 24
        ));
        appUserRepository.save(guest);

        auditLogService.record(
            "GUEST_ACCOUNT_CREATED",
            admin.role(),
            admin.fullName(),
            admin.email(),
            "AppUser",
            guest.getId(),
            email + " as " + request.guestSubRole()
        );

        return Map.of(
            "id", guest.getId(),
            "email", guest.getEmail(),
            "fullName", guest.getFullName(),
            "guestSubRole", guest.getGuestSubRole(),
            "sessionExpiresAt", guest.getSessionExpiresAt().toString()
        );
    }

    @GetMapping("/api/admin/guests")
@PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
public List<Map<String, Object>> getGuests(@AuthenticationPrincipal AuthenticatedUser admin) {
    return appUserRepository.findByAssignedSiteIgnoreCase(admin.assignedSite())
        .stream()
        .filter(u -> "guest".equals(u.getRole()))
        .map(u -> {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("fullName", u.getFullName());
            map.put("email", u.getEmail());
            map.put("guestSubRole", u.getGuestSubRole());
            map.put("sessionExpiresAt", u.getSessionExpiresAt() != null ? u.getSessionExpiresAt().toString() : null);
            map.put("expired", u.isExpired());
            return map;
        })
        .collect(java.util.stream.Collectors.toList());
}
@PostMapping("/api/admin/users/reset-password")
@PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
public Map<String, Object> resetPassword(
    @AuthenticationPrincipal AuthenticatedUser admin,
    @RequestBody Map<String, String> body
) {
    String email = body.get("email");
    if (email == null || email.isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
    }

    AppUser user = appUserRepository.findByEmailIgnoreCase(email.trim())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No account found with that email"));

    if ("supervisor".equals(user.getRole()) || "safetyOfficer".equals(user.getRole())) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot reset passwords for supervisors or safety officers");
    }
    if (user.getAssignedSite() == null || !user.getAssignedSite().equalsIgnoreCase(admin.assignedSite()))
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User belongs to a different site");

    String tempPassword = "Reset" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase() + "!";
    user.setPasswordHash(passwordEncoder.encode(tempPassword));
    appUserRepository.save(user);

    auditLogService.record("PASSWORD_RESET", admin.role(), admin.fullName(), admin.email(),
        "AppUser", user.getId(), email.trim());

    return Map.of(
        "email", user.getEmail(),
        "fullName", user.getFullName(),
        "temporaryPassword", tempPassword
    );
}

@PostMapping("/api/admin/users/suspend")
@PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
public Map<String, Object> suspendUser(
    @AuthenticationPrincipal AuthenticatedUser admin,
    @RequestBody Map<String, String> body
) {
    String email = body.get("email");
    AppUser user = appUserRepository.findByEmailIgnoreCase(email.trim())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No account found with that email"));

    if ("supervisor".equals(user.getRole()) || "safetyOfficer".equals(user.getRole())) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot suspend supervisors or safety officers");
    }
    if (user.getAssignedSite() == null || !user.getAssignedSite().equalsIgnoreCase(admin.assignedSite()))
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User belongs to a different site");

    boolean suspend = !"false".equals(body.get("suspend")) ;
    user.setActive(!suspend);
    appUserRepository.save(user);

    auditLogService.record(suspend ? "ACCOUNT_SUSPENDED" : "ACCOUNT_REINSTATED",
        admin.role(), admin.fullName(), admin.email(),
        "AppUser", user.getId(), email.trim());

    return Map.of(
        "email", user.getEmail(),
        "fullName", user.getFullName(),
        "active", user.getActive()
    );
}

@GetMapping("/api/admin/workers/pending")
@PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
public List<Map<String, Object>> getPendingWorkers(@AuthenticationPrincipal AuthenticatedUser admin) {
    return appUserRepository.findByRoleAndPendingAndAssignedSiteIgnoreCase("worker", true, admin.assignedSite())
        .stream()
        .map(u -> {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("fullName", u.getFullName());
            map.put("email", u.getEmail());
            map.put("assignedSite", u.getAssignedSite());
            map.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : null);
            return map;
        })
        .collect(java.util.stream.Collectors.toList());
}

@PostMapping("/api/admin/workers/approve")
@PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
public Map<String, Object> approveWorker(
    @AuthenticationPrincipal AuthenticatedUser admin,
    @RequestBody Map<String, String> body
) {
    String email = body.get("email");
    if (email == null || email.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email required");
    AppUser user = appUserRepository.findByEmailIgnoreCase(email.trim())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No account found"));
    user.setPending(false);
    appUserRepository.save(user);
    auditLogService.record("WORKER_APPROVED", admin.role(), admin.fullName(), admin.email(),
        "AppUser", user.getId(), email.trim());
    return Map.of("email", user.getEmail(), "fullName", user.getFullName(), "approved", true);
}

@PostMapping("/api/admin/workers/reject")
@PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
public Map<String, Object> rejectWorker(
    @AuthenticationPrincipal AuthenticatedUser admin,
    @RequestBody Map<String, String> body
) {
    String email = body.get("email");
    if (email == null || email.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email required");
    AppUser user = appUserRepository.findByEmailIgnoreCase(email.trim())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No account found"));
    if (!Boolean.TRUE.equals(user.getPending())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account is not pending approval");
    }
    auditLogService.record("WORKER_REJECTED", admin.role(), admin.fullName(), admin.email(),
        "AppUser", user.getId(), email.trim());
    appUserRepository.delete(user);
    return Map.of("email", email.trim(), "rejected", true);
}

@GetMapping("/api/admin/buyers/pending")
@PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
public List<Map<String, Object>> getPendingBuyers() {
    return appUserRepository.findByRoleAndPending("buyer", true)
        .stream()
        .map(u -> {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("fullName", u.getFullName());
            map.put("email", u.getEmail());
            map.put("businessName", u.getBusinessName());
            map.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : null);
            return map;
        })
        .collect(java.util.stream.Collectors.toList());
}

@PostMapping("/api/admin/buyers/approve")
@PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
public Map<String, Object> approveBuyer(
    @AuthenticationPrincipal AuthenticatedUser admin,
    @RequestBody Map<String, String> body
) {
    String email = body.get("email");
    if (email == null || email.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email required");
    AppUser user = appUserRepository.findByEmailIgnoreCase(email.trim())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No account found"));
    user.setBuyerVerificationStatus("VERIFIED");
    user.setPending(false);
    appUserRepository.save(user);
    auditLogService.record("BUYER_VERIFIED", admin.role(), admin.fullName(), admin.email(),
        "AppUser", user.getId(), email.trim());
    return Map.of("email", user.getEmail(), "fullName", user.getFullName(), "approved", true);
}

@PostMapping("/api/admin/buyers/reject")
@PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
public Map<String, Object> rejectBuyer(
    @AuthenticationPrincipal AuthenticatedUser admin,
    @RequestBody Map<String, String> body
) {
    String email = body.get("email");
    if (email == null || email.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email required");
    AppUser user = appUserRepository.findByEmailIgnoreCase(email.trim())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No account found"));
    if (!Boolean.TRUE.equals(user.getPending())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account is not pending");
    }
    auditLogService.record("BUYER_REJECTED", admin.role(), admin.fullName(), admin.email(),
        "AppUser", user.getId(), email.trim());
    appUserRepository.delete(user);
    return Map.of("email", email.trim(), "rejected", true);
}

}