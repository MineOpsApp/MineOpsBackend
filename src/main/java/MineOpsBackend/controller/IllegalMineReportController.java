package MineOpsBackend.controller;

import MineOpsBackend.dto.CreateIllegalMineReportRequest;
import MineOpsBackend.model.IllegalMineReport;
import MineOpsBackend.repository.IllegalMineReportRepository;
import MineOpsBackend.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@RestController
public class IllegalMineReportController {

    private final IllegalMineReportRepository repo;

    public IllegalMineReportController(IllegalMineReportRepository repo) {
        this.repo = repo;
    }

    @PostMapping("/api/illegal-mine-reports")
    @PreAuthorize("hasAnyAuthority('ROLE_WORKER','ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER','ROLE_BUYER')")
    public IllegalMineReport submitReport(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody CreateIllegalMineReportRequest request
    ) {
        if (request.photoData() != null && request.photoData().length() > 2_000_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo is too large. Use a smaller image.");
        }
        IllegalMineReport report = new IllegalMineReport();
        report.setReporterEmail(user.email());
        report.setReporterRole(user.role());
        report.setLocationDescription(request.locationDescription().trim());
        report.setDetails(request.details());
        report.setPhotoData(request.photoData());
        report.setLatitude(request.latitude());
        report.setLongitude(request.longitude());
        report.setCreatedAt(LocalDateTime.now());
        return repo.save(report);
    }
}
