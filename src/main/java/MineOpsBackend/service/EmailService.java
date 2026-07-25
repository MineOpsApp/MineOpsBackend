package MineOpsBackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

/**
 * Sends transactional emails (registration status, password reset, pay disbursement, etc.) via
 * the Gmail API (https://gmail.googleapis.com) instead of SMTP or a third-party relay.
 *
 * Why: Railway blocks outbound SMTP ports below its Pro plan, so direct SMTP was out. Every
 * third-party relay we tried (Brevo, Resend) hit the same wall: a relay can't get DKIM/SPF
 * alignment for a "From" domain it doesn't own (gmail.com, outlook.com, whatever), so receiving
 * providers — especially Gmail — silently soft-bounce the mail. The Gmail API sidesteps both
 * problems at once: it's a plain HTTPS call (no port block) that genuinely sends through the real
 * mobilegroup96@gmail.com account via Google's own infrastructure, so it's authenticated by
 * definition — no spoofing, no relay, no deliverability guesswork. It's also free (large daily
 * quota, no Google Cloud billing account required) — unlike Firebase Extensions, which needed the
 * paid Blaze plan.
 *
 * Auth: uses a long-lived OAuth2 refresh token (obtained once via Google's OAuth consent flow for
 * the gmail.send scope) to mint short-lived access tokens on demand. The access token is cached
 * in memory and refreshed ~5 minutes before it expires.
 *
 * All send methods swallow their own exceptions and log rather than throwing: a failed email
 * (bad OAuth creds, network blip) must never roll back or block the underlying business action
 * (approving a worker, disbursing pay, resetting a password) that triggered it.
 *
 * Every public method is @Async: callers (AuthController, AdminController, etc.) must not block
 * the HTTP response on the Gmail API call — the caller's request returns immediately and the send
 * happens on a background thread. Requires @EnableAsync on the main application class.
 *
 * Configure mineops.gmail.client-id / client-secret / refresh-token (GMAIL_CLIENT_ID /
 * GMAIL_CLIENT_SECRET / GMAIL_REFRESH_TOKEN env vars) — see Google Cloud Console OAuth setup.
 * With mineops.mail.enabled=false, sends are skipped and logged instead — useful for local dev
 * without Gmail API credentials on hand.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final URI TOKEN_ENDPOINT = URI.create("https://oauth2.googleapis.com/token");
    private static final URI SEND_ENDPOINT = URI.create("https://gmail.googleapis.com/gmail/v1/users/me/messages/send");

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String clientId;
    private final String clientSecret;
    private final String refreshToken;
    private final String senderEmail;
    private final String senderName;
    private final boolean enabled;

    // Cached access token — Gmail API access tokens are short-lived (~1hr); avoid re-minting one
    // on every single send.
    private volatile String cachedAccessToken;
    private volatile Instant cachedAccessTokenExpiry = Instant.EPOCH;

    public EmailService(
        @Value("${mineops.gmail.client-id:}") String clientId,
        @Value("${mineops.gmail.client-secret:}") String clientSecret,
        @Value("${mineops.gmail.refresh-token:}") String refreshToken,
        @Value("${mineops.gmail.sender-email:mobilegroup96@gmail.com}") String senderEmail,
        @Value("${mineops.gmail.sender-name:MineOps}") String senderName,
        @Value("${mineops.mail.enabled:true}") boolean enabled
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.enabled = enabled;
    }

    // Runs once at boot. A missing/blank Gmail credential doesn't fail startup (email is
    // best-effort and must never block the app from coming up), but it silently disables every
    // transactional email in the system — including password reset OTPs, which is the ONLY
    // self-service recovery path for supervisor/safetyOfficer accounts (admins are explicitly
    // blocked from resetting those two roles' passwords for privilege-separation reasons — see
    // AdminController.resetPassword). Log this loudly and immediately on deploy, not only when a
    // real user hits a lockout and someone has to dig through logs to find out why.
    @PostConstruct
    private void checkConfigOnStartup() {
        if (!enabled) {
            log.warn("[EMAIL] mineops.mail.enabled=false — all transactional email (including password reset OTPs) is disabled.");
            return;
        }
        if (clientId.isBlank() || clientSecret.isBlank() || refreshToken.isBlank()) {
            log.error("[EMAIL MISCONFIGURED] Gmail API credentials are missing (GMAIL_CLIENT_ID / GMAIL_CLIENT_SECRET / "
                + "GMAIL_REFRESH_TOKEN). ALL transactional email will silently fail to send, including password reset "
                + "OTPs — supervisors and safety officers have NO other way to recover a forgotten password. Fix the "
                + "Railway env vars now, before someone gets locked out.");
        } else {
            log.info("[EMAIL] Gmail API credentials configured — sender={}", senderEmail);
        }
    }

    private synchronized String getAccessToken() throws Exception {
        if (cachedAccessToken != null && Instant.now().isBefore(cachedAccessTokenExpiry.minusSeconds(300))) {
            return cachedAccessToken;
        }
        String form = "grant_type=refresh_token"
            + "&client_id=" + java.net.URLEncoder.encode(clientId, StandardCharsets.UTF_8)
            + "&client_secret=" + java.net.URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
            + "&refresh_token=" + java.net.URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(TOKEN_ENDPOINT)
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Gmail token refresh failed: status=" + response.statusCode() + " body=" + response.body());
        }
        JsonNode json = objectMapper.readTree(response.body());
        String accessToken = json.path("access_token").asText(null);
        int expiresIn = json.path("expires_in").asInt(3600);
        if (accessToken == null) {
            throw new IllegalStateException("Gmail token refresh response missing access_token: " + response.body());
        }
        cachedAccessToken = accessToken;
        cachedAccessTokenExpiry = Instant.now().plusSeconds(expiresIn);
        return accessToken;
    }

    private void send(String toEmail, String subject, String htmlBody) {
        if (!enabled) {
            log.info("[EMAIL disabled] to={} subject={}", toEmail, subject);
            return;
        }
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("[EMAIL skipped] no recipient address for subject={}", subject);
            return;
        }
        if (clientId.isBlank() || clientSecret.isBlank() || refreshToken.isBlank()) {
            log.warn("[EMAIL skipped] Gmail API credentials not configured — to={} subject={}", toEmail, subject);
            return;
        }
        try {
            String accessToken = getAccessToken();

            String mime = "From: " + senderName + " <" + senderEmail + ">\r\n"
                + "To: " + toEmail + "\r\n"
                + "Subject: " + subject + "\r\n"
                + "MIME-Version: 1.0\r\n"
                + "Content-Type: text/html; charset=UTF-8\r\n\r\n"
                + htmlBody;
            String rawMessage = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mime.getBytes(StandardCharsets.UTF_8));

            String requestBody = objectMapper.writeValueAsString(Map.of("raw", rawMessage));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(SEND_ENDPOINT)
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("[EMAIL sent via Gmail API] to={} subject={}", toEmail, subject);
            } else {
                log.error("[EMAIL FAILED] to={} subject={} status={} body={}", toEmail, subject, response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("[EMAIL FAILED] to={} subject={} reason={}", toEmail, subject, e.toString());
        }
    }

    private String wrap(String title, String bodyHtml) {
        return "<!DOCTYPE html><html><body style=\"margin:0;padding:0;background:#f2ede6;font-family:Arial,Helvetica,sans-serif;\">"
            + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f2ede6;padding:24px 0;\">"
            + "<tr><td align=\"center\">"
            + "<table role=\"presentation\" width=\"480\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#ffffff;border-radius:12px;overflow:hidden;\">"
            + "<tr><td style=\"background:#8a4b1f;padding:20px 28px;\">"
            + "<span style=\"color:#ffffff;font-size:20px;font-weight:800;\">MineOps</span>"
            + "</td></tr>"
            + "<tr><td style=\"padding:28px;\">"
            + "<h2 style=\"margin:0 0 12px;color:#241b12;font-size:18px;\">" + title + "</h2>"
            + "<div style=\"color:#4a4038;font-size:14px;line-height:22px;\">" + bodyHtml + "</div>"
            + "</td></tr>"
            + "<tr><td style=\"padding:16px 28px;background:#f7f3ef;\">"
            + "<span style=\"color:#9a8f83;font-size:11px;\">This is an automated message from MineOps. Please do not reply to this email.</span>"
            + "</td></tr>"
            + "</table></td></tr></table></body></html>";
    }

    @Async
    public void sendRegistrationPending(String email, String fullName, String role) {
        String title = "Registration received";
        String body = "<p>Hi " + safe(fullName) + ",</p>"
            + "<p>Thanks for registering with MineOps as a <strong>" + safe(role) + "</strong>. "
            + "Your account is now awaiting review by a supervisor or safety officer.</p>"
            + "<p>You'll receive another email as soon as a decision is made, and you'll be able to sign in once approved.</p>";
        send(email, title, wrap(title, body));
    }

    @Async
    public void sendRegistrationApproved(String email, String fullName) {
        String title = "Your account has been approved";
        String body = "<p>Hi " + safe(fullName) + ",</p>"
            + "<p>Good news — your MineOps account has been approved. You can now sign in with your email and password.</p>";
        send(email, title, wrap(title, body));
    }

    @Async
    public void sendRegistrationRejected(String email, String fullName) {
        String title = "Registration not approved";
        String body = "<p>Hi " + safe(fullName) + ",</p>"
            + "<p>Your MineOps registration was not approved by your site's supervisor or safety officer. "
            + "If you believe this is a mistake, please contact your site administrator directly.</p>";
        send(email, title, wrap(title, body));
    }

    @Async
    public void sendAccountSuspended(String email, String fullName) {
        String title = "Your account has been suspended";
        String body = "<p>Hi " + safe(fullName) + ",</p>"
            + "<p>Your MineOps account has been suspended by your site administrator and you will not be able to sign in until it is reinstated. "
            + "Contact your supervisor or safety officer for details.</p>";
        send(email, title, wrap(title, body));
    }

    @Async
    public void sendAccountReinstated(String email, String fullName) {
        String title = "Your account has been reinstated";
        String body = "<p>Hi " + safe(fullName) + ",</p>"
            + "<p>Your MineOps account has been reinstated. You can now sign in as usual.</p>";
        send(email, title, wrap(title, body));
    }

    @Async
    public void sendAccountDeleted(String email, String fullName) {
        String title = "Your account has been removed";
        String body = "<p>Hi " + safe(fullName) + ",</p>"
            + "<p>Your MineOps account has been removed by your site administrator and you will no longer be able to sign in. "
            + "If you believe this was done in error, contact your supervisor or safety officer.</p>";
        send(email, title, wrap(title, body));
    }

    @Async
    public void sendBuyerVerified(String email, String fullName) {
        String title = "Your buyer account is verified";
        String body = "<p>Hi " + safe(fullName) + ",</p>"
            + "<p>Your buyer account has been verified. You can now access the MineOps marketplace.</p>";
        send(email, title, wrap(title, body));
    }

    @Async
    public void sendBuyerRejected(String email, String fullName) {
        String title = "Buyer application not approved";
        String body = "<p>Hi " + safe(fullName) + ",</p>"
            + "<p>Your buyer account application was not approved. Contact MineOps support if you have questions.</p>";
        send(email, title, wrap(title, body));
    }

    @Async
    public void sendPasswordResetOtp(String email, String fullName, String otp, int validityMinutes) {
        String title = "Your password reset code";
        String body = "<p>Hi " + safe(fullName) + ",</p>"
            + "<p>Use the code below to reset your MineOps password. This code expires in " + validityMinutes + " minutes.</p>"
            + "<p style=\"font-size:28px;font-weight:800;letter-spacing:6px;color:#8a4b1f;margin:20px 0;\">" + otp + "</p>"
            + "<p>If you didn't request this, you can safely ignore this email — your password will not be changed.</p>";
        send(email, title, wrap(title, body));
    }

    @Async
    public void sendPayDisbursed(String email, String fullName, BigDecimal netPay, String momoNumber) {
        String title = "Your pay has been disbursed";
        String amount = netPay != null ? String.format("GHS %.2f", netPay) : "your pay";
        String body = "<p>Hi " + safe(fullName) + ",</p>"
            + "<p>" + amount + " has been sent to your mobile money account"
            + (momoNumber != null && !momoNumber.isBlank() ? " (" + safe(momoNumber) + ")" : "") + ".</p>"
            + "<p>Check your MoMo account to confirm receipt.</p>";
        send(email, title, wrap(title, body));
    }

    private static String safe(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
