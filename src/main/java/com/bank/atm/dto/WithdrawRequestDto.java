package com.bank.atm.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class WithdrawRequestDto {

    @NotNull(message = "Withdrawal amount is required")
    @DecimalMin(value = "10.00", message = "Minimum withdrawal is ₹10.00")
    @DecimalMax(value = "50000.00", message = "Maximum single withdrawal is ₹50,000.00")
    private BigDecimal amount;

    @NotBlank(message = "ATM PIN is required to authorize withdrawal")
    @Pattern(regexp = "^[0-9]{4}$", message = "ATM PIN must be 4 digits")
    private String atmPin;

    private String idempotencyKey;

    public WithdrawRequestDto() {}

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

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
