package com.bank.atm.dto;

import com.bank.atm.enums.FraudAlertStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class FraudAlertResolutionDto {

    @NotNull(message = "Resolution status is required")
    private FraudAlertStatus status;

    @NotBlank(message = "Resolution notes are required")
    @Size(min = 5, max = 500, message = "Resolution notes must be between 5 and 500 characters")
    private String resolutionNotes;

    private boolean freezeAccount;

    public FraudAlertResolutionDto() {}

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

    public boolean isFreezeAccount() {
        return freezeAccount;
    }

    public void setFreezeAccount(boolean freezeAccount) {
        this.freezeAccount = freezeAccount;
    }
}
