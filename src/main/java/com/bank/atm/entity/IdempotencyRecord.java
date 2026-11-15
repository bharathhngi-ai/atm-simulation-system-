package com.bank.atm.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_records", indexes = {
        @Index(name = "idx_idem_key", columnList = "idempotency_key")
})
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "operation_type", nullable = false, length = 50)
    private String operationType;

    @Column(name = "user_identifier", nullable = false, length = 50)
    private String userIdentifier;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "COMPLETED";

    @Column(name = "result_message", length = 255)
    private String resultMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public IdempotencyRecord() {}

    public IdempotencyRecord(String idempotencyKey, String operationType, String userIdentifier, String status, String resultMessage) {
        this.idempotencyKey = idempotencyKey;
        this.operationType = operationType;
        this.userIdentifier = userIdentifier;
        this.status = status;
        this.resultMessage = resultMessage;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getUserIdentifier() {
        return userIdentifier;
    }

    public void setUserIdentifier(String userIdentifier) {
        this.userIdentifier = userIdentifier;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
