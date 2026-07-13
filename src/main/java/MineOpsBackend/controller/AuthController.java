package MineOpsBackend.controller;

import MineOpsBackend.dto.LoginRequest;
import MineOpsBackend.dto.RegisterRequest;
import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.RefreshToken;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.JwtService;
import MineOpsBackend.service.RefreshTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class AuthController {

    private static final Set<String> ALLOWED_ROLES = Set.of("worker", "guest", "buyer");

    private final AppUserRepository appUserRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(
        AppUserRepository appUserRepository,
        JwtService jwtService,
        RefreshTokenService refreshTokenService
    ) {
        this.appUserRepository = appUserRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/api/auth/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (!ALLOWED_ROLES.contains(request.role())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
        }
        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        if ("buyer".equals(request.role())) {
            AppUser buyer = new AppUser(
                request.fullName().trim(), email,
                passwordEncoder.encode(request.password()),
                "buyer", null
            );
            buyer.setBuyerVerificationStatus("PENDING_VERIFICATION");
            buyer.setPending(true);
            if (request.businessName() != null) buyer.setBusinessName(request.businessName().trim());
            if (request.verificationDocument() != null) buyer.setVerificationDocument(request.verificationDocument());
            if (request.goldbodLicenseNumber() != null && !request.goldbodLicenseNumber().isBlank())
                buyer.setGoldbodLicenseNumber(request.goldbodLicenseNumber().trim());
            appUserRepository.save(buyer);
            return ResponseEntity.ok(Map.of("pending", true));
        }

        String assignedSite = (request.assignedSite() == null || request.assignedSite().isBlank())
            ? "Obuasi Mine"
            : request.assignedSite();

        AppUser user = appUserRepository.save(new AppUser(
            request.fullName().trim(), email,
            passwordEncoder.encode(request.password()),
            request.role(), assignedSite
        ));

        if ("guest".equals(request.role()) && request.guestSubRole() != null) {
            user.setGuestSubRole(request.guestSubRole());
            appUserRepository.save(user);
        }

        if ("worker".equals(request.role())) {
            user.setPending(true);
            appUserRepository.save(user);
            return ResponseEntity.ok(Map.of("pending", true));
        }

        return ResponseEntity.ok(authResponse(user, request.deviceName(), request.platform()));
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        AppUser user = appUserRepository.findByEmailIgnoreCase(email).orElse(null);

        if (user != null && user.getLockedUntil() != null && LocalDateTime.now().isBefore(user.getLockedUntil())) {
            return ResponseEntity.status(200).body(Map.of("error", "LOCKED"));
        }

        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            if (user != null) {
                int attempts = (user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts()) + 1;
                if (attempts >= 5) {
                    user.setFailedLoginAttempts(0);
                    user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
                    appUserRepository.save(user);
                    return ResponseEntity.status(200).body(Map.of("error", "LOCKED"));
                }
                user.setFailedLoginAttempts(attempts);
                appUserRepository.save(user);
            }
            return ResponseEntity.status(401).body(Map.of("error", "INVALID_CREDENTIALS"));
        }

        if ((user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() > 0) || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            appUserRepository.save(user);
        }

        if (Boolean.TRUE.equals(user.getPending())) {
            return ResponseEntity.status(200).body(Map.of("error", "PENDING_APPROVAL"));
        }
        if (Boolean.FALSE.equals(user.getActive())) {
            return ResponseEntity.status(200).body(Map.of("error", "SUSPENDED"));
        }
        if (user.isExpired()) {
            return ResponseEntity.status(200).body(Map.of("error", "EXPIRED"));
        }

        return ResponseEntity.ok(authResponse(user, request.deviceName(), request.platform()));
    }

    @PostMapping("/api/auth/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@RequestBody Map<String, String> body) {
        String raw = body == null ? null : body.get("refreshToken");
        if (raw == null || raw.isBlank()) {
            return ResponseEntity.status(400).body(Map.of("error", "MISSING_REFRESH_TOKEN"));
        }
        try {
            RefreshToken existing = refreshTokenService.validate(raw);
            AppUser user = appUserRepository.findById(existing.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            if (Boolean.FALSE.equals(user.getActive())) {
                return ResponseEntity.status(200).body(Map.of("error", "SUSPENDED"));
            }
            if (user.isExpired()) {
                return ResponseEntity.status(200).body(Map.of("error", "EXPIRED"));
            }

            String newRaw = refreshTokenService.rotate(existing);
            return ResponseEntity.ok(authResponseWithToken(user, newRaw));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(401).body(Map.of("error", "INVALID_REFRESH_TOKEN", "message", e.getReason() != null ? e.getReason() : "Token invalid or expired"));
        }
    }

    @PostMapping("/api/auth/logout")
    public Map<String, Object> logout(@RequestBody(required = false) Map<String, String> body) {
        if (body != null) {
            String raw = body.get("refreshToken");
            if (raw != null && !raw.isBlank()) {
                try {
                    RefreshToken token = refreshTokenService.validate(raw);
                    refreshTokenService.revokeAll(token.getUserId());
                } catch (Exception ignored) {
                    // Already revoked or expired — logout is still successful
                }
            }
        }
        return Map.of("success", true);
    }

    @PostMapping("/api/auth/push-token")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> savePushToken(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody Map<String, String> body
    ) {
        AppUser appUser = appUserRepository.findByEmailIgnoreCase(user.email())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        appUser.setPushToken(body.get("token"));
        appUserRepository.save(appUser);
        return Map.of("success", true);
    }

    @GetMapping("/api/auth/sessions")
    @PreAuthorize("isAuthenticated()")
    public List<Map<String, Object>> listSessions(@AuthenticationPrincipal AuthenticatedUser user) {
        return refreshTokenService.listActiveSessions(user.id()).stream()
            .map(t -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", t.getId());
                m.put("deviceName", t.getDeviceName() != null ? t.getDeviceName() : "Unknown device");
                m.put("platform", t.getPlatform());
                m.put("createdAt", t.getCreatedAt().toString());
                m.put("lastUsedAt", t.getLastUsedAt() != null ? t.getLastUsedAt().toString() : t.getCreatedAt().toString());
                return m;
            })
            .collect(Collectors.toList());
    }

    @PostMapping("/api/auth/sessions/{id}/revoke")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> revokeSession(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        refreshTokenService.revokeOne(user.id(), id);
        return Map.of("success", true);
    }

    private Map<String, Object> authResponse(AppUser user, String deviceName, String platform) {
        String rawRefreshToken = refreshTokenService.generate(user.getId(), deviceName, platform);
        return authResponseWithToken(user, rawRefreshToken);
    }

    private Map<String, Object> authResponseWithToken(AppUser user, String rawRefreshToken) {
        Map<String, Object> userMap = new LinkedHashMap<>();
        userMap.put("id", user.getId());
        userMap.put("fullName", user.getFullName());
        userMap.put("email", user.getEmail());
        userMap.put("role", user.getRole());
        userMap.put("assignedSite", user.getAssignedSite());
        userMap.put("guestSubRole", user.getGuestSubRole());
        userMap.put("goldbodLicenseNumber", user.getGoldbodLicenseNumber());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", jwtService.createToken(user));
        result.put("refreshToken", rawRefreshToken);
        result.put("user", userMap);
        return result;
    }
}
