package MineOpsBackend.controller;

import MineOpsBackend.model.CommunityEvent;
import MineOpsBackend.repository.CommunityEventRepository;
import MineOpsBackend.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/community/events")
public class CommunityEventController {

    private final CommunityEventRepository eventRepo;

    public CommunityEventController(CommunityEventRepository eventRepo) {
        this.eventRepo = eventRepo;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_WORKER', 'ROLE_SUPERVISOR', 'ROLE_SAFETY_OFFICER', 'ROLE_BUYER')")
    public List<CommunityEvent> getEvents() {
        return eventRepo.findAllByOrderByEventDateAsc();
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR', 'ROLE_SAFETY_OFFICER')")
    public CommunityEvent createEvent(@AuthenticationPrincipal AuthenticatedUser user,
                                      @RequestBody Map<String, String> body) {
        String title = body.get("title");
        String description = body.get("description");
        String eventType = body.get("eventType");
        String eventDateStr = body.get("eventDate");

        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title is required");
        }
        if (eventDateStr == null || eventDateStr.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "eventDate is required");
        }

        CommunityEvent event = new CommunityEvent();
        event.setTitle(title.trim());
        event.setDescription(description != null ? description.trim() : "");
        event.setEventType(eventType != null ? eventType.trim() : "General");
        try {
            event.setEventDate(LocalDateTime.parse(eventDateStr));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "eventDate must be ISO date-time format (yyyy-MM-ddTHH:mm:ss)");
        }
        event.setCreatedByEmail(user.email());
        event.setCreatedByName(user.fullName());
        event.setCreatedAt(LocalDateTime.now());
        return eventRepo.save(event);
    }
}
