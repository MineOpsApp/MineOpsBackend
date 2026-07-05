package MineOpsBackend.controller;

import MineOpsBackend.model.CommunityEvent;
import MineOpsBackend.repository.CommunityEventRepository;
import MineOpsBackend.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunityEventControllerTest {

    @Mock CommunityEventRepository eventRepo;

    @InjectMocks CommunityEventController controller;

    private static final String SITE = "Obuasi Mine";

    private AuthenticatedUser supervisor() {
        return new AuthenticatedUser(1L, "Kwame", "kwame@mine.com", "supervisor", SITE, null);
    }

    @Test
    void getEvents_returnsAllOrderedByDate() {
        CommunityEvent e = new CommunityEvent();
        e.setTitle("Safety Day");
        when(eventRepo.findAllByOrderByEventDateAsc()).thenReturn(List.of(e));

        List<CommunityEvent> result = controller.getEvents();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Safety Day");
    }

    @Test
    void createEvent_throws400_whenTitleMissing() {
        Map<String, String> body = Map.of("eventDate", "2026-09-01T10:00:00");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.createEvent(supervisor(), body));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(eventRepo, never()).save(any());
    }

    @Test
    void createEvent_throws400_whenEventDateMissing() {
        Map<String, String> body = Map.of("title", "Safety Drill");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.createEvent(supervisor(), body));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(eventRepo, never()).save(any());
    }

    @Test
    void createEvent_savesEvent_withAllFields() {
        CommunityEvent saved = new CommunityEvent();
        when(eventRepo.save(any(CommunityEvent.class))).thenReturn(saved);

        Map<String, String> body = Map.of(
                "title", "Annual Safety Day",
                "description", "Site-wide safety review",
                "eventType", "Safety Drill",
                "eventDate", "2026-09-15T09:00:00"
        );
        controller.createEvent(supervisor(), body);

        ArgumentCaptor<CommunityEvent> cap = ArgumentCaptor.forClass(CommunityEvent.class);
        verify(eventRepo).save(cap.capture());
        CommunityEvent ev = cap.getValue();
        assertThat(ev.getTitle()).isEqualTo("Annual Safety Day");
        assertThat(ev.getDescription()).isEqualTo("Site-wide safety review");
        assertThat(ev.getEventType()).isEqualTo("Safety Drill");
        assertThat(ev.getCreatedByEmail()).isEqualTo("kwame@mine.com");
        assertThat(ev.getCreatedByName()).isEqualTo("Kwame");
    }

    @Test
    void createEvent_defaultsEventType_whenOmitted() {
        CommunityEvent saved = new CommunityEvent();
        when(eventRepo.save(any(CommunityEvent.class))).thenReturn(saved);

        Map<String, String> body = Map.of("title", "Meeting", "eventDate", "2026-10-01T08:00:00");
        controller.createEvent(supervisor(), body);

        ArgumentCaptor<CommunityEvent> cap = ArgumentCaptor.forClass(CommunityEvent.class);
        verify(eventRepo).save(cap.capture());
        assertThat(cap.getValue().getEventType()).isEqualTo("General");
    }
}
