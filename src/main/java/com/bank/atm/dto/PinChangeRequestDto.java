package com.bank.atm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class PinChangeRequestDto {

    @NotBlank(message = "Current ATM PIN is required")
    @Pattern(regexp = "^[0-9]{4}$", message = "Current PIN must be 4 digits")
    private String currentPin;

    @NotBlank(message = "New ATM PIN is required")
    @Pattern(regexp = "^[0-9]{4}$", message = "New PIN must be 4 digits")
    private String newPin;

    @NotBlank(message = "Confirm new ATM PIN is required")
    @Pattern(regexp = "^[0-9]{4}$", message = "Confirm PIN must be 4 digits")
    private String confirmPin;

    public PinChangeRequestDto() {}

    public String getCurrentPin() {
        return currentPin;
    }

    public void setCurrentPin(String currentPin) {
        this.currentPin = currentPin;
    }

    public String getNewPin() {
        return newPin;
    }

    public void setNewPin(String newPin) {
        this.newPin = newPin;
    }

    public String getConfirmPin() {
        return confirmPin;
    }

    public void setConfirmPin(String confirmPin) {
        this.confirmPin = confirmPin;
    }
}
