package MineOpsBackend.controller;

import MineOpsBackend.model.AppUser;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.CertificationRepository;
import MineOpsBackend.repository.EmergencyContactRepository;
import MineOpsBackend.repository.ShiftLogRepository;
import MineOpsBackend.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class ProfileController {

    private final AppUserRepository userRepo;
    private final ShiftLogRepository shiftLogRepo;
    private final CertificationRepository certRepo;
    private final EmergencyContactRepository ecRepo;

    public ProfileController(
        AppUserRepository userRepo,
        ShiftLogRepository shiftLogRepo,
        CertificationRepository certRepo,
        EmergencyContactRepository ecRepo
    ) {
        this.userRepo = userRepo;
        this.shiftLogRepo = shiftLogRepo;
        this.certRepo = certRepo;
        this.ecRepo = ecRepo;
    }

    @GetMapping("/api/profile")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> getOwnProfile(@AuthenticationPrincipal AuthenticatedUser user) {
        AppUser appUser = userRepo.findById(user.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return buildProfile(appUser);
    }

    @PutMapping("/api/profile")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> updateProfile(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody Map<String, String> body
    ) {
        AppUser appUser = userRepo.findById(user.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (body.containsKey("photo")) {
            String photo = body.get("photo");
            if (photo != null && photo.length() > 2_000_000) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo is too large. Use a smaller image.");
            }
            appUser.setProfilePhoto(photo);
        }
        if (body.containsKey("bio")) {
            String bio = body.get("bio");
            if (bio != null && bio.length() > 500) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bio must be 500 characters or less.");
            }
            appUser.setBio(bio);
        }
        if (body.containsKey("momoNumber")) {
            String num = body.get("momoNumber");
            if (num != null && !num.isBlank() && !num.matches("\\d{10,15}"))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MoMo number must be 10–15 digits.");
            appUser.setMomoNumber(num == null || num.isBlank() ? null : num);
        }
        if (body.containsKey("momoNetwork")) {
            String net = body.get("momoNetwork");
            if (net != null && !net.isBlank()
                    && !java.util.Set.of("MTN", "TELECEL", "AIRTELTIGO").contains(net.toUpperCase()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "momoNetwork must be MTN, TELECEL, or AIRTELTIGO.");
            appUser.setMomoNetwork(net == null || net.isBlank() ? null : net.toUpperCase());
        }
        userRepo.save(appUser);
        return buildProfile(appUser);
    }

    @GetMapping("/api/profile/{email}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public Map<String, Object> getWorkerProfile(
        @PathVariable String email,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        AppUser worker = userRepo.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker not found"));
        if (!user.assignedSite().equalsIgnoreCase(worker.getAssignedSite())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Worker is not on your site");
        }
        return buildProfile(worker);
    }

    private Map<String, Object> buildProfile(AppUser u) {
        long shiftCount = shiftLogRepo.countByWorkerEmailIgnoreCase(u.getEmail());
        long certCount = certRepo.countByWorkerEmailIgnoreCase(u.getEmail());
        long contactCount = ecRepo.findByWorkerIdOrderByContactTypeAsc(u.getId()).size();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("fullName", u.getFullName());
        m.put("email", u.getEmail());
        m.put("role", u.getRole());
        m.put("assignedSite", u.getAssignedSite());
        m.put("profilePhoto", u.getProfilePhoto());
        m.put("bio", u.getBio());
        m.put("createdAt", u.getCreatedAt());
        m.put("active", u.getActive());
        m.put("shiftLogCount", shiftCount);
        m.put("certificationCount", certCount);
        m.put("emergencyContactCount", contactCount);
        m.put("momoNumber", u.getMomoNumber());
        m.put("momoNetwork", u.getMomoNetwork());
        m.put("insuranceStatus", u.getInsuranceStatus() != null ? u.getInsuranceStatus() : "NOT_INSURED");
        if ("buyer".equals(u.getRole())) {
            m.put("businessName", u.getBusinessName());
            m.put("goldbodLicenseNumber", u.getGoldbodLicenseNumber());
            m.put("buyerVerificationStatus", u.getBuyerVerificationStatus());
        }
        return m;
    }
}
