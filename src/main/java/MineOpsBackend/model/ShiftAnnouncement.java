package MineOpsBackend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "shift_announcements")
public class ShiftAnnouncement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String site;

    @Column(nullable = false, length = 200)
    private String content;

    @Column(name = "created_by_name", nullable = false)
    private String createdByName;

    @Column(name = "created_by_email", nullable = false)
    private String createdByEmail;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { createdAt = LocalDateTime.now(); }

    public ShiftAnnouncement() {}

    public ShiftAnnouncement(String site, String content, String createdByName, String createdByEmail) {
        this.site = site;
        this.content = content;
        this.createdByName = createdByName;
        this.createdByEmail = createdByEmail;
    }

    public Long getId() { return id; }
    public String getSite() { return site; }
    public String getContent() { return content; }
    public String getCreatedByName() { return createdByName; }
    public String getCreatedByEmail() { return createdByEmail; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
