package MineOpsBackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "forum_post")
public class ForumPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String category;
    private String subforum;
    private String authorEmail;
    private String authorName;
    private String authorRole;
    private String title;

    @Column(length = 3000)
    private String body;

    private int replyCount;
    private LocalDateTime createdAt;

    public ForumPost() {}

    public Long getId() { return id; }
    public String getCategory() { return category; }
    public void setCategory(String v) { this.category = v; }
    public String getSubforum() { return subforum; }
    public void setSubforum(String v) { this.subforum = v; }
    public String getAuthorEmail() { return authorEmail; }
    public void setAuthorEmail(String v) { this.authorEmail = v; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String v) { this.authorName = v; }
    public String getAuthorRole() { return authorRole; }
    public void setAuthorRole(String v) { this.authorRole = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getBody() { return body; }
    public void setBody(String v) { this.body = v; }
    public int getReplyCount() { return replyCount; }
    public void setReplyCount(int v) { this.replyCount = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
