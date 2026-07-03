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

    public void setReply(String reply) { this.reply = reply; }
    public void setRepliedAt(LocalDateTime repliedAt) { this.repliedAt = repliedAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
}
