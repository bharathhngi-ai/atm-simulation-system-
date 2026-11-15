package com.bank.atm.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class DepositRequestDto {

    @NotNull(message = "Deposit amount is required")
    @DecimalMin(value = "10.00", message = "Minimum deposit amount is ₹10.00")
    @DecimalMax(value = "500000.00", message = "Maximum single deposit is ₹500,000.00")
    private BigDecimal amount;

    @Size(max = 100, message = "Remarks cannot exceed 100 characters")
    private String description = "ATM Cash Deposit";

    private String idempotencyKey;

    public DepositRequestDto() {}

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
