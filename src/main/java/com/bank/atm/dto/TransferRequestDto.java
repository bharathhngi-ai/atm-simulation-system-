package com.bank.atm.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class TransferRequestDto {

    @NotBlank(message = "Recipient account number is required")
    @Pattern(regexp = "^[0-9]{12}$", message = "Recipient account number must be exactly 12 digits")
    private String recipientAccountNumber;

    @NotNull(message = "Transfer amount is required")
    @DecimalMin(value = "1.00", message = "Minimum transfer amount is ₹1.00")
    @DecimalMax(value = "100000.00", message = "Maximum single transfer is ₹100,000.00")
    private BigDecimal amount;

    @NotBlank(message = "ATM PIN is required to authorize transfer")
    @Pattern(regexp = "^[0-9]{4}$", message = "ATM PIN must be 4 digits")
    private String atmPin;

    @Size(max = 100, message = "Remarks cannot exceed 100 characters")
    private String remarks;

    private String idempotencyKey;

    public TransferRequestDto() {}

    public String getRecipientAccountNumber() {
        return recipientAccountNumber;
    }

    public void setRecipientAccountNumber(String recipientAccountNumber) {
        this.recipientAccountNumber = recipientAccountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getAtmPin() {
        return atmPin;
    }

    public void setAtmPin(String atmPin) {
        this.atmPin = atmPin;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
