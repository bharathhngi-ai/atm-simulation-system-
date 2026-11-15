package com.bank.atm.entity;

import com.bank.atm.enums.EmergencyRequestStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "emergency_requests", indexes = {
        @Index(name = "idx_er_requester", columnList = "requester_account_id"),
        @Index(name = "idx_er_target", columnList = "target_account_id"),
        @Index(name = "idx_er_status", columnList = "status")
})
public class EmergencyRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_code", nullable = false, unique = true, length = 36)
    private String requestCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_account_id", nullable = false)
    private BankAccount requesterAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_account_id", nullable = false)
    private BankAccount targetAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmergencyRequestStatus status = EmergencyRequestStatus.PENDING;

    @Column(name = "decision_note", length = 255)
    private String decisionNote;

    @Column(name = "transaction_reference", length = 36)
    private String transactionReference;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public EmergencyRequest() {}

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.expiresAt == null) {
            this.expiresAt = this.createdAt.plusHours(24);
        }
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt) && this.status == EmergencyRequestStatus.PENDING;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(String requestCode) {
        this.requestCode = requestCode;
    }

    public BankAccount getRequesterAccount() {
        return requesterAccount;
    }

    public void setRequesterAccount(BankAccount requesterAccount) {
        this.requesterAccount = requesterAccount;
    }

    public BankAccount getTargetAccount() {
        return targetAccount;
    }

    public void setTargetAccount(BankAccount targetAccount) {
        this.targetAccount = targetAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public EmergencyRequestStatus getStatus() {
        return status;
    }

    public void setStatus(EmergencyRequestStatus status) {
        this.status = status;
    }

    public String getDecisionNote() {
        return decisionNote;
    }

    public void setDecisionNote(String decisionNote) {
        this.decisionNote = decisionNote;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
