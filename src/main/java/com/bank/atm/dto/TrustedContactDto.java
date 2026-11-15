package com.bank.atm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class TrustedContactDto {

    @NotBlank(message = "Contact account number is required")
    @Pattern(regexp = "^[0-9]{12}$", message = "Account number must be 12 digits")
    private String contactAccountNumber;

    @NotBlank(message = "Nickname / Contact name is required")
    @Size(min = 2, max = 50, message = "Nickname must be between 2 and 50 characters")
    private String nickname;

    @Size(max = 50, message = "Relationship cannot exceed 50 characters")
    private String relationship;

    public TrustedContactDto() {}

    public String getContactAccountNumber() {
        return contactAccountNumber;
    }

    public void setContactAccountNumber(String contactAccountNumber) {
        this.contactAccountNumber = contactAccountNumber;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }
}
