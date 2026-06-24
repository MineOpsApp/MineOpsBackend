package MineOpsBackend.controller;

import MineOpsBackend.dto.LoginRequest;
import MineOpsBackend.dto.RegisterRequest;
import MineOpsBackend.model.AppUser;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
}