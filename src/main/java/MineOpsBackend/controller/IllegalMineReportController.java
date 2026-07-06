package MineOpsBackend.controller;

import MineOpsBackend.model.IllegalMineReport;
import MineOpsBackend.repository.IllegalMineReportRepository;
import MineOpsBackend.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;

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
        @RequestBody Map<String, String> body
    ) {
        String location = body.get("locationDescription");
        if (location == null || location.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location description is required");
        }
        IllegalMineReport report = new IllegalMineReport();
        report.setReporterEmail(user.email());
        report.setReporterRole(user.role());
        report.setLocationDescription(location.trim());
        report.setDetails(body.get("details"));
        report.setCreatedAt(LocalDateTime.now());
        return repo.save(report);
    }
}
