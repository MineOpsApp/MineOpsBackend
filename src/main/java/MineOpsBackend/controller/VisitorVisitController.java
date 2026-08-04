package MineOpsBackend.controller;

import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.VisitorVisit;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.VisitorVisitRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/visitor-visits")
public class VisitorVisitController {

    private final VisitorVisitRepository visitRepo;
    private final AppUserRepository userRepo;
    private final NotificationService notificationService;

    public VisitorVisitController(
        VisitorVisitRepository visitRepo,
        AppUserRepository userRepo,
        NotificationService notificationService
    ) {
        this.visitRepo = visitRepo;
        this.userRepo = userRepo;
        this.notificationService = notificationService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public VisitorVisit create(
        @AuthenticationPrincipal AuthenticatedUser actor,
        @RequestBody Map<String, Object> body
    ) {
        Long guestUserId = getLong(body, "guestUserId");
        AppUser guest = userRepo.findById(guestUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Guest user not found"));
        if (guest.getAssignedSite() != null && !guest.getAssignedSite().equalsIgnoreCase(actor.assignedSite())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Guest belongs to a different site");
        }

        // If the supervisor picked a host from the site directory (rather than typing a free-text
        // name), resolve them so both the host and the guest can be notified below.
        String hostEmail = getString(body, "hostEmail");
        AppUser hostUser = hostEmail != null
            ? userRepo.findByEmailIgnoreCase(hostEmail)
                .filter(u -> u.getAssignedSite() != null && u.getAssignedSite().equalsIgnoreCase(actor.assignedSite()))
                .orElse(null)
            : null;

        VisitorVisit visit = new VisitorVisit();
        visit.setGuestUserId(guestUserId);
        visit.setHostName(hostUser != null ? hostUser.getFullName() : getString(body, "hostName"));
        visit.setHostEmail(hostUser != null ? hostUser.getEmail() : null);
        visit.setPurposeOfVisit(getString(body, "purposeOfVisit"));
        // Always use the actor's own site — never trust a client-supplied site value for scoping.
        visit.setAssignedSite(actor.assignedSite());
        visit.setVisitStart(parseDateTime(body, "visitStart"));
        visit.setVisitEnd(parseDateTime(body, "visitEnd"));
        visit.setApprovedZones(getString(body, "approvedZones"));
        visit.setEmergencyContactName(getString(body, "emergencyContactName"));
        visit.setEmergencyContactPhone(getString(body, "emergencyContactPhone"));
        visit.setVisitingOrganisation(getString(body, "visitingOrganisation"));
        visit.setRelationshipToHost(getString(body, "relationshipToHost"));
        visit.setVisitReason(getString(body, "visitReason"));
        visit.setVehicleRegistrationNumber(getString(body, "vehicleRegistrationNumber"));
        visit.setGroupSize(getInt(body, "groupSize"));
        visit.setMedicalConditionsNote(getString(body, "medicalConditionsNote"));
        visit.setStatus("PENDING");

        // Auto-generate visitor pass number: VIS-000123 style
        VisitorVisit saved = visitRepo.save(visit);
        saved.setVisitorPassNumber("VIS-" + String.format("%06d", saved.getId()));
        saved = visitRepo.save(saved);
        saved.setGuestFullName(guest.getFullName());

        try {
            if (hostUser != null) {
                notificationService.notify(
                    hostUser.getEmail(), "VISITOR_HOST",
                    "You've been assigned as a host",
                    guest.getFullName() + " is visiting " + actor.assignedSite() + " and you've been named as their host. Meet them shortly after they check in.",
                    "VisitorVisit", saved.getId()
                );
            }
            notificationService.notify(
                guest.getEmail(), "VISITOR_HOST",
                "Your host is on the way",
                (saved.getHostName() != null ? saved.getHostName() : "Your host") + " will meet you shortly during your visit to " + actor.assignedSite() + ".",
                "VisitorVisit", saved.getId()
            );
        } catch (Exception e) {
            // Visit creation must not fail because a notification couldn't be written.
        }

        return saved;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public List<VisitorVisit> listForSite(@AuthenticationPrincipal AuthenticatedUser actor) {
        // Always scoped to the caller's own site — a client-supplied site param (or omitting it
        // entirely) must never be able to pull another site's visitor records.
        List<VisitorVisit> visits = visitRepo.findByAssignedSiteOrderByVisitStartDesc(actor.assignedSite());
        // Per-request memoized lookup so a site with many visits doesn't re-query the same
        // repeat guest once per row (same pattern as EquipmentIssuesController.resolveSite).
        java.util.Map<Long, String> nameById = new java.util.LinkedHashMap<>();
        for (VisitorVisit v : visits) {
            String name = nameById.computeIfAbsent(v.getGuestUserId(), id ->
                userRepo.findById(id).map(AppUser::getFullName).orElse(null));
            v.setGuestFullName(name);
        }
        return visits;
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('ROLE_GUEST')")
    public VisitorVisit getMyVisit(@AuthenticationPrincipal AuthenticatedUser user) {
        VisitorVisit visit = visitRepo.findFirstByGuestUserIdOrderByCreatedAtDesc(user.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No visit record found"));
        visit.setGuestFullName(user.fullName());
        return visit;
    }

    @PostMapping("/{id}/check-in")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public VisitorVisit checkIn(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser actor) {
        VisitorVisit visit = findOwnedBySite(id, actor);
        if (!Boolean.TRUE.equals(visit.getInductionCompleted()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Induction must be completed before check-in.");
        visit.setCheckInAt(LocalDateTime.now());
        visit.setStatus("CHECKED_IN");
        VisitorVisit saved = visitRepo.save(visit);
        resolveGuestName(saved);
        return saved;
    }

    @PostMapping("/{id}/check-out")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public VisitorVisit checkOut(
        @PathVariable Long id,
        @AuthenticationPrincipal AuthenticatedUser actor,
        @RequestBody(required = false) Map<String, Object> body
    ) {
        VisitorVisit visit = findOwnedBySite(id, actor);
        visit.setCheckOutAt(LocalDateTime.now());
        visit.setStatus("CHECKED_OUT");
        if (body != null) {
            if (body.containsKey("zonesVisited")) visit.setZonesVisited(getString(body, "zonesVisited"));
        }
        VisitorVisit saved = visitRepo.save(visit);
        resolveGuestName(saved);
        return saved;
    }

    @PostMapping("/{id}/induction")
    @PreAuthorize("hasAuthority('ROLE_GUEST')")
    public VisitorVisit completeInduction(
        @PathVariable Long id,
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody(required = false) Map<String, Object> body
    ) {
        VisitorVisit visit = findOrThrow(id);
        if (!visit.getGuestUserId().equals(user.id()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your visit record.");
        visit.setInductionCompleted(true);
        visit.setInductionCompletedAt(LocalDateTime.now());
        if (body != null && body.containsKey("signOff"))
            visit.setInductionSignOff(getString(body, "signOff"));
        return visitRepo.save(visit);
    }

    @PatchMapping("/{id}/ppe")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public VisitorVisit updatePpe(
        @PathVariable Long id,
        @AuthenticationPrincipal AuthenticatedUser actor,
        @RequestBody Map<String, Object> body
    ) {
        VisitorVisit visit = findOwnedBySite(id, actor);
        if (body.containsKey("ppeIssued"))
            visit.setPpeIssued(Boolean.TRUE.equals(body.get("ppeIssued")));
        if (body.containsKey("ppeItems"))
            visit.setPpeItems(getString(body, "ppeItems"));
        VisitorVisit saved = visitRepo.save(visit);
        resolveGuestName(saved);
        return saved;
    }

    private void resolveGuestName(VisitorVisit visit) {
        userRepo.findById(visit.getGuestUserId()).map(AppUser::getFullName).ifPresent(visit::setGuestFullName);
    }

    private VisitorVisit findOrThrow(Long id) {
        return visitRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visit not found"));
    }

    /** Like findOrThrow, but also rejects visits belonging to a different supervisor/safety officer's site. */
    private VisitorVisit findOwnedBySite(Long id, AuthenticatedUser actor) {
        VisitorVisit visit = findOrThrow(id);
        if (visit.getAssignedSite() != null && !visit.getAssignedSite().equalsIgnoreCase(actor.assignedSite())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Visit belongs to a different site");
        }
        return visit;
    }

    private String getString(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v instanceof String s && !s.isBlank() ? s.trim() : null;
    }

    private Long getLong(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s) return Long.parseLong(s);
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, key + " is required");
    }

    private Integer getInt(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s && !s.isBlank()) return Integer.parseInt(s);
        return null;
    }

    private LocalDateTime parseDateTime(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null || (v instanceof String s && s.isBlank())) return null;
        try { return LocalDateTime.parse(v.toString()); } catch (Exception e) { return null; }
    }
}
