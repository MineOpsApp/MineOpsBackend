package MineOpsBackend.controller;

import MineOpsBackend.dto.CreateMessageRequest;
import MineOpsBackend.model.SupervisorMessage;
import MineOpsBackend.repository.SupervisorMessageRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MessageController {

    private final SupervisorMessageRepository supervisorMessageRepository;
    private final AuditLogService auditLogService;

    public MessageController(
        SupervisorMessageRepository supervisorMessageRepository,
        AuditLogService auditLogService
    ) {
        this.supervisorMessageRepository = supervisorMessageRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/api/messages")
    @PreAuthorize("hasAnyAuthority('ROLE_WORKER','ROLE_SUPERVISOR')")
    public List<SupervisorMessage> getMessages() {
        return supervisorMessageRepository.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping("/api/messages")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public SupervisorMessage createMessage(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody CreateMessageRequest request
    ) {
        SupervisorMessage message = new SupervisorMessage(
            user.role(),
            request.audience(),
            request.message()
        );

        SupervisorMessage saved = supervisorMessageRepository.save(message);
        auditLogService.record(
            "MESSAGE_SENT",
            saved.getSenderRole(),
            user.fullName(),
            user.email(),
            "SupervisorMessage",
            saved.getId(),
            saved.getAudience()
        );

        return saved;
    }
}
