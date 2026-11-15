package com.bank.atm.dto;

import jakarta.validation.constraints.NotBlank;

public class UnfreezeAccountDto {

    @NotBlank(message = "ATM PIN confirmation is required to unfreeze account")
    private String atmPin;

    public UnfreezeAccountDto() {}

    public String getAtmPin() {
        return atmPin;
    }

    public void setAtmPin(String atmPin) {
        this.atmPin = atmPin;
    }
}
