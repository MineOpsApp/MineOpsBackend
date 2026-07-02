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

import java.util.List;
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

        String type = request.contactType().toUpperCase();

        EmergencyContact contact = contactRepository
            .findByWorkerIdAndContactType(user.id(), type)
            .orElse(null);

        if (contact == null) {
            List<EmergencyContact> existing = contactRepository.findByWorkerIdOrderByContactTypeAsc(user.id());
            if (existing.size() >= 2) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum 2 emergency contacts allowed");
            }
            contact = new EmergencyContact(user.id(), type, request.name().trim(), request.relationship().trim(), request.phone().trim());
        } else {
            contact.setName(request.name().trim());
            contact.setRelationship(request.relationship().trim());
            contact.setPhone(request.phone().trim());
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

    @GetMapping("/api/emergency-contacts/worker/{workerId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public List<EmergencyContact> getWorkerContactsById(@PathVariable Long workerId) {
        return contactRepository.findByWorkerIdOrderByContactTypeAsc(workerId);
    }

    @GetMapping("/api/emergency-contacts/worker/email/{email}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public List<EmergencyContact> getWorkerContactsByEmail(@PathVariable String email) {
        AppUser worker = appUserRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker not found"));
        return contactRepository.findByWorkerIdOrderByContactTypeAsc(worker.getId());
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
