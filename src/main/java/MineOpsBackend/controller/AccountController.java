package MineOpsBackend.controller;

import MineOpsBackend.dto.DeleteAccountRequest;
import MineOpsBackend.model.AppUser;
import MineOpsBackend.repository.*;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import MineOpsBackend.service.RefreshTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
public class AccountController {

    private final AppUserRepository userRepo;
    private final HazardReportRepository hazardRepo;
    private final IncidentReportRepository incidentRepo;
    private final SosAlertRepository sosRepo;
    private final SafetyChecklistRepository checklistRepo;
    private final DrillOperationRepository drillRepo;
    private final ShiftLogRepository shiftLogRepo;
    private final AttendanceRepository attendanceRepo;
    private final CertificationRepository certRepo;
    private final EmergencyContactRepository ecRepo;
    private final IllegalMineReportRepository illegalReportRepo;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogService auditLogService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AccountController(
        AppUserRepository userRepo,
        HazardReportRepository hazardRepo,
        IncidentReportRepository incidentRepo,
        SosAlertRepository sosRepo,
        SafetyChecklistRepository checklistRepo,
        DrillOperationRepository drillRepo,
        ShiftLogRepository shiftLogRepo,
        AttendanceRepository attendanceRepo,
        CertificationRepository certRepo,
        EmergencyContactRepository ecRepo,
        IllegalMineReportRepository illegalReportRepo,
        RefreshTokenService refreshTokenService,
        AuditLogService auditLogService
    ) {
        this.userRepo = userRepo;
        this.hazardRepo = hazardRepo;
        this.incidentRepo = incidentRepo;
        this.sosRepo = sosRepo;
        this.checklistRepo = checklistRepo;
        this.drillRepo = drillRepo;
        this.shiftLogRepo = shiftLogRepo;
        this.attendanceRepo = attendanceRepo;
        this.certRepo = certRepo;
        this.ecRepo = ecRepo;
        this.illegalReportRepo = illegalReportRepo;
        this.refreshTokenService = refreshTokenService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/api/account/export")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> exportMyData(@AuthenticationPrincipal AuthenticatedUser user) {
        AppUser appUser = userRepo.findById(user.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("fullName", appUser.getFullName());
        profile.put("email", appUser.getEmail());
        profile.put("role", appUser.getRole());
        profile.put("assignedSite", appUser.getAssignedSite());
        profile.put("phone", appUser.getPhone());
        profile.put("bio", appUser.getBio());
        profile.put("momoNumber", appUser.getMomoNumber());
        profile.put("momoNetwork", appUser.getMomoNetwork());
        profile.put("businessName", appUser.getBusinessName());
        profile.put("goldbodLicenseNumber", appUser.getGoldbodLicenseNumber());
        profile.put("insuranceStatus", appUser.getInsuranceStatus());
        profile.put("createdAt", appUser.getCreatedAt());
        profile.put("termsAcceptedAt", appUser.getTermsAcceptedAt());
        profile.put("notifyHazard", appUser.getNotifyHazard());
        profile.put("notifyNotice", appUser.getNotifyNotice());

        Map<String, Object> export = new LinkedHashMap<>();
        export.put("exportedAt", LocalDateTime.now().toString());
        export.put("profile", profile);
        export.put("hazardReports", hazardRepo.findByReportedByEmailIgnoreCaseOrderByCreatedAtDesc(appUser.getEmail()));
        export.put("incidentReports", incidentRepo.findByReportedByEmailIgnoreCaseOrderByReportedAtDesc(appUser.getEmail()));
        export.put("sosAlerts", sosRepo.findByActorEmailIgnoreCaseOrderByCreatedAtDesc(appUser.getEmail()));
        export.put("safetyChecklists", checklistRepo.findByWorkerEmailIgnoreCaseOrderByShiftDateDesc(appUser.getEmail()));
        export.put("drillOperations", drillRepo.findByWorkerEmailIgnoreCaseOrderByStartedAtDesc(appUser.getEmail()));
        export.put("shiftLogs", shiftLogRepo.findByWorkerEmailIgnoreCaseOrderBySubmittedAtDesc(appUser.getEmail()));
        export.put("attendanceRecords", attendanceRepo.findByWorkerEmailIgnoreCaseOrderByClockInAtDesc(appUser.getEmail()));
        export.put("certifications", certRepo.findByWorkerEmailIgnoreCaseOrderByExpiryDateAsc(appUser.getEmail()));
        export.put("emergencyContacts", ecRepo.findByWorkerIdOrderByContactTypeAsc(appUser.getId()));
        export.put("illegalMineReports", illegalReportRepo.findByReporterEmailIgnoreCaseOrderByCreatedAtDesc(appUser.getEmail()));
        return export;
    }

    @PostMapping("/api/account/delete")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> deleteAccount(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody DeleteAccountRequest request
    ) {
        AppUser appUser = userRepo.findById(user.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(request.password(), appUser.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect password");
        }

        auditLogService.record("ACCOUNT_DELETED", appUser.getRole(), appUser.getFullName(), appUser.getEmail(),
            "AppUser", appUser.getId(), "Self-service account deletion");

        refreshTokenService.revokeAll(appUser.getId());
        ecRepo.deleteByWorkerId(appUser.getId());

        appUser.setFullName("Deleted User");
        appUser.setEmail("deleted-" + appUser.getId() + "@mineops.invalid");
        appUser.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        appUser.setActive(false);
        appUser.setDeletedAt(LocalDateTime.now());
        appUser.setProfilePhoto(null);
        appUser.setBio(null);
        appUser.setPhone(null);
        appUser.setMomoNumber(null);
        appUser.setMomoNetwork(null);
        appUser.setPushToken(null);
        appUser.setVerificationDocument(null);
        appUser.setBusinessName(null);
        appUser.setGoldbodLicenseNumber(null);
        userRepo.save(appUser);

        return Map.of("success", true);
    }
}
