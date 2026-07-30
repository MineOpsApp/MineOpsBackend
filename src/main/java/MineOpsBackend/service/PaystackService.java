package MineOpsBackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thin wrapper around Paystack's REST API for the marketplace buyer-payment flow. Follows the
 * same java.net.http.HttpClient pattern as PushNotificationService (no new HTTP client dependency
 * needed). Every method that actually calls Paystack throws a clear 503 if no secret key is
 * configured — see the application.properties comment on mineops.paystack.secret-key for why this
 * is a soft, non-fail-fast config (no Paystack account exists yet as of this writing).
 */
@Service
public class PaystackService {

    private static final Logger log = LoggerFactory.getLogger(PaystackService.class);
    private static final String BASE_URL = "https://api.paystack.co";

    private final String secretKey;
    private final String callbackUrl;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8))
        .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaystackService(
        @Value("${mineops.paystack.secret-key}") String secretKey,
        @Value("${mineops.paystack.callback-url}") String callbackUrl
    ) {
        this.secretKey = secretKey;
        this.callbackUrl = callbackUrl;
        if (secretKey == null || secretKey.isBlank()) {
            log.warn("[PaystackService] PAYSTACK_SECRET_KEY is not set — payment initiation/verification " +
                "will return 503 until it's configured. Set it once a Paystack account exists.");
        }
    }

    public boolean isConfigured() {
        return secretKey != null && !secretKey.isBlank();
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Payment processing is not configured yet. Contact MineOps support.");
        }
    }

    public record InitResult(String authorizationUrl, String accessCode, String reference) {}

    public record VerifyResult(
        boolean success, String status, long amountPesewas, String currency, String channel, String paidAtIso
    ) {}

    /** amountPesewas is GHS's lowest denomination (1 cedi = 100 pesewas) — Paystack always expects
     *  amount in the subunit of whatever currency is passed, never major units. */
    public InitResult initializeTransaction(String buyerEmail, long amountPesewas, String reference) {
        requireConfigured();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", buyerEmail);
        body.put("amount", amountPesewas);
        body.put("currency", "GHS");
        body.put("reference", reference);
        body.put("callback_url", callbackUrl);

        JsonNode data = post("/transaction/initialize", body);
        return new InitResult(
            data.path("authorization_url").asText(null),
            data.path("access_code").asText(null),
            data.path("reference").asText(reference)
        );
    }

    public VerifyResult verifyTransaction(String reference) {
        requireConfigured();
        JsonNode data = get("/transaction/verify/" + java.net.URLEncoder.encode(reference, StandardCharsets.UTF_8));
        String status = data.path("status").asText(""); // "success" | "failed" | "abandoned" | ...
        return new VerifyResult(
            "success".equals(status),
            status,
            data.path("amount").asLong(0),
            data.path("currency").asText("GHS"),
            data.path("channel").asText(null),
            data.path("paid_at").asText(null)
        );
    }

    /** Paystack signs each webhook body with HMAC-SHA512 using the secret key, sent in the
     *  x-paystack-signature header. Must be checked against the raw request body bytes — anything
     *  that re-serializes the parsed JSON first (even Jackson's own writer) can produce a
     *  byte-for-byte different string and fail a legitimate signature. */
    public boolean verifyWebhookSignature(String rawBody, String signatureHeader) {
        if (!isConfigured() || signatureHeader == null || signatureHeader.isBlank()) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] computed = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String computedHex = bytesToHex(computed);
            return MessageDigest.isEqual(
                computedHex.getBytes(StandardCharsets.UTF_8),
                signatureHeader.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.warn("Paystack webhook signature check failed: {}", e.getMessage());
            return false;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private JsonNode post(String path, Map<String, Object> body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + secretKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
            return send(request);
        } catch (ResponseStatusException e) {
            // send() already threw a specific, useful message (e.g. Paystack's own error text) —
            // don't let the catch-all below swallow it and replace it with a generic one.
            throw e;
        } catch (Exception e) {
            log.error("Paystack POST {} failed: {}", path, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not reach Paystack. Try again shortly.");
        }
    }

    private JsonNode get(String path) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + secretKey)
                .GET()
                .build();
            return send(request);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Paystack GET {} failed: {}", path, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not reach Paystack. Try again shortly.");
        }
    }

    private JsonNode send(HttpRequest request) throws Exception {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());
        boolean ok = root.path("status").asBoolean(false);
        if (response.statusCode() >= 400 || !ok) {
            String message = root.path("message").asText("Paystack request failed");
            log.warn("Paystack call returned {} — {}", response.statusCode(), message);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Paystack: " + message);
        }
        return root.path("data");
    }
}
