package MineOpsBackend.controller;

import MineOpsBackend.dto.LoginRequest;
import MineOpsBackend.dto.RegisterRequest;
import MineOpsBackend.model.AppUser;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;

@RestController
public class AuthController {

    private static final Set<String> ALLOWED_ROLES = Set.of(
        "worker",
        "guest"
    );

    private final AppUserRepository appUserRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService;

    public AuthController(AppUserRepository appUserRepository, JwtService jwtService) {
        this.appUserRepository = appUserRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/api/auth/register")
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (!ALLOWED_ROLES.contains(request.role())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
        }

        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        String assignedSite = (request.assignedSite() == null || request.assignedSite().isBlank())
            ? "Obuasi Mine"
            : request.assignedSite();

        AppUser user = appUserRepository.save(new AppUser(
            request.fullName().trim(),
            email,
            passwordEncoder.encode(request.password()),
            request.role(),
            assignedSite
        ));

        if ("guest".equals(request.role()) && request.guestSubRole() != null) {
    user.setGuestSubRole(request.guestSubRole());
    appUserRepository.save(user);
}

        return authResponse(user);
    }

    @PostMapping("/api/auth/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
System.out.println("Current UTC time: " + java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
System.out.println("Session expires at: " + user.getSessionExpiresAt());
System.out.println("Is expired: " + user.isExpired());
        if (user.isExpired()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Guest session has expired. Contact your site administrator to renew access.");
        }

        return authResponse(user);
    }
    

    private Map<String, Object> authResponse(AppUser user) {
    Map<String, Object> userMap = new LinkedHashMap<>();
    userMap.put("id", user.getId());
    userMap.put("fullName", user.getFullName());
    userMap.put("email", user.getEmail());
    userMap.put("role", user.getRole());
    userMap.put("assignedSite", user.getAssignedSite());
    userMap.put("guestSubRole", user.getGuestSubRole());
    return Map.of(
        "token", jwtService.createToken(user),
        "user", userMap
    );
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
}