package MineOpsBackend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "supervisor_site_access")
public class SupervisorSiteAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String supervisorEmail;

    @Column(nullable = false)
    private String site;

    @Column(nullable = false)
    private String grantedByEmail;

    @Column(nullable = false)
    private LocalDateTime grantedAt;

    public SupervisorSiteAccess() {}

    public Long getId() { return id; }
    public String getSupervisorEmail() { return supervisorEmail; }
    public void setSupervisorEmail(String e) { this.supervisorEmail = e; }
    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }
    public String getGrantedByEmail() { return grantedByEmail; }
    public void setGrantedByEmail(String e) { this.grantedByEmail = e; }
    public LocalDateTime getGrantedAt() { return grantedAt; }
    public void setGrantedAt(LocalDateTime t) { this.grantedAt = t; }
}
