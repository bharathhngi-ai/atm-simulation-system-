package com.bank.atm.service;

import com.bank.atm.entity.BankAccount;
import com.bank.atm.entity.FraudAlert;
import com.bank.atm.entity.User;
import com.bank.atm.enums.*;
import com.bank.atm.repository.BankAccountRepository;
import com.bank.atm.repository.FraudAlertRepository;
import com.bank.atm.repository.TransactionRepository;
import com.bank.atm.util.AccountNumberGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FraudDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionService.class);

    private final FraudAlertRepository fraudAlertRepository;
    private final TransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final AccountNumberGenerator generator;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public FraudDetectionService(FraudAlertRepository fraudAlertRepository,
                                 TransactionRepository transactionRepository,
                                 BankAccountRepository bankAccountRepository,
                                 AccountNumberGenerator generator,
                                 AuditLogService auditLogService,
                                 NotificationService notificationService) {
        this.fraudAlertRepository = fraudAlertRepository;
        this.transactionRepository = transactionRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.generator = generator;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Transactional
    public FraudAlert recordFailedLoginAlert(User user, int attempts) {
        String code = generator.generateAlertCode();
        String description = String.format("User '%s' had %d consecutive failed login attempts.", user.getUsername(), attempts);
        FraudAlertSeverity severity = attempts >= 5 ? FraudAlertSeverity.CRITICAL : FraudAlertSeverity.HIGH;

        FraudAlert alert = new FraudAlert(code, user.getBankAccount(), user, "FAILED_LOGINS", description, severity);
        FraudAlert saved = fraudAlertRepository.save(alert);

        auditLogService.logAction(user.getUsername(), AuditAction.FRAUD_ALERT_GENERATED, "FraudAlert", code, description, "SUCCESS");

        if (attempts >= 5 && user.getBankAccount() != null && user.getBankAccount().isActive()) {
            BankAccount account = user.getBankAccount();
            account.setStatus(AccountStatus.FROZEN);
            account.setFrozenReason("Auto-frozen due to " + attempts + " consecutive failed login attempts.");
            account.setFrozenAt(LocalDateTime.now());
            bankAccountRepository.save(account);
            notificationService.sendNotification(user, "Security Alert: Account Frozen",
                    "Your account has been frozen due to multiple failed login attempts. Contact support to unfreeze.",
                    "DANGER", "/security/settings");
        }

        return saved;
    }

    @Transactional
    public void checkWithdrawalFraudRules(BankAccount account, BigDecimal amount) {
        // Rule 1: Unusually large withdrawal (> ₹10,000)
        if (amount.compareTo(new BigDecimal("10000.00")) >= 0) {
            createAlert(account, "LARGE_WITHDRAWAL",
                    String.format("Unusually large withdrawal of ₹%s initiated from account %s", amount, account.getAccountNumber()),
                    FraudAlertSeverity.HIGH);
        }

        // Rule 2: Multiple withdrawals within a short period (>= 3 in last 10 minutes)
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
        long recentWithdrawals = transactionRepository.countByAccountAndTypeAndCreatedAtAfter(account, TransactionType.WITHDRAWAL, tenMinutesAgo);
        if (recentWithdrawals >= 3) {
            createAlert(account, "RAPID_WITHDRAWALS",
                    String.format("Detected %d withdrawals within 10 minutes from account %s", recentWithdrawals + 1, account.getAccountNumber()),
                    FraudAlertSeverity.CRITICAL);
        }
    }

    @Transactional
    public void checkTransferFraudRules(BankAccount sourceAccount, BankAccount targetAccount, BigDecimal amount) {
        // Rule 1: Large transfer (> ₹20,000)
        if (amount.compareTo(new BigDecimal("20000.00")) >= 0) {
            createAlert(sourceAccount, "LARGE_TRANSFER",
                    String.format("High-value transfer of ₹%s from account %s to %s", amount, sourceAccount.getAccountNumber(), targetAccount.getAccountNumber()),
                    FraudAlertSeverity.HIGH);
        }

        // Rule 2: Rapid successive transfers (>= 3 in last 15 minutes)
        LocalDateTime fifteenMinutesAgo = LocalDateTime.now().minusMinutes(15);
        long recentTransfers = transactionRepository.countByAccountAndTypeAndCreatedAtAfter(sourceAccount, TransactionType.TRANSFER_DEBIT, fifteenMinutesAgo);
        if (recentTransfers >= 3) {
            createAlert(sourceAccount, "RAPID_TRANSFERS",
                    String.format("Detected %d transfers within 15 minutes from account %s", recentTransfers + 1, sourceAccount.getAccountNumber()),
                    FraudAlertSeverity.CRITICAL);
        }
    }

    @Transactional
    public void checkFailedTransactionFraudRules(BankAccount account, String operation) {
        LocalDateTime fifteenMinutesAgo = LocalDateTime.now().minusMinutes(15);
        long failedCount = transactionRepository.countByAccountAndStatusAndCreatedAtAfter(account, TransactionStatus.FAILED, fifteenMinutesAgo);
        if (failedCount >= 3) {
            createAlert(account, "REPEATED_FAILED_TRANSACTIONS",
                    String.format("Repeated %d failed transactions detected for account %s during %s", failedCount + 1, account.getAccountNumber(), operation),
                    FraudAlertSeverity.HIGH);
        }
    }

    private void createAlert(BankAccount account, String rule, String description, FraudAlertSeverity severity) {
        String code = generator.generateAlertCode();
        FraudAlert alert = new FraudAlert(code, account, account.getUser(), rule, description, severity);
        fraudAlertRepository.save(alert);
        auditLogService.logAction(account.getUser().getUsername(), AuditAction.FRAUD_ALERT_GENERATED, "FraudAlert", code, description, "SUCCESS");
        log.warn("[FRAUD ALERT] code={}, rule={}, account={}, severity={}", code, rule, account.getAccountNumber(), severity);

        notificationService.sendNotification(account.getUser(), "Security Alert Triggered",
                description, "WARNING", "/security/settings");
    }

    public List<FraudAlert> getRecentAlertsForAccount(BankAccount account) {
        return fraudAlertRepository.findTop5ByAccountOrderByCreatedAtDesc(account);
    }

    public Page<FraudAlert> getAllAlerts(FraudAlertStatus status, FraudAlertSeverity severity, Pageable pageable) {
        return fraudAlertRepository.findFilteredAlerts(status, severity, pageable);
    }

    @Transactional
    public void resolveAlert(String alertCode, FraudAlertStatus newStatus, String resolutionNotes, String adminUsername, boolean freezeAccount) {
        FraudAlert alert = fraudAlertRepository.findByAlertCode(alertCode)
                .orElseThrow(() -> new IllegalArgumentException("Fraud alert not found: " + alertCode));

        alert.setStatus(newStatus);
        alert.setResolutionNotes(resolutionNotes);
        alert.setResolvedBy(adminUsername);
        alert.setResolvedAt(LocalDateTime.now());
        fraudAlertRepository.save(alert);

        if (freezeAccount && alert.getAccount() != null && alert.getAccount().isActive()) {
            BankAccount account = alert.getAccount();
            account.setStatus(AccountStatus.FROZEN);
            account.setFrozenReason("Frozen by admin following fraud investigation: " + resolutionNotes);
            account.setFrozenAt(LocalDateTime.now());
            bankAccountRepository.save(account);
            auditLogService.logAction(adminUsername, AuditAction.ACCOUNT_FROZEN, "BankAccount", account.getAccountNumber(), "Frozen via Fraud Alert " + alertCode, "SUCCESS");
        }

        auditLogService.logAction(adminUsername, AuditAction.FRAUD_ALERT_RESOLVED, "FraudAlert", alertCode,
                "Status: " + newStatus + ", notes: " + resolutionNotes, "SUCCESS");
    }

    public long countOpenAlerts() {
        return fraudAlertRepository.countByStatus(FraudAlertStatus.OPEN);
    }
}
