package MineOpsBackend.controller;

import MineOpsBackend.dto.RedeemGuestCodeRequest;
import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.GuestAccessCode;
import MineOpsBackend.model.RefreshToken;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.GuestAccessCodeRepository;
import MineOpsBackend.service.AuditLogService;
import MineOpsBackend.service.JwtService;
import MineOpsBackend.service.RateLimitService;
import MineOpsBackend.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
public class GuestRedemptionController {

    // A 6-digit code has only 1,000,000 possible values — without this, it's brute-forceable in
    // minutes. This caps redemption attempts per client IP well below what's needed for legitimate
    // use (a guest mistyping a code a few times) while making brute-forcing the whole code space
    // impractical.
    private static final int MAX_ATTEMPTS_PER_WINDOW = 8;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(15);

    private final GuestAccessCodeRepository codeRepo;
    private final AppUserRepository userRepo;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogService auditLogService;
    private final RateLimitService rateLimitService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public GuestRedemptionController(
        GuestAccessCodeRepository codeRepo,
        AppUserRepository userRepo,
        JwtService jwtService,
        RefreshTokenService refreshTokenService,
        AuditLogService auditLogService,
        RateLimitService rateLimitService
    ) {
        this.codeRepo = codeRepo;
        this.userRepo = userRepo;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.auditLogService = auditLogService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/api/guest/redeem")
    @Transactional
    public ResponseEntity<Map<String, Object>> redeem(@Valid @RequestBody RedeemGuestCodeRequest req, HttpServletRequest httpRequest) {
        String ip = rateLimitService.clientIp(httpRequest);
        if (!rateLimitService.tryAcquire("guest-redeem:" + ip, MAX_ATTEMPTS_PER_WINDOW, RATE_LIMIT_WINDOW)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts. Please wait a while before trying again.");
        }

        // Pessimistic-write lock, held for the rest of this @Transactional method: without it, two
        // requests redeeming the same code at the same instant can both pass the maxRedemptions
        // check below before either commits its increment, letting the code be over-redeemed.
        GuestAccessCode code = codeRepo.findByCodeForUpdate(req.code().trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Code not found"));

        if (!Boolean.TRUE.equals(code.getActive()))
            throw new ResponseStatusException(HttpStatus.GONE, "This code has been revoked");
        if (!LocalDateTime.now().isBefore(code.getExpiresAt()))
            throw new ResponseStatusException(HttpStatus.GONE, "This code has expired");
        if (code.getRedemptionCount() >= code.getMaxRedemptions())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This code has reached its guest limit");

        String email = req.email().trim().toLowerCase();
        if (userRepo.existsByEmailIgnoreCase(email))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
        String rawPassword = UUID.randomUUID().toString();

        AppUser guest = new AppUser(req.fullName().trim(), email,
            passwordEncoder.encode(rawPassword), "guest", code.getSite());
        guest.setGuestSubRole(code.getGuestSubRole());
        guest.setSessionExpiresAt(LocalDateTime.now().plusHours(code.getSessionHours()));
        guest.setRedeemedCodeId(code.getId());
        guest.setPhone(req.phone().trim());
        userRepo.save(guest);

        code.setRedemptionCount(code.getRedemptionCount() + 1);
        codeRepo.save(code);

        auditLogService.record("GUEST_CODE_REDEEMED", "guest", guest.getFullName(), guest.getEmail(),
            "GuestAccessCode", code.getId(),
            code.getSite() + " sub=" + code.getGuestSubRole());

        RefreshTokenService.IssuedToken issued = refreshTokenService.generate(guest.getId(), null, null);
        return ResponseEntity.ok(buildAuthResponse(guest, issued));
    }

    private Map<String, Object> buildAuthResponse(AppUser user, RefreshTokenService.IssuedToken issued) {
        Map<String, Object> userMap = new LinkedHashMap<>();
        userMap.put("id", user.getId());
        userMap.put("fullName", user.getFullName());
        userMap.put("email", user.getEmail());
        userMap.put("role", user.getRole());
        userMap.put("assignedSite", user.getAssignedSite());
        userMap.put("guestSubRole", user.getGuestSubRole());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", jwtService.createToken(user, issued.sessionId()));
        result.put("refreshToken", issued.raw());
        result.put("user", userMap);
        return result;
    }
}
