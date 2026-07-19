package MineOpsBackend.service;

import MineOpsBackend.model.AppUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JwtService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String secret;

    public JwtService(@Value("${mineops.jwt.secret}") String secret) {
        if (secret == null || secret.trim().length() < 32) {
            throw new IllegalStateException("MINEOPS_JWT_SECRET must be set and at least 32 characters long");
        }

        this.secret = secret.trim();
    }

    public String createToken(AppUser user, Long sessionId) {
        try {
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", user.getId());
            payload.put("email", user.getEmail());
            payload.put("role", user.getRole());
            payload.put("site", user.getAssignedSite());
            payload.put("guestSubRole", user.getGuestSubRole());
            payload.put("sid", sessionId);
            payload.put("exp", Instant.now().plusSeconds(60 * 60).getEpochSecond()); // 1 hour

            String headerPart = encode(objectMapper.writeValueAsBytes(header));
            String payloadPart = encode(objectMapper.writeValueAsBytes(payload));
            String signaturePart = sign(headerPart + "." + payloadPart);

            return headerPart + "." + payloadPart + "." + signaturePart;
        } catch (Exception error) {
            throw new IllegalStateException("Could not create token", error);
        }
    }

    public JwtClaims validateToken(String token) {
        try {
            String[] parts = token.split("\\.");

            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid token format");
            }

            String signedContent = parts[0] + "." + parts[1];
            String expectedSignature = sign(signedContent);

            if (!constantTimeEquals(expectedSignature, parts[2])) {
                throw new IllegalArgumentException("Invalid token signature");
            }

            Map<String, Object> payload = objectMapper.readValue(decode(parts[1]), Map.class);
            long expiresAt = asLong(payload.get("exp"));

            if (Instant.now().getEpochSecond() >= expiresAt) {
                throw new IllegalArgumentException("Token expired");
            }

            return new JwtClaims(
    asLong(payload.get("sub")),
    asString(payload.get("email")),
    asString(payload.get("role")),
    payload.get("site") == null ? null : String.valueOf(payload.get("site")),
    payload.get("guestSubRole") == null ? null : String.valueOf(payload.get("guestSubRole")),
    asLong(payload.get("sid"))
);
        } catch (Exception error) {
            throw new IllegalArgumentException("Invalid token", error);
        }
    }

    private String sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

        return encode(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            actual.getBytes(StandardCharsets.UTF_8)
        );
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        return Long.parseLong(String.valueOf(value));
    }

    private String asString(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Missing token claim");
        }

        return String.valueOf(value);
    }

    public record JwtClaims(Long userId, String email, String role, String site, String guestSubRole, Long sessionId) {
    }
}


