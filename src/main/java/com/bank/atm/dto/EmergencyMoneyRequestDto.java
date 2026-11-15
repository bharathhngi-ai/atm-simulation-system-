package com.bank.atm.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class EmergencyMoneyRequestDto {

    @NotBlank(message = "Contact / Target account number is required")
    @Pattern(regexp = "^[0-9]{12}$", message = "Target account number must be 12 digits")
    private String targetAccountNumber;

    @NotNull(message = "Emergency amount is required")
    @DecimalMin(value = "5.00", message = "Minimum emergency request is ₹5.00")
    @DecimalMax(value = "25000.00", message = "Maximum emergency request is ₹25,000.00")
    private BigDecimal amount;

    @NotBlank(message = "Emergency reason is required")
    @Size(min = 5, max = 250, message = "Reason must be between 5 and 250 characters")
    private String reason;

    public EmergencyMoneyRequestDto() {}

    public String getTargetAccountNumber() {
        return targetAccountNumber;
    }

    public void setTargetAccountNumber(String targetAccountNumber) {
        this.targetAccountNumber = targetAccountNumber;
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
}
