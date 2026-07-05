package MineOpsBackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "forum_reply")
public class ForumReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long postId;
    private String authorEmail;
    private String authorName;
    private String authorRole;

    @Column(length = 2000)
    private String body;

    private LocalDateTime createdAt;

    public ForumReply() {}

    public Long getId() { return id; }
    public Long getPostId() { return postId; }
    public void setPostId(Long v) { this.postId = v; }
    public String getAuthorEmail() { return authorEmail; }
    public void setAuthorEmail(String v) { this.authorEmail = v; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String v) { this.authorName = v; }
    public String getAuthorRole() { return authorRole; }
    public void setAuthorRole(String v) { this.authorRole = v; }
    public String getBody() { return body; }
    public void setBody(String v) { this.body = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
