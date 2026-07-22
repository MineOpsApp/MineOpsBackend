package MineOpsBackend.controller;

import MineOpsBackend.dto.LoginRequest;
import MineOpsBackend.dto.RegisterRequest;
import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.PasswordResetOtp;
import MineOpsBackend.model.RefreshToken;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.PasswordResetOtpRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.EmailService;
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

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class AuthController {

    private static final Set<String> ALLOWED_ROLES = Set.of("worker", "guest", "buyer");
    private static final SecureRandom OTP_RANDOM = new SecureRandom();
    private static final int OTP_VALIDITY_MINUTES = 15;
    private static final int OTP_RESEND_COOLDOWN_SECONDS = 60;
    private static final int OTP_MAX_REQUESTS_PER_DAY = 5;

    private final AppUserRepository appUserRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;
    private final PasswordResetOtpRepository passwordResetOtpRepository;

    public AuthController(
        AppUserRepository appUserRepository,
        JwtService jwtService,
        RefreshTokenService refreshTokenService,
        EmailService emailService,
        PasswordResetOtpRepository passwordResetOtpRepository
    ) {
        this.appUserRepository = appUserRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.emailService = emailService;
        this.passwordResetOtpRepository = passwordResetOtpRepository;
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
        if (!Boolean.TRUE.equals(request.acceptedTerms())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You must accept the Terms of Service and Privacy Policy to register");
        }

        if ("buyer".equals(request.role())) {
            AppUser buyer = new AppUser(
                request.fullName().trim(), email,
                passwordEncoder.encode(request.password()),
                "buyer", null
            );
            buyer.setBuyerVerificationStatus("PENDING_VERIFICATION");
            buyer.setPending(true);
            buyer.setTermsAcceptedAt(LocalDateTime.now());
            if (request.businessName() != null) buyer.setBusinessName(request.businessName().trim());
            if (request.verificationDocument() != null) buyer.setVerificationDocument(request.verificationDocument());
            if (request.goldbodLicenseNumber() != null && !request.goldbodLicenseNumber().isBlank())
                buyer.setGoldbodLicenseNumber(request.goldbodLicenseNumber().trim());
            appUserRepository.save(buyer);
            emailService.sendRegistrationPending(buyer.getEmail(), buyer.getFullName(), "buyer");
            return ResponseEntity.ok(Map.of("pending", true));
        }

        String assignedSite = (request.assignedSite() == null || request.assignedSite().isBlank())
            ? "Obuasi Mine"
            : request.assignedSite();

        AppUser user = new AppUser(
            request.fullName().trim(), email,
            passwordEncoder.encode(request.password()),
            request.role(), assignedSite
        );
        user.setTermsAcceptedAt(LocalDateTime.now());
        user = appUserRepository.save(user);

        if ("guest".equals(request.role()) && request.guestSubRole() != null) {
            user.setGuestSubRole(request.guestSubRole());
            appUserRepository.save(user);
        }

        if ("worker".equals(request.role())) {
            user.setPending(true);
            appUserRepository.save(user);
            emailService.sendRegistrationPending(user.getEmail(), user.getFullName(), "worker");
            return ResponseEntity.ok(Map.of("pending", true));
        }

        // Self-registered guests need a supervisor's sign-off, same as workers — this is the
        // sign-up form path, distinct from the QR/access-code redemption flow, which is already
        // pre-authorized because a supervisor generated that code in the first place.
        if ("guest".equals(request.role())) {
            user.setPending(true);
            appUserRepository.save(user);
            emailService.sendRegistrationPending(user.getEmail(), user.getFullName(), "guest");
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

            RefreshTokenService.IssuedToken issued = refreshTokenService.rotate(existing);
            return ResponseEntity.ok(authResponseWithToken(user, issued));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(401).body(Map.of("error", "INVALID_REFRESH_TOKEN", "message", e.getReason() != null ? e.getReason() : "Token invalid or expired"));
        }
    }

    @PostMapping("/api/auth/forgot-password")
    public Map<String, Object> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body == null ? null : body.get("email");
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        String normalizedEmail = email.trim().toLowerCase();

        // The response is the same generic message whether or not the account exists, so this
        // endpoint can't be used to enumerate registered emails. Rate limiting only kicks in for
        // emails that do have an account (it's checked inside this block), which means a 429 does
        // leak "this email exists" after repeated attempts — an accepted tradeoff to stop someone
        // from burning through the SMTP provider's daily send quota with one repeated address.
        AppUser user = appUserRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (user != null) {
            LocalDateTime now = LocalDateTime.now();

            passwordResetOtpRepository.findTopByEmailIgnoreCaseOrderByCreatedAtDesc(normalizedEmail)
                .filter(last -> last.getCreatedAt().isAfter(now.minusSeconds(OTP_RESEND_COOLDOWN_SECONDS)))
                .ifPresent(last -> {
                    throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Please wait a minute before requesting another code");
                });

            long requestsToday = passwordResetOtpRepository.countByEmailIgnoreCaseAndCreatedAtAfter(
                normalizedEmail, now.minusHours(24));
            if (requestsToday >= OTP_MAX_REQUESTS_PER_DAY) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many reset requests for this email today. Try again tomorrow or contact your supervisor.");
            }

            passwordResetOtpRepository.findByEmailIgnoreCaseAndUsedFalse(normalizedEmail)
                .forEach(old -> { old.setUsed(true); passwordResetOtpRepository.save(old); });

            String otp = String.format("%06d", OTP_RANDOM.nextInt(1_000_000));
            PasswordResetOtp record = new PasswordResetOtp(normalizedEmail, otp, now.plusMinutes(OTP_VALIDITY_MINUTES));
            passwordResetOtpRepository.save(record);
            emailService.sendPasswordResetOtp(user.getEmail(), user.getFullName(), otp, OTP_VALIDITY_MINUTES);
        }

        return Map.of("success", true, "message", "If an account exists for that email, a reset code has been sent.");
    }

    @PostMapping("/api/auth/reset-password")
    public Map<String, Object> resetPasswordWithOtp(@RequestBody Map<String, String> body) {
        String email = body == null ? null : body.get("email");
        String otp = body == null ? null : body.get("otp");
        String newPassword = body == null ? null : body.get("newPassword");

        if (email == null || email.isBlank() || otp == null || otp.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and code are required");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be at least 6 characters");
        }
        String normalizedEmail = email.trim().toLowerCase();

        PasswordResetOtp record = passwordResetOtpRepository
            .findTopByEmailIgnoreCaseAndOtpCodeAndUsedFalseOrderByCreatedAtDesc(normalizedEmail, otp.trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired code"));

        if (record.isExpired()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired code");
        }

        AppUser user = appUserRepository.findByEmailIgnoreCase(normalizedEmail)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired code"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setMustChangePassword(false);
        appUserRepository.save(user);

        record.setUsed(true);
        passwordResetOtpRepository.save(record);
        refreshTokenService.revokeAll(user.getId());

        return Map.of("success", true);
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

    @PostMapping("/api/auth/change-password")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> changePassword(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @RequestBody Map<String, String> body
    ) {
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");
        if (currentPassword == null || newPassword == null || newPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current and new password are required");
        }
        if (newPassword.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be at least 6 characters");
        }

        AppUser user = appUserRepository.findByEmailIgnoreCase(principal.email())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        appUserRepository.save(user);

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
        RefreshTokenService.IssuedToken issued = refreshTokenService.generate(user.getId(), deviceName, platform);
        return authResponseWithToken(user, issued);
    }

    private Map<String, Object> authResponseWithToken(AppUser user, RefreshTokenService.IssuedToken issued) {
        Map<String, Object> userMap = new LinkedHashMap<>();
        userMap.put("id", user.getId());
        userMap.put("fullName", user.getFullName());
        userMap.put("email", user.getEmail());
        userMap.put("role", user.getRole());
        userMap.put("assignedSite", user.getAssignedSite());
        userMap.put("guestSubRole", user.getGuestSubRole());
        userMap.put("goldbodLicenseNumber", user.getGoldbodLicenseNumber());
        userMap.put("mustChangePassword", Boolean.TRUE.equals(user.getMustChangePassword()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", jwtService.createToken(user, issued.sessionId()));
        result.put("refreshToken", issued.raw());
        result.put("user", userMap);
        return result;
    }
}
