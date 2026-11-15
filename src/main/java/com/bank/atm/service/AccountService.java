package com.bank.atm.service;

import com.bank.atm.dto.PinChangeRequestDto;
import com.bank.atm.entity.BankAccount;
import com.bank.atm.enums.AccountStatus;
import com.bank.atm.enums.AuditAction;
import com.bank.atm.exception.AccountFrozenException;
import com.bank.atm.exception.AccountNotFoundException;
import com.bank.atm.exception.BankingException;
import com.bank.atm.exception.InvalidPinException;
import com.bank.atm.repository.BankAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class AccountService {

    private final BankAccountRepository bankAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public AccountService(BankAccountRepository bankAccountRepository,
                          PasswordEncoder passwordEncoder,
                          AuditLogService auditLogService,
                          NotificationService notificationService) {
        this.bankAccountRepository = bankAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    public BankAccount getAccountByUsername(String username) {
        return bankAccountRepository.findByUserUsername(username)
                .orElseThrow(() -> new AccountNotFoundException("Bank account not found for user: " + username));
    }

    public BankAccount getAccountByNumber(String accountNumber) {
        return bankAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account number not found: " + accountNumber));
    }

    public BigDecimal getBalance(String username) {
        BankAccount account = getAccountByUsername(username);
        auditLogService.logAction(username, AuditAction.BALANCE_INQUIRY, "BankAccount", account.getAccountNumber(),
                "Balance inquiry: ₹" + account.getBalance(), "SUCCESS");
        return account.getBalance();
    }

    @Transactional
    public void changePin(String username, PinChangeRequestDto dto) {
        BankAccount account = getAccountByUsername(username);

        if (!passwordEncoder.matches(dto.getCurrentPin(), account.getAtmPin())) {
            auditLogService.logAction(username, AuditAction.PIN_CHANGE, "BankAccount", account.getAccountNumber(),
                    "Invalid current PIN entered during PIN change", "FAILURE");
            throw new InvalidPinException("Current ATM PIN is incorrect");
        }

        if (!dto.getNewPin().equals(dto.getConfirmPin())) {
            throw new BankingException("New PIN and confirmation PIN do not match");
        }

        if (passwordEncoder.matches(dto.getNewPin(), account.getAtmPin())) {
            throw new BankingException("New PIN cannot be the same as your current PIN");
        }

        account.setAtmPin(passwordEncoder.encode(dto.getNewPin()));
        bankAccountRepository.save(account);

        auditLogService.logAction(username, AuditAction.PIN_CHANGE, "BankAccount", account.getAccountNumber(),
                "ATM PIN successfully changed", "SUCCESS");

        notificationService.sendNotification(account.getUser(), "ATM PIN Updated",
                "Your ATM security PIN was successfully changed. If this was not you, please contact support immediately.",
                "WARNING", "/security/settings");
    }

    public void verifyPin(BankAccount account, String pin) {
        if (!passwordEncoder.matches(pin, account.getAtmPin())) {
            throw new InvalidPinException("Invalid 4-digit ATM PIN entered.");
        }
    }

    @Transactional
    public void freezeAccount(String username, String reason, String pin) {
        BankAccount account = getAccountByUsername(username);
        verifyPin(account, pin);

        if (account.isFrozen()) {
            throw new BankingException("Account is already frozen.");
        }

        account.setStatus(AccountStatus.FROZEN);
        account.setFrozenReason(reason);
        account.setFrozenAt(LocalDateTime.now());
        bankAccountRepository.save(account);

        auditLogService.logAction(username, AuditAction.ACCOUNT_FROZEN, "BankAccount", account.getAccountNumber(),
                "User froze account: " + reason, "SUCCESS");

        notificationService.sendNotification(account.getUser(), "Account Frozen",
                "Your account " + account.getAccountNumber() + " has been frozen. Outgoing transactions are now blocked.",
                "DANGER", "/security/settings");
    }

    @Transactional
    public void unfreezeAccount(String username, String pin) {
        BankAccount account = getAccountByUsername(username);
        verifyPin(account, pin);

        if (account.isActive()) {
            throw new BankingException("Account is already active.");
        }

        account.setStatus(AccountStatus.ACTIVE);
        account.setFrozenReason(null);
        account.setFrozenAt(null);
        bankAccountRepository.save(account);

        auditLogService.logAction(username, AuditAction.ACCOUNT_UNFROZEN, "BankAccount", account.getAccountNumber(),
                "User unfroze account successfully", "SUCCESS");

        notificationService.sendNotification(account.getUser(), "Account Unfrozen",
                "Your account " + account.getAccountNumber() + " is now active. All ATM and banking services are restored.",
                "SUCCESS", "/dashboard");
    }

    @Transactional
    public void adminToggleAccountFreeze(String accountNumber, String adminUsername, String reason) {
        BankAccount account = getAccountByNumber(accountNumber);
        if (account.isActive()) {
            account.setStatus(AccountStatus.FROZEN);
            account.setFrozenReason(reason);
            account.setFrozenAt(LocalDateTime.now());
            auditLogService.logAction(adminUsername, AuditAction.ACCOUNT_FROZEN, "BankAccount", account.getAccountNumber(),
                    "Admin froze account: " + reason, "SUCCESS");
            notificationService.sendNotification(account.getUser(), "Account Suspended by Admin",
                    "Your account has been frozen by banking administration: " + reason, "DANGER", "/security/settings");
        } else {
            account.setStatus(AccountStatus.ACTIVE);
            account.setFrozenReason(null);
            account.setFrozenAt(null);
            auditLogService.logAction(adminUsername, AuditAction.ACCOUNT_UNFROZEN, "BankAccount", account.getAccountNumber(),
                    "Admin unfroze account: " + reason, "SUCCESS");
            notificationService.sendNotification(account.getUser(), "Account Restored by Admin",
                    "Your account status was restored to ACTIVE by administration.", "SUCCESS", "/dashboard");
        }
        bankAccountRepository.save(account);
    }
}
