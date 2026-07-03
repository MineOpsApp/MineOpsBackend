package MineOpsBackend.controller;

import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.LoneWorkerSession;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.LoneWorkerRepository;
import MineOpsBackend.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/lone-worker")
public class LoneWorkerController {

    private final LoneWorkerRepository repo;
    private final AppUserRepository userRepo;

    public LoneWorkerController(LoneWorkerRepository repo, AppUserRepository userRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
    }

    @PostMapping("/start")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public Map<String, Object> start(
        @AuthenticationPrincipal AuthenticatedUser auth,
        @RequestBody Map<String, Object> body
    ) {
        AppUser worker = userRepo.findById(auth.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // deactivate any existing session
        repo.findTopByWorkerIdAndActiveOrderByStartedAtDesc(worker.getId(), true)
            .ifPresent(s -> { s.setActive(false); repo.save(s); });

        int intervalMinutes = body.containsKey("intervalMinutes")
            ? (int) body.get("intervalMinutes") : 60;

        LoneWorkerSession session = new LoneWorkerSession();
        session.setWorkerId(worker.getId());
        session.setWorkerEmail(worker.getEmail());
        session.setWorkerName(worker.getFullName());
        session.setSite(worker.getAssignedSite() != null ? worker.getAssignedSite() : "");
        session.setIntervalMinutes(intervalMinutes);
        session.setActive(true);
        session.setAlerted(false);
        LoneWorkerSession saved = repo.save(session);

        return sessionToMap(saved);
    }

    @PostMapping("/checkin")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public Map<String, Object> checkIn(@AuthenticationPrincipal AuthenticatedUser auth) {
        LoneWorkerSession session = repo
            .findTopByWorkerIdAndActiveOrderByStartedAtDesc(auth.id(), true)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active lone worker session"));
        session.recordCheckIn();
        return sessionToMap(repo.save(session));
    }

    @PostMapping("/stop")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public Map<String, Object> stop(@AuthenticationPrincipal AuthenticatedUser auth) {
        LoneWorkerSession session = repo
            .findTopByWorkerIdAndActiveOrderByStartedAtDesc(auth.id(), true)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active lone worker session"));
        session.setActive(false);
        return sessionToMap(repo.save(session));
    }

    @GetMapping("/status")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public Map<String, Object> status(@AuthenticationPrincipal AuthenticatedUser auth) {
        return repo.findTopByWorkerIdAndActiveOrderByStartedAtDesc(auth.id(), true)
            .map(this::sessionToMap)
            .orElse(Map.of("active", false));
    }

    @GetMapping("/site")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public List<Map<String, Object>> siteView(@AuthenticationPrincipal AuthenticatedUser auth) {
        AppUser supervisor = userRepo.findById(auth.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String site = supervisor.getAssignedSite() != null ? supervisor.getAssignedSite() : "";
        return repo.findBySiteIgnoreCaseAndActive(site, true).stream()
            .map(this::sessionToMap)
            .collect(Collectors.toList());
    }

    private Map<String, Object> sessionToMap(LoneWorkerSession s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("workerId", s.getWorkerId());
        m.put("workerName", s.getWorkerName());
        m.put("site", s.getSite());
        m.put("intervalMinutes", s.getIntervalMinutes());
        m.put("startedAt", s.getStartedAt());
        m.put("lastCheckedInAt", s.getLastCheckedInAt());
        m.put("deadline", s.getDeadline());
        m.put("active", s.isActive());
        m.put("alerted", s.isAlerted());
        return m;
    }
}
