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
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
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

        if ("worker".equals(request.role())) {
            user.setPending(true);
            appUserRepository.save(user);
            return Map.of("pending", true);
        }

        return authResponse(user);
    }

    @PostMapping("/api/auth/login")
public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
   
    String email = request.email().trim().toLowerCase();
    
    AppUser user = appUserRepository.findByEmailIgnoreCase(email).orElse(null);
    
    if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
        return ResponseEntity.status(401).body(Map.of("error", "INVALID_CREDENTIALS"));
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
    
    return ResponseEntity.ok(authResponse(user));
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
@PostMapping("/api/auth/refresh")
@PreAuthorize("isAuthenticated()")
public Map<String, Object> refresh(@AuthenticationPrincipal AuthenticatedUser principal) {
    AppUser user = appUserRepository.findByEmailIgnoreCase(principal.email())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    return Map.of("token", jwtService.createToken(user));
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