package MineOpsBackend.service;

import MineOpsBackend.model.AuditOutboxEntry;
import MineOpsBackend.repository.AuditOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AuditOutboxRelay {

    private static final String INTERNAL_API_KEY_HEADER = "X-MineOps-Internal-Key";
    private static final int MAX_ATTEMPTS = 8;

    private final AuditOutboxRepository auditOutboxRepository;
    private final String auditServiceUrl;
    private final String internalApiKey;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuditOutboxRelay(
        AuditOutboxRepository auditOutboxRepository,
        @Value("${mineops.audit-service.url}") String auditServiceUrl,
        @Value("${mineops.internal-api-key}") String internalApiKey
    ) {
        this.auditOutboxRepository = auditOutboxRepository;
        this.auditServiceUrl = auditServiceUrl.trim().replaceAll("/+$", "");
        this.internalApiKey = internalApiKey.trim();
    }

    @Scheduled(fixedDelay = 10000)
    public void relayPending() {
        List<AuditOutboxEntry> batch = auditOutboxRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");

        for (AuditOutboxEntry entry : batch) {
            try {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("action", entry.getAction());
                body.put("actorRole", entry.getActorRole());
                body.put("actorName", entry.getActorName());
                body.put("actorEmail", entry.getActorEmail());
                body.put("targetType", entry.getTargetType());
                body.put("targetId", entry.getTargetId());
                body.put("details", entry.getDetails());

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(auditServiceUrl + "/api/audit-logs"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    entry.setStatus("SENT");
                } else {
                    markFailedAttempt(entry);
                }
            } catch (Exception error) {
                markFailedAttempt(entry);
            }

            entry.setLastAttemptAt(LocalDateTime.now());
            auditOutboxRepository.save(entry);
        }
    }

    private void markFailedAttempt(AuditOutboxEntry entry) {
        entry.setAttempts(entry.getAttempts() + 1);

        if (entry.getAttempts() >= MAX_ATTEMPTS) {
            entry.setStatus("FAILED");
        }
    }
}
