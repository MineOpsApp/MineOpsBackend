package MineOpsBackend.controller;

import MineOpsBackend.model.SupervisorMessage;
import MineOpsBackend.repository.SupervisorMessageRepository;
import MineOpsBackend.service.AuditLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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
    public List<SupervisorMessage> getMessages() {
        return supervisorMessageRepository.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping("/api/messages")
    public SupervisorMessage createMessage(@RequestBody Map<String, String> request) {
        SupervisorMessage message = new SupervisorMessage(
            request.getOrDefault("senderRole", "supervisor"),
            request.getOrDefault("audience", "Workers"),
            request.getOrDefault("message", "Supervisor briefing sent from MineOps")
        );

        SupervisorMessage saved = supervisorMessageRepository.save(message);
        auditLogService.record(
            "MESSAGE_SENT",
            saved.getSenderRole(),
            request.getOrDefault("actorName", "Unknown User"),
            request.getOrDefault("actorEmail", ""),
            "SupervisorMessage",
            saved.getId(),
            saved.getAudience()
        );

        return saved;
    }
}
