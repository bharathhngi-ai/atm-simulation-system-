package com.bank.atm.dto;

import com.bank.atm.enums.AccountType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class RegisterRequestDto {

    @NotBlank(message = "Username is required")
    @Size(min = 4, max = 30, message = "Username must be between 4 and 30 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username can only contain letters, numbers, and .-_")
    private String username;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 80, message = "Full name must be between 2 and 80 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9+ -]{10,15}$", message = "Phone number must be valid (10-15 digits)")
    private String phoneNumber;

    @NotBlank(message = "Address is required")
    @Size(min = 5, max = 150, message = "Address must be between 5 and 150 characters")
    private String address;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 50, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    @NotNull(message = "Account type is required")
    private AccountType accountType = AccountType.SAVINGS;

    @NotBlank(message = "4-digit ATM PIN is required")
    @Pattern(regexp = "^[0-9]{4}$", message = "ATM PIN must be exactly 4 digits")
    private String atmPin;

    @NotNull(message = "Initial deposit amount is required")
    @DecimalMin(value = "500.00", message = "Minimum opening deposit is ₹500.00")
    @DecimalMax(value = "1000000.00", message = "Maximum opening deposit is ₹1,000,000.00")
    private BigDecimal initialDeposit;

    public RegisterRequestDto() {}

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public String getAtmPin() {
        return atmPin;
    }

    public void setAtmPin(String atmPin) {
        this.atmPin = atmPin;
    }

    public BigDecimal getInitialDeposit() {
        return initialDeposit;
    }

    public void setInitialDeposit(BigDecimal initialDeposit) {
        this.initialDeposit = initialDeposit;
    }
}
