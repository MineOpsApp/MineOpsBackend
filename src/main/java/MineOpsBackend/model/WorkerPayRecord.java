package MineOpsBackend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "worker_pay_record")
public class WorkerPayRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long payCycleId;
    private String workerEmail;
    private String workerName;
    private BigDecimal hoursWorked;
    private BigDecimal grossShare;
    private BigDecimal insuranceDeduction;
    private BigDecimal netPay;
    private String momoNumber;
    private String momoNetwork;
    private String disbursementStatus; // PENDING | SENT | FAILED
    private String momoTransactionRef;
    private String failureReason;
    private LocalDateTime disbursedAt;

    public WorkerPayRecord() {}

    public WorkerPayRecord(Long payCycleId, String workerEmail, String workerName,
                           BigDecimal hoursWorked, BigDecimal grossShare,
                           BigDecimal insuranceDeduction, BigDecimal netPay,
                           String momoNumber, String momoNetwork) {
        this.payCycleId = payCycleId;
        this.workerEmail = workerEmail;
        this.workerName = workerName;
        this.hoursWorked = hoursWorked;
        this.grossShare = grossShare;
        this.insuranceDeduction = insuranceDeduction;
        this.netPay = netPay;
        this.momoNumber = momoNumber;
        this.momoNetwork = momoNetwork;
        this.disbursementStatus = "PENDING";
    }

    public Long getId() { return id; }
    public Long getPayCycleId() { return payCycleId; }
    public void setPayCycleId(Long v) { this.payCycleId = v; }
    public String getWorkerEmail() { return workerEmail; }
    public void setWorkerEmail(String v) { this.workerEmail = v; }
    public String getWorkerName() { return workerName; }
    public void setWorkerName(String v) { this.workerName = v; }
    public BigDecimal getHoursWorked() { return hoursWorked; }
    public void setHoursWorked(BigDecimal v) { this.hoursWorked = v; }
    public BigDecimal getGrossShare() { return grossShare; }
    public void setGrossShare(BigDecimal v) { this.grossShare = v; }
    public BigDecimal getInsuranceDeduction() { return insuranceDeduction; }
    public void setInsuranceDeduction(BigDecimal v) { this.insuranceDeduction = v; }
    public BigDecimal getNetPay() { return netPay; }
    public void setNetPay(BigDecimal v) { this.netPay = v; }
    public String getMomoNumber() { return momoNumber; }
    public void setMomoNumber(String v) { this.momoNumber = v; }
    public String getMomoNetwork() { return momoNetwork; }
    public void setMomoNetwork(String v) { this.momoNetwork = v; }
    public String getDisbursementStatus() { return disbursementStatus; }
    public void setDisbursementStatus(String v) { this.disbursementStatus = v; }
    public String getMomoTransactionRef() { return momoTransactionRef; }
    public void setMomoTransactionRef(String v) { this.momoTransactionRef = v; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String v) { this.failureReason = v; }
    public LocalDateTime getDisbursedAt() { return disbursedAt; }
    public void setDisbursedAt(LocalDateTime v) { this.disbursedAt = v; }
}
