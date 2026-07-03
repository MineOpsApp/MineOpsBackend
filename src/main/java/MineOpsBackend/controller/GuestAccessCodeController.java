package MineOpsBackend.controller;

import MineOpsBackend.dto.CreateGuestCodeRequest;
import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.GuestAccessCode;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.GuestAccessCodeRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/admin/guest-codes")
public class GuestAccessCodeController {

    private final GuestAccessCodeRepository codeRepo;
    private final AppUserRepository userRepo;
    private final AuditLogService auditLogService;

    public GuestAccessCodeController(
        GuestAccessCodeRepository codeRepo,
        AppUserRepository userRepo,
        AuditLogService auditLogService
    ) {
        this.codeRepo = codeRepo;
        this.userRepo = userRepo;
        this.auditLogService = auditLogService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public GuestAccessCode create(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody CreateGuestCodeRequest req
    ) {
        LocalDateTime expiresAt;
        try {
            expiresAt = LocalDateTime.parse(req.expiresAt());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid expiresAt format — use YYYY-MM-DDTHH:mm");
        }
        if (!expiresAt.isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expiry must be in the future");
        }

        String code = generateUniqueCode();
        GuestAccessCode entity = new GuestAccessCode(
            user.assignedSite(), req.guestSubRole(), code,
            req.sessionHours(), req.maxRedemptions(),
            user.email(), expiresAt
        );
        GuestAccessCode saved = codeRepo.save(entity);

        auditLogService.record("GUEST_CODE_CREATED", user.role(), user.fullName(), user.email(),
            "GuestAccessCode", saved.getId(),
            req.guestSubRole() + " max=" + req.maxRedemptions() + " site=" + user.assignedSite());
        return saved;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public List<GuestAccessCode> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return codeRepo.findBySiteIgnoreCaseOrderByCreatedAtDesc(user.assignedSite());
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public GuestAccessCode revoke(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id
    ) {
        GuestAccessCode code = findAndCheckSite(id, user.assignedSite());
        code.setActive(false);
        codeRepo.save(code);

        auditLogService.record("GUEST_CODE_REVOKED", user.role(), user.fullName(), user.email(),
            "GuestAccessCode", id, code.getCode() + " site=" + code.getSite());
        return code;
    }

    @GetMapping("/{id}/roster")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public List<Map<String, Object>> roster(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id
    ) {
        findAndCheckSite(id, user.assignedSite());

        List<AppUser> guests = userRepo.findByRedeemedCodeId(id);
        List<Map<String, Object>> result = new ArrayList<>();
        for (AppUser g : guests) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", g.getId());
            entry.put("fullName", g.getFullName());
            entry.put("phone", g.getPhone());
            entry.put("joinedAt", g.getCreatedAt());
            entry.put("inductionCompleted", g.getInductionCompletedAt() != null);
            entry.put("sessionExpired", g.isExpired());
            result.add(entry);
        }
        return result;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private GuestAccessCode findAndCheckSite(Long id, String site) {
        GuestAccessCode code = codeRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Guest code not found"));
        if (!code.getSite().equalsIgnoreCase(site))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Guest code belongs to a different site");
        return code;
    }

    private String generateUniqueCode() {
        for (int i = 0; i < 20; i++) {
            String candidate = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
            if (codeRepo.findByCode(candidate).isEmpty()) return candidate;
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate a unique code");
    }
}
