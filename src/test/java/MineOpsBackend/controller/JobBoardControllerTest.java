package MineOpsBackend.controller;

import MineOpsBackend.model.JobInterest;
import MineOpsBackend.model.JobPosting;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.JobInterestRepository;
import MineOpsBackend.repository.JobPostingRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import MineOpsBackend.service.NotificationService;
import MineOpsBackend.service.PushNotificationService;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobBoardControllerTest {

    @Mock JobPostingRepository jobRepo;
    @Mock JobInterestRepository interestRepo;
    @Mock AppUserRepository appUserRepository;
    @Mock AuditLogService auditLogService;
    @Mock NotificationService notificationService;
    @Mock PushNotificationService pushNotificationService;

    @InjectMocks JobBoardController controller;

    private static final String SITE = "Obuasi Mine";
    private static final String OTHER_SITE = "Bibiani Mine";

    private AuthenticatedUser supervisor(String site) {
        return new AuthenticatedUser(1L, "Kwame", "kwame@mine.com", "supervisor", site, null);
    }

    private AuthenticatedUser buyer() {
        return new AuthenticatedUser(10L, "Ama", "ama@buyer.com", "buyer", null, null);
    }

    private JobPosting openJob(String site) {
        JobPosting j = new JobPosting();
        j.setSite(site);
        j.setTitle("Blasting Technician");
        j.setStatus("OPEN");
        j.setPostedByEmail("kwame@mine.com");
        return j;
    }

    // ── createJob ─────────────────────────────────────────────────────────────

    @Test
    void createJob_throws400_whenTitleMissing() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.createJob(supervisor(SITE), Map.of()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(jobRepo, never()).save(any());
    }

    @Test
    void createJob_savesWithSupervisorSite() {
        when(jobRepo.save(any())).thenReturn(new JobPosting());
        controller.createJob(supervisor(SITE), Map.of("title", "Driller", "description", "Drill stuff"));

        ArgumentCaptor<JobPosting> cap = ArgumentCaptor.forClass(JobPosting.class);
        verify(jobRepo).save(cap.capture());
        assertThat(cap.getValue().getSite()).isEqualTo(SITE);
        assertThat(cap.getValue().getStatus()).isEqualTo("OPEN");
    }

    // ── closeJob — site-scoping ───────────────────────────────────────────────

    @Test
    void closeJob_throws404_whenNotFound() {
        when(jobRepo.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.closeJob(99L, supervisor(SITE)));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void closeJob_throws403_whenDifferentSite() {
        when(jobRepo.findById(1L)).thenReturn(Optional.of(openJob(OTHER_SITE)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.closeJob(1L, supervisor(SITE)));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(jobRepo, never()).save(any());
    }

    @Test
    void closeJob_marksJobClosed_whenSameSite() {
        JobPosting job = openJob(SITE);
        when(jobRepo.findById(1L)).thenReturn(Optional.of(job));
        when(jobRepo.save(any())).thenReturn(job);

        controller.closeJob(1L, supervisor(SITE));

        assertThat(job.getStatus()).isEqualTo("CLOSED");
        verify(jobRepo).save(job);
    }

    // ── expressInterest ───────────────────────────────────────────────────────

    @Test
    void expressInterest_throws404_whenJobNotFound() {
        when(jobRepo.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.expressInterest(99L, buyer(), Map.of()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void expressInterest_throws409_whenJobClosed() {
        JobPosting job = openJob(SITE);
        job.setStatus("CLOSED");
        when(jobRepo.findById(1L)).thenReturn(Optional.of(job));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.expressInterest(1L, buyer(), Map.of()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(interestRepo, never()).save(any());
    }

    @Test
    void expressInterest_throws409_whenAlreadyApplied() {
        when(jobRepo.findById(1L)).thenReturn(Optional.of(openJob(SITE)));
        when(interestRepo.existsByJobPostingIdAndApplicantEmail(1L, "ama@buyer.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.expressInterest(1L, buyer(), Map.of()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(interestRepo, never()).save(any());
    }

    @Test
    void expressInterest_savesInterest_whenEligible() {
        when(jobRepo.findById(1L)).thenReturn(Optional.of(openJob(SITE)));
        when(interestRepo.existsByJobPostingIdAndApplicantEmail(1L, "ama@buyer.com")).thenReturn(false);
        when(interestRepo.save(any())).thenReturn(new JobInterest());

        controller.expressInterest(1L, buyer(), Map.of("message", "Interested!"));

        ArgumentCaptor<JobInterest> cap = ArgumentCaptor.forClass(JobInterest.class);
        verify(interestRepo).save(cap.capture());
        assertThat(cap.getValue().getApplicantEmail()).isEqualTo("ama@buyer.com");
        assertThat(cap.getValue().getMessage()).isEqualTo("Interested!");
    }

    // ── getInterest — site-scoping ────────────────────────────────────────────

    @Test
    void getInterest_throws404_whenJobNotFound() {
        when(jobRepo.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.getInterest(99L, supervisor(SITE)));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getInterest_throws403_whenDifferentSite() {
        when(jobRepo.findById(1L)).thenReturn(Optional.of(openJob(OTHER_SITE)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.getInterest(1L, supervisor(SITE)));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getInterest_returnsApplicants_whenSameSite() {
        when(jobRepo.findById(1L)).thenReturn(Optional.of(openJob(SITE)));
        when(interestRepo.findByJobPostingIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        List<JobInterest> result = controller.getInterest(1L, supervisor(SITE));

        assertThat(result).isEmpty();
        verify(interestRepo).findByJobPostingIdOrderByCreatedAtDesc(1L);
    }
}
