package MineOpsBackend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "worker_messages")
public class WorkerMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_email", nullable = false)
    private String senderEmail;

    @Column(name = "sender_name", nullable = false)
    private String senderName;

    @Column(nullable = false)
    private String site;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String reply;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "recipient_email")
    private String recipientEmail;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(name = "initiated_by", nullable = false)
    private String initiatedBy = "WORKER";

    @Column(name = "replied_by_email")
    private String repliedByEmail;

    @Column(name = "replied_by_name")
    private String repliedByName;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public WorkerMessage() {}

    public WorkerMessage(String senderEmail, String senderName, String site, String content) {
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.site = site;
        this.content = content;
        this.initiatedBy = "WORKER";
    }

    public WorkerMessage(String senderEmail, String senderName, String site, String content,
                         String recipientEmail, String recipientName) {
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.site = site;
        this.content = content;
        this.recipientEmail = recipientEmail;
        this.recipientName = recipientName;
        this.initiatedBy = "STAFF";
    }

    public Long getId() { return id; }
    public String getSenderEmail() { return senderEmail; }
    public String getSenderName() { return senderName; }
    public String getSite() { return site; }
    public String getContent() { return content; }
    public String getReply() { return reply; }
    public LocalDateTime getRepliedAt() { return repliedAt; }
    public LocalDateTime getReadAt() { return readAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getRecipientEmail() { return recipientEmail; }
    public String getRecipientName() { return recipientName; }
    public String getInitiatedBy() { return initiatedBy; }
    public String getRepliedByEmail() { return repliedByEmail; }
    public String getRepliedByName() { return repliedByName; }

    public void setInitiatedBy(String initiatedBy) { this.initiatedBy = initiatedBy; }
    public void setReply(String reply) { this.reply = reply; }
    public void setRepliedAt(LocalDateTime repliedAt) { this.repliedAt = repliedAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
    public void setRepliedByEmail(String repliedByEmail) { this.repliedByEmail = repliedByEmail; }
    public void setRepliedByName(String repliedByName) { this.repliedByName = repliedByName; }
}
