package MineOpsBackend.controller;

import MineOpsBackend.dto.SaveEmergencyContactRequest;
import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.EmergencyContact;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.EmergencyContactRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
public class EmergencyContactController {

    private static final Set<String> VALID_TYPES = Set.of("PRIMARY", "BACKUP");

    private final EmergencyContactRepository contactRepository;
    private final AppUserRepository appUserRepository;
    private final AuditLogService auditLogService;

    public EmergencyContactController(
        EmergencyContactRepository contactRepository,
        AppUserRepository appUserRepository,
        AuditLogService auditLogService
    ) {
        this.contactRepository = contactRepository;
        this.appUserRepository = appUserRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/api/emergency-contacts")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public List<EmergencyContact> getMyContacts(@AuthenticationPrincipal AuthenticatedUser user) {
        return contactRepository.findByWorkerIdOrderByContactTypeAsc(user.id());
    }

    @PostMapping("/api/emergency-contacts")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public EmergencyContact saveContact(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody SaveEmergencyContactRequest request
    ) {
        if (request.contactType() == null || !VALID_TYPES.contains(request.contactType().toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contactType must be PRIMARY or BACKUP");
        }
        if (isBlank(request.name())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        if (isBlank(request.relationship())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "relationship is required");
        if (isBlank(request.phone())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "phone is required");

        String normalizedPhone = normalizePhone(request.phone());
        if (!isValidPhone(normalizedPhone)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid phone number. Use Ghana format (e.g. 0244 123 456 or +233 24 123 456) or a valid international number.");
        }

        String type = request.contactType().toUpperCase();

        EmergencyContact contact = contactRepository
            .findByWorkerIdAndContactType(user.id(), type)
            .orElse(null);

        if (contact == null) {
            List<EmergencyContact> existing = contactRepository.findByWorkerIdOrderByContactTypeAsc(user.id());
            if (existing.size() >= 2) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum 2 emergency contacts allowed");
            }
            contact = new EmergencyContact(user.id(), type, request.name().trim(), request.relationship().trim(), normalizedPhone);
        } else {
            contact.setName(request.name().trim());
            contact.setRelationship(request.relationship().trim());
            contact.setPhone(normalizedPhone);
        }

        EmergencyContact saved = contactRepository.save(contact);
        auditLogService.record(
            "EMERGENCY_CONTACT_SAVED", user.role(), user.fullName(), user.email(),
            "EmergencyContact", saved.getId(), type + ": " + saved.getName()
        );
        return saved;
    }

    @DeleteMapping("/api/emergency-contacts/{id}")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public void deleteContact(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id
    ) {
        EmergencyContact contact = contactRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found"));

        if (!contact.getWorkerId().equals(user.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your contact");
        }

        contactRepository.delete(contact);
        auditLogService.record(
            "EMERGENCY_CONTACT_DELETED", user.role(), user.fullName(), user.email(),
            "EmergencyContact", id, contact.getContactType() + ": " + contact.getName()
        );
    }

    @GetMapping("/api/emergency-contacts/directory")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public List<Map<String, Object>> getWorkerDirectory(@AuthenticationPrincipal AuthenticatedUser user) {
        String site = user.assignedSite() != null ? user.assignedSite() : "";
        return appUserRepository.findByAssignedSiteIgnoreCaseAndPendingFalseOrderByFullNameAsc(site)
            .stream()
            .filter(u -> !u.getEmail().equalsIgnoreCase(user.email()))
            .filter(u -> List.of("worker", "supervisor", "safetyOfficer").contains(u.getRole()))
            .filter(u -> u.getDeletedAt() == null && !Boolean.FALSE.equals(u.getActive()))
            .map(u -> {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("id", u.getId());
                entry.put("fullName", u.getFullName());
                entry.put("email", u.getEmail());
                entry.put("role", u.getRole());
                entry.put("contactCount", contactRepository.findByWorkerIdOrderByContactTypeAsc(u.getId()).size());
                return entry;
            })
            .collect(java.util.stream.Collectors.toList());
    }

    @GetMapping("/api/emergency-contacts/worker/{workerId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public List<EmergencyContact> getWorkerContactsById(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long workerId
    ) {
        AppUser worker = appUserRepository.findById(workerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker not found"));
        if (worker.getAssignedSite() == null || !worker.getAssignedSite().equalsIgnoreCase(user.assignedSite()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Worker belongs to a different site");
        return contactRepository.findByWorkerIdOrderByContactTypeAsc(workerId);
    }

    @GetMapping("/api/emergency-contacts/worker/email/{email}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public List<EmergencyContact> getWorkerContactsByEmail(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable String email
    ) {
        AppUser worker = appUserRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker not found"));
        if (worker.getAssignedSite() == null || !worker.getAssignedSite().equalsIgnoreCase(user.assignedSite()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Worker belongs to a different site");
        return contactRepository.findByWorkerIdOrderByContactTypeAsc(worker.getId());
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    // Strip spaces, dashes, brackets, dots — keep + for international prefix
    private String normalizePhone(String phone) {
        return phone.replaceAll("[\\s\\-().]", "");
    }

    // Ghana local: 0 + 9 digits (10 total)
    // Ghana international: +233 + 9 digits
    // Foreign international: + then 7–15 digits (not +233)
    private boolean isValidPhone(String normalized) {
        if (normalized.matches("^0\\d{9}$")) return true;
        if (normalized.matches("^\\+233\\d{9}$")) return true;
        if (normalized.matches("^\\+(?!233)\\d{7,14}$")) return true;
        return false;
    }
}
