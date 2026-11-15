package com.bank.atm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FreezeAccountDto {

    @NotBlank(message = "Reason for freezing account is required")
    @Size(min = 5, max = 200, message = "Reason must be between 5 and 200 characters")
    private String reason;

    @NotBlank(message = "ATM PIN confirmation is required to freeze account")
    private String atmPin;

    public FreezeAccountDto() {}

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getAtmPin() {
        return atmPin;
    }

    public void setAtmPin(String atmPin) {
        this.atmPin = atmPin;
    }
}
