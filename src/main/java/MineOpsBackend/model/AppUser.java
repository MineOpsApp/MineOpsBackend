package MineOpsBackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role;

    private String assignedSite;

    private LocalDateTime createdAt;

    private LocalDateTime sessionExpiresAt;

    private String guestSubRole;

    public AppUser() {
    }

    public AppUser(String fullName, String email, String passwordHash, String role) {
        this(fullName, email, passwordHash, role, "Obuasi Mine");
    }

    public AppUser(String fullName, String email, String passwordHash, String role, String assignedSite) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.assignedSite = assignedSite;
        this.createdAt = LocalDateTime.now();
        if ("guest".equals(role)) {
            this.sessionExpiresAt = LocalDateTime.now().plusHours(24);
        }
    }

    public Long getId() { return id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getAssignedSite() { return assignedSite; }
    public void setAssignedSite(String assignedSite) { this.assignedSite = assignedSite; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getSessionExpiresAt() { return sessionExpiresAt; }
    public void setSessionExpiresAt(LocalDateTime sessionExpiresAt) { this.sessionExpiresAt = sessionExpiresAt; }

    public String getGuestSubRole() { return guestSubRole; }
public void setGuestSubRole(String guestSubRole) { this.guestSubRole = guestSubRole; }



       public boolean isExpired() {
    return sessionExpiresAt != null && LocalDateTime.now(java.time.ZoneOffset.UTC).isAfter(sessionExpiresAt);
} 
}