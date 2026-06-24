package MineOpsBackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class PushNotificationService {

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void sendToToken(String pushToken, String title, String body, String channelId) {
        if (pushToken == null || pushToken.isBlank()) return;
        try {
            Map<String, Object> message = new java.util.LinkedHashMap<>();
            message.put("to", pushToken);
            message.put("title", title);
            message.put("body", body);
            message.put("sound", "default");
            message.put("priority", "high");
            if (channelId != null) message.put("channelId", channelId);

            String json = objectMapper.writeValueAsString(List.of(message));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://exp.host/--/api/v2/push/send"))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Push notification sent: " + response.statusCode());
        } catch (Exception e) {
            System.err.println("Push notification failed: " + e.getMessage());
        }
    }

    public void sendToTokens(List<String> pushTokens, String title, String body, String channelId) {
        pushTokens.stream()
            .filter(t -> t != null && !t.isBlank())
            .forEach(token -> sendToToken(token, title, body, channelId));
    }
}