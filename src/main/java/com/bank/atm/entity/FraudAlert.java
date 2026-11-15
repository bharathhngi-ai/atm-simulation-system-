package com.bank.atm.entity;

import com.bank.atm.enums.FraudAlertSeverity;
import com.bank.atm.enums.FraudAlertStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_alerts", indexes = {
        @Index(name = "idx_fa_account", columnList = "account_id"),
        @Index(name = "idx_fa_status", columnList = "status"),
        @Index(name = "idx_fa_severity", columnList = "severity"),
        @Index(name = "idx_fa_created_at", columnList = "created_at")
})
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_code", nullable = false, unique = true, length = 36)
    private String alertCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private BankAccount account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "rule_triggered", nullable = false, length = 100)
    private String ruleTriggered;

    @Column(nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FraudAlertSeverity severity = FraudAlertSeverity.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FraudAlertStatus status = FraudAlertStatus.OPEN;

    @Column(name = "resolution_notes", length = 500)
    private String resolutionNotes;

    @Column(name = "resolved_by", length = 50)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public FraudAlert() {}

    public FraudAlert(String alertCode, BankAccount account, User user, String ruleTriggered, String description, FraudAlertSeverity severity) {
        this.alertCode = alertCode;
        this.account = account;
        this.user = user;
        this.ruleTriggered = ruleTriggered;
        this.description = description;
        this.severity = severity;
        this.status = FraudAlertStatus.OPEN;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAlertCode() {
        return alertCode;
    }

    public void setAlertCode(String alertCode) {
        this.alertCode = alertCode;
    }

    public BankAccount getAccount() {
        return account;
    }

    public void setAccount(BankAccount account) {
        this.account = account;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getRuleTriggered() {
        return ruleTriggered;
    }

    public void setRuleTriggered(String ruleTriggered) {
        this.ruleTriggered = ruleTriggered;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public FraudAlertSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(FraudAlertSeverity severity) {
        this.severity = severity;
    }

    public FraudAlertStatus getStatus() {
        return status;
    }

    public void setStatus(FraudAlertStatus status) {
        this.status = status;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
