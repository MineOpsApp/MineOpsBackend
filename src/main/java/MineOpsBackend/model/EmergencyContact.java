package MineOpsBackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "emergency_contacts")
public class EmergencyContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long workerId;

    @Column(nullable = false)
    private String contactType; // PRIMARY or BACKUP

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String relationship;

    @Column(nullable = false)
    private String phone;

    private LocalDateTime createdAt;

    public EmergencyContact() {}

    public EmergencyContact(Long workerId, String contactType, String name, String relationship, String phone) {
        this.workerId = workerId;
        this.contactType = contactType;
        this.name = name;
        this.relationship = relationship;
        this.phone = phone;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getWorkerId() { return workerId; }
    public String getContactType() { return contactType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
