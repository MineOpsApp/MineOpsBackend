package MineOpsBackend.controller;

import MineOpsBackend.model.AppUser;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

@RestController
public class AuthController {

    private static final Set<String> ALLOWED_ROLES = Set.of(
        "worker",
        "supervisor",
        "safetyOfficer",
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
    public Map<String, Object> register(@RequestBody Map<String, String> request) {
        String fullName = required(request, "fullName");
        String email = required(request, "email").trim().toLowerCase();
        String password = required(request, "password");
        String role = required(request, "role");

        if (!ALLOWED_ROLES.contains(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
        }

        if (password.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters");
        }

        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        AppUser user = appUserRepository.save(new AppUser(
            fullName,
            email,
            passwordEncoder.encode(password),
            role
        ));

        return authResponse(user);
    }

    @PostMapping("/api/auth/login")
    public Map<String, Object> login(@RequestBody Map<String, String> request) {
        String email = required(request, "email").trim().toLowerCase();
        String password = required(request, "password");
        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        return authResponse(user);
    }

    private Map<String, Object> authResponse(AppUser user) {
        return Map.of(
            "token", jwtService.createToken(user),
            "user", Map.of(
                "id", user.getId(),
                "fullName", user.getFullName(),
                "email", user.getEmail(),
                "role", user.getRole()
            )
        );
    }

    private String required(Map<String, String> request, String key) {
        String value = request.get(key);

        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, key + " is required");
        }

        return value.trim();
    }
}
