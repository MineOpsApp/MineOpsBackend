package MineOpsBackend.controller;

import MineOpsBackend.dto.CertificationRequest;
import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.Certification;
import MineOpsBackend.model.CertificationHistory;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.CertificationHistoryRepository;
import MineOpsBackend.repository.CertificationRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class CertificationController {

    private final CertificationRepository certRepo;
    private final CertificationHistoryRepository historyRepo;
    private final AppUserRepository userRepo;
    private final AuditLogService auditLogService;

    public CertificationController(
        CertificationRepository certRepo,
        CertificationHistoryRepository historyRepo,
        AppUserRepository userRepo,
        AuditLogService auditLogService
    ) {
        this.certRepo = certRepo;
        this.historyRepo = historyRepo;
        this.userRepo = userRepo;
        this.auditLogService = auditLogService;
    }

    // Supervisor: all site certifications, optional filters
    @GetMapping("/api/certifications")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public List<Certification> getSiteCertifications(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String type
    ) {
        List<Certification> certs = certRepo.findBySiteIgnoreCaseOrderByExpiryDateAsc(user.assignedSite());

        if (status != null && !status.isBlank()) {
            final String s = status.toUpperCase();
            certs = certs.stream().filter(c -> s.equals(c.getStatus())).collect(Collectors.toList());
        }
        if (type != null && !type.isBlank()) {
            final String t = type.toLowerCase();
            certs = certs.stream()
                .filter(c -> c.getCertificationName().toLowerCase().contains(t))
                .collect(Collectors.toList());
        }
        return certs;
    }

    // Worker: own certifications
    @GetMapping("/api/certifications/mine")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public List<Certification> getMyCertifications(@AuthenticationPrincipal AuthenticatedUser user) {
        return certRepo.findByWorkerEmailIgnoreCaseOrderByExpiryDateAsc(user.email());
    }

    // Supervisor: renewal history for a specific certification
    @GetMapping("/api/certifications/{id}/history")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER','ROLE_WORKER')")
    public List<CertificationHistory> getCertHistory(
        @PathVariable Long id,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Certification cert = certRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certification not found"));

        // Workers can only see their own history
        if ("ROLE_WORKER".equals(user.role()) &&
                !cert.getWorkerEmail().equalsIgnoreCase(user.email())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return historyRepo.findByCertificationIdOrderByRenewedAtDesc(id);
    }

    // Supervisor: add certification for a site worker
    @PostMapping("/api/certifications")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public Certification addCertification(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody CertificationRequest req
    ) {
        if (req.workerId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "workerId is required");

        AppUser worker = userRepo.findById(req.workerId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker not found"));

        if (!user.assignedSite().equalsIgnoreCase(worker.getAssignedSite()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Worker is not on your site");

        LocalDate issueDate = parseDate(req.issueDate());
        LocalDate expiryDate = parseDate(req.expiryDate());

        if (!expiryDate.isAfter(issueDate))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expiry date must be after issue date");

        Certification cert = new Certification(
            worker.getId(), worker.getFullName(), worker.getEmail(),
            user.assignedSite(),
            req.certificationName(), req.issuingAuthority(),
            issueDate, expiryDate,
            req.notes(), user.fullName()
        );
        Certification saved = certRepo.save(cert);

        auditLogService.record(
            "CERTIFICATION_ADDED",
            user.role(), user.fullName(), user.email(),
            "Certification", saved.getId(),
            worker.getFullName() + " — " + req.certificationName() + " expires " + req.expiryDate()
        );
        return saved;
    }

    // Supervisor: update / renew a certification
    @PutMapping("/api/certifications/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public Certification updateCertification(
        @PathVariable Long id,
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody CertificationRequest req
    ) {
        Certification cert = certRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certification not found"));

        if (!cert.getSite().equalsIgnoreCase(user.assignedSite()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Certification belongs to a different site");

        LocalDate newExpiry = parseDate(req.expiryDate());
        LocalDate newIssue = parseDate(req.issueDate());

        if (!newExpiry.isAfter(newIssue))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expiry date must be after issue date");

        boolean expiryChanged = !newExpiry.equals(cert.getExpiryDate());
        boolean authorityChanged = !req.issuingAuthority().equals(cert.getIssuingAuthority());

        if (expiryChanged || authorityChanged) {
            historyRepo.save(new CertificationHistory(
                cert.getId(),
                cert.getExpiryDate(), newExpiry,
                cert.getIssuingAuthority(), req.issuingAuthority(),
                user.fullName(),
                req.notes()
            ));
        }

        cert.setCertificationName(req.certificationName());
        cert.setIssuingAuthority(req.issuingAuthority());
        cert.setIssueDate(newIssue);
        cert.setExpiryDate(newExpiry);
        cert.setNotes(req.notes());
        cert.setUpdatedAt(LocalDateTime.now());
        Certification saved = certRepo.save(cert);

        auditLogService.record(
            "CERTIFICATION_UPDATED",
            user.role(), user.fullName(), user.email(),
            "Certification", id,
            cert.getWorkerName() + " — " + req.certificationName() + " → expires " + req.expiryDate()
        );
        return saved;
    }

    // Supervisor: delete a certification
    @DeleteMapping("/api/certifications/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public void deleteCertification(
        @PathVariable Long id,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Certification cert = certRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certification not found"));

        if (!cert.getSite().equalsIgnoreCase(user.assignedSite()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Certification belongs to a different site");

        certRepo.delete(cert);

        auditLogService.record(
            "CERTIFICATION_DELETED",
            user.role(), user.fullName(), user.email(),
            "Certification", id,
            cert.getWorkerName() + " — " + cert.getCertificationName()
        );
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format, expected YYYY-MM-DD: " + dateStr);
        }
    }
}
