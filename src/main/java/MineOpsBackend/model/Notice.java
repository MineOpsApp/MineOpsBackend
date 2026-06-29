package MineOpsBackend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "notices")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String message;

    private String postedByRole;

    private LocalDateTime createdAt;
    private String category;
    private java.time.LocalDateTime expiresAt;
public java.time.LocalDateTime getExpiresAt() { return expiresAt; }
public void setExpiresAt(java.time.LocalDateTime v) { this.expiresAt = v; }

    public Notice() {
    }

    public Notice(String title, String message, String postedByRole) {
        this.title = title;
        this.message = message;
        this.postedByRole = postedByRole;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPostedByRole() {
        return postedByRole;
    }

    public void setPostedByRole(String postedByRole) {
        this.postedByRole = postedByRole;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCategory() { return category; }
public void setCategory(String v) { this.category = v; }
}
