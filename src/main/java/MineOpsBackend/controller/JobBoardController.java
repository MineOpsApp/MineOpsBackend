package MineOpsBackend.controller;

import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.JobInterest;
import MineOpsBackend.model.JobPosting;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.JobInterestRepository;
import MineOpsBackend.repository.JobPostingRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import MineOpsBackend.service.NotificationService;
import MineOpsBackend.service.PushNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/community/jobs")
public class JobBoardController {

    private static final Logger log = LoggerFactory.getLogger(JobBoardController.class);

    private final JobPostingRepository jobRepo;
    private final JobInterestRepository interestRepo;
    private final AppUserRepository appUserRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final PushNotificationService pushNotificationService;

    public JobBoardController(
        JobPostingRepository jobRepo,
        JobInterestRepository interestRepo,
        AppUserRepository appUserRepository,
        AuditLogService auditLogService,
        NotificationService notificationService,
        PushNotificationService pushNotificationService
    ) {
        this.jobRepo = jobRepo;
        this.interestRepo = interestRepo;
        this.appUserRepository = appUserRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.pushNotificationService = pushNotificationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_WORKER', 'ROLE_SUPERVISOR', 'ROLE_SAFETY_OFFICER', 'ROLE_BUYER')")
    public List<JobPosting> getJobs() {
        return jobRepo.findByStatusOrderByCreatedAtDesc("OPEN");
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public JobPosting createJob(@AuthenticationPrincipal AuthenticatedUser user,
                                @RequestBody Map<String, String> body) {
        String title = body.get("title");
        String description = body.get("description");

        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title is required");
        }

        JobPosting job = new JobPosting();
        job.setSite(user.assignedSite());
        job.setTitle(title.trim());
        job.setDescription(description != null ? description.trim() : "");
        job.setPostedByEmail(user.email());
        job.setPostedByName(user.fullName());
        job.setStatus("OPEN");
        job.setCreatedAt(LocalDateTime.now());
        return jobRepo.save(job);
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public JobPosting closeJob(@PathVariable Long id,
                               @AuthenticationPrincipal AuthenticatedUser user) {
        JobPosting job = jobRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        if (!user.assignedSite().equalsIgnoreCase(job.getSite())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your site's job posting");
        }
        job.setStatus("CLOSED");
        return jobRepo.save(job);
    }

    @PostMapping("/{id}/interest")
    @PreAuthorize("hasAnyAuthority('ROLE_WORKER', 'ROLE_SUPERVISOR', 'ROLE_SAFETY_OFFICER', 'ROLE_BUYER')")
    public JobInterest expressInterest(@PathVariable Long id,
                                       @AuthenticationPrincipal AuthenticatedUser user,
                                       @RequestBody Map<String, String> body) {
        JobPosting job = jobRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        if (!"OPEN".equals(job.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Job is no longer open");
        }
        if (interestRepo.existsByJobPostingIdAndApplicantEmail(id, user.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already expressed interest");
        }

        JobInterest interest = new JobInterest();
        interest.setJobPostingId(id);
        interest.setApplicantEmail(user.email());
        interest.setApplicantName(user.fullName());
        interest.setApplicantRole(user.role());
        String message = body.get("message");
        interest.setMessage(message != null ? message.trim() : "");
        interest.setCreatedAt(LocalDateTime.now());
        JobInterest saved = interestRepo.save(interest);

        auditLogService.record("JOB_INTEREST_EXPRESSED", user.role(), user.fullName(), user.email(),
            "JobInterest", saved.getId(), job.getTitle());

        // Tell the supervisor who posted the job — previously this saved silently and the
        // poster had no way to find out short of guessing to check, since no screen even
        // listed interests.
        try {
            String notifTitle = "Interest in " + job.getTitle();
            String notifBody = user.fullName() + " is interested in \"" + job.getTitle() + "\".";
            notificationService.notify(job.getPostedByEmail(), "JOB_INTEREST", notifTitle, notifBody,
                "JobPosting", job.getId());
            appUserRepository.findByEmailIgnoreCase(job.getPostedByEmail()).ifPresent((AppUser poster) -> {
                String token = poster.getPushToken();
                if (token != null && !token.isBlank()) {
                    pushNotificationService.sendToToken(token, notifTitle, notifBody, "default");
                }
            });
        } catch (Exception e) {
            log.warn("Push notification failed for job interest: {}", e.getMessage());
        }

        return saved;
    }

    @GetMapping("/{id}/interest")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public List<JobInterest> getInterest(@PathVariable Long id,
                                         @AuthenticationPrincipal AuthenticatedUser user) {
        JobPosting job = jobRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        if (!user.assignedSite().equalsIgnoreCase(job.getSite())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your site's job posting");
        }
        return interestRepo.findByJobPostingIdOrderByCreatedAtDesc(id);
    }
}
