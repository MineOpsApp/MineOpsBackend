package MineOpsBackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "safety_checklists", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"worker_id", "shift_date"})
})
public class SafetyChecklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private Long workerId;
    @Column(nullable = false) private String workerName;
    @Column(nullable = false) private String workerEmail;
    @Column(nullable = false) private String site;
    @Column(nullable = false) private LocalDate shiftDate;

    @Column(nullable = false) private boolean ppeHelmet;
    @Column(nullable = false) private boolean ppeBoots;
    @Column(nullable = false) private boolean ppeGloves;
    @Column(nullable = false) private boolean ppeVest;
    @Column(nullable = false) private boolean equipmentChecked;
    @Column(nullable = false) private boolean communicationDevice;
    @Column(nullable = false) private boolean emergencyExitsClear;
    @Column(nullable = false) private boolean hazardousMaterialsSecured;

    @Column(nullable = false) private LocalDateTime submittedAt;

    public SafetyChecklist() {}

    public SafetyChecklist(
        Long workerId, String workerName, String workerEmail, String site, LocalDate shiftDate,
        boolean ppeHelmet, boolean ppeBoots, boolean ppeGloves, boolean ppeVest,
        boolean equipmentChecked, boolean communicationDevice,
        boolean emergencyExitsClear, boolean hazardousMaterialsSecured
    ) {
        this.workerId = workerId;
        this.workerName = workerName;
        this.workerEmail = workerEmail;
        this.site = site;
        this.shiftDate = shiftDate;
        this.ppeHelmet = ppeHelmet;
        this.ppeBoots = ppeBoots;
        this.ppeGloves = ppeGloves;
        this.ppeVest = ppeVest;
        this.equipmentChecked = equipmentChecked;
        this.communicationDevice = communicationDevice;
        this.emergencyExitsClear = emergencyExitsClear;
        this.hazardousMaterialsSecured = hazardousMaterialsSecured;
        this.submittedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getWorkerId() { return workerId; }
    public String getWorkerName() { return workerName; }
    public String getWorkerEmail() { return workerEmail; }
    public String getSite() { return site; }
    public LocalDate getShiftDate() { return shiftDate; }
    public boolean isPpeHelmet() { return ppeHelmet; }
    public boolean isPpeBoots() { return ppeBoots; }
    public boolean isPpeGloves() { return ppeGloves; }
    public boolean isPpeVest() { return ppeVest; }
    public boolean isEquipmentChecked() { return equipmentChecked; }
    public boolean isCommunicationDevice() { return communicationDevice; }
    public boolean isEmergencyExitsClear() { return emergencyExitsClear; }
    public boolean isHazardousMaterialsSecured() { return hazardousMaterialsSecured; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }

    public boolean isAllCleared() {
        return ppeHelmet && ppeBoots && ppeGloves && ppeVest
            && equipmentChecked && communicationDevice
            && emergencyExitsClear && hazardousMaterialsSecured;
    }

    public void update(
        boolean ppeHelmet, boolean ppeBoots, boolean ppeGloves, boolean ppeVest,
        boolean equipmentChecked, boolean communicationDevice,
        boolean emergencyExitsClear, boolean hazardousMaterialsSecured
    ) {
        this.ppeHelmet = ppeHelmet;
        this.ppeBoots = ppeBoots;
        this.ppeGloves = ppeGloves;
        this.ppeVest = ppeVest;
        this.equipmentChecked = equipmentChecked;
        this.communicationDevice = communicationDevice;
        this.emergencyExitsClear = emergencyExitsClear;
        this.hazardousMaterialsSecured = hazardousMaterialsSecured;
        this.submittedAt = LocalDateTime.now();
    }
}
