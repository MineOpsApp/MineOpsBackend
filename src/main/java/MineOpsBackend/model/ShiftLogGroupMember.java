package MineOpsBackend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "shift_log_group_members")
public class ShiftLogGroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long shiftLogId;
    private String workerEmail;
    private String workerName;

    public ShiftLogGroupMember() {}

    public ShiftLogGroupMember(Long shiftLogId, String workerEmail, String workerName) {
        this.shiftLogId = shiftLogId;
        this.workerEmail = workerEmail;
        this.workerName = workerName;
    }

    public Long getId() { return id; }
    public Long getShiftLogId() { return shiftLogId; }
    public void setShiftLogId(Long v) { this.shiftLogId = v; }
    public String getWorkerEmail() { return workerEmail; }
    public void setWorkerEmail(String v) { this.workerEmail = v; }
    public String getWorkerName() { return workerName; }
    public void setWorkerName(String v) { this.workerName = v; }
}
