package MineOpsBackend.controller;

import MineOpsBackend.model.SupervisorMessage;
import MineOpsBackend.repository.SupervisorMessageRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class MessageController {

    private final SupervisorMessageRepository supervisorMessageRepository;

    public MessageController(SupervisorMessageRepository supervisorMessageRepository) {
        this.supervisorMessageRepository = supervisorMessageRepository;
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

        return supervisorMessageRepository.save(message);
    }
}
