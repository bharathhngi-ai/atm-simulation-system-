package com.bank.atm.service;

import com.bank.atm.dto.DepositRequestDto;
import com.bank.atm.dto.TransferRequestDto;
import com.bank.atm.dto.WithdrawRequestDto;
import com.bank.atm.entity.BankAccount;
import com.bank.atm.entity.IdempotencyRecord;
import com.bank.atm.entity.Transaction;
import com.bank.atm.enums.*;
import com.bank.atm.exception.*;
import com.bank.atm.repository.BankAccountRepository;
import com.bank.atm.repository.IdempotencyRecordRepository;
import com.bank.atm.repository.TransactionRepository;
import com.bank.atm.util.AccountNumberGenerator;
import com.bank.atm.util.MoneyUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class TransactionService {

    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final AccountService accountService;
    private final FraudDetectionService fraudDetectionService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final AccountNumberGenerator generator;

    public TransactionService(BankAccountRepository bankAccountRepository,
                              TransactionRepository transactionRepository,
                              IdempotencyRecordRepository idempotencyRecordRepository,
                              AccountService accountService,
                              FraudDetectionService fraudDetectionService,
                              AuditLogService auditLogService,
                              NotificationService notificationService,
                              AccountNumberGenerator generator) {
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.accountService = accountService;
        this.fraudDetectionService = fraudDetectionService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.generator = generator;
    }

    private void checkIdempotency(String key, String operation, String user) {
        if (key != null && !key.isBlank()) {
            if (idempotencyRecordRepository.existsByIdempotencyKey(key)) {
                throw new DuplicateTransactionException("Duplicate transaction detected. Request has already been processed.");
            }
        }
    }

    private void saveIdempotency(String key, String operation, String user, String resultMsg) {
        if (key != null && !key.isBlank()) {
            idempotencyRecordRepository.save(new IdempotencyRecord(key, operation, user, "SUCCESS", resultMsg));
        }
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Transaction deposit(String username, DepositRequestDto dto) {
        checkIdempotency(dto.getIdempotencyKey(), "DEPOSIT", username);

        BankAccount account = bankAccountRepository.findByUserUsername(username)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for user: " + username));

        if (account.isFrozen()) {
            throw new AccountFrozenException("Cannot deposit to a frozen account. Please unfreeze first.");
        }

        BigDecimal amount = MoneyUtils.formatAmount(dto.getAmount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Deposit amount must be greater than zero.");
        }

        // Lock row for update
        BankAccount lockedAccount = bankAccountRepository.findByIdForUpdate(account.getId())
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        BigDecimal balanceBefore = lockedAccount.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);
        lockedAccount.setBalance(balanceAfter);
        bankAccountRepository.save(lockedAccount);

        Transaction tx = new Transaction();
        tx.setReferenceNumber(generator.generateReferenceNumber("DEP"));
        tx.setAccount(lockedAccount);
        tx.setType(TransactionType.DEPOSIT);
        tx.setAmount(amount);
        tx.setBalanceBefore(balanceBefore);
        tx.setBalanceAfter(balanceAfter);
        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setDescription(dto.getDescription() != null && !dto.getDescription().isBlank() ? dto.getDescription() : "ATM Cash Deposit");
        tx.setAtmLocation("Main ATM Terminal #01");
        Transaction savedTx = transactionRepository.save(tx);

        saveIdempotency(dto.getIdempotencyKey(), "DEPOSIT", username, "Deposited " + amount);

        auditLogService.logAction(username, AuditAction.DEPOSIT, "Transaction", savedTx.getReferenceNumber(),
                "Deposited ₹" + amount + ". New balance: ₹" + balanceAfter, "SUCCESS");

        notificationService.sendNotification(lockedAccount.getUser(), "Cash Deposit Successful",
                "Your account " + lockedAccount.getAccountNumber() + " was credited with ₹" + amount + ". Ref: " + savedTx.getReferenceNumber(),
                "SUCCESS", "/transactions/receipt/" + savedTx.getId());

        return savedTx;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Transaction withdraw(String username, WithdrawRequestDto dto) {
        checkIdempotency(dto.getIdempotencyKey(), "WITHDRAWAL", username);

        BankAccount account = bankAccountRepository.findByUserUsername(username)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for user: " + username));

        if (account.isFrozen()) {
            throw new AccountFrozenException("Account is frozen. Withdrawals are currently blocked.");
        }

        // Verify PIN
        try {
            accountService.verifyPin(account, dto.getAtmPin());
        } catch (InvalidPinException ex) {
            fraudDetectionService.checkFailedTransactionFraudRules(account, "WITHDRAWAL");
            auditLogService.logAction(username, AuditAction.WITHDRAWAL, "BankAccount", account.getAccountNumber(),
                    "Invalid PIN entered for withdrawal attempt", "FAILURE");
            throw ex;
        }

        BigDecimal amount = MoneyUtils.formatAmount(dto.getAmount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Withdrawal amount must be greater than zero.");
        }

        // Check Daily Withdrawal Limit
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        BigDecimal todayWithdrawn = transactionRepository.sumAmountByAccountAndTypeSince(account, TransactionType.WITHDRAWAL, startOfDay);
        if (todayWithdrawn.add(amount).compareTo(account.getDailyWithdrawalLimit()) > 0) {
            throw new BankingException(String.format("Daily withdrawal limit of ₹%s exceeded. Already withdrawn today: ₹%s",
                    account.getDailyWithdrawalLimit(), todayWithdrawn));
        }

        // Lock row for balance deduction
        BankAccount lockedAccount = bankAccountRepository.findByIdForUpdate(account.getId())
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        if (lockedAccount.getBalance().compareTo(amount) < 0) {
            auditLogService.logAction(username, AuditAction.WITHDRAWAL, "BankAccount", account.getAccountNumber(),
                    "Insufficient balance for withdrawal: requested ₹" + amount + ", available ₹" + lockedAccount.getBalance(), "FAILURE");
            throw new InsufficientBalanceException(String.format("Insufficient funds. Available balance is ₹%s", lockedAccount.getBalance()));
        }

        // Fraud Rules Check
        fraudDetectionService.checkWithdrawalFraudRules(lockedAccount, amount);

        BigDecimal balanceBefore = lockedAccount.getBalance();
        BigDecimal balanceAfter = balanceBefore.subtract(amount);
        lockedAccount.setBalance(balanceAfter);
        bankAccountRepository.save(lockedAccount);

        Transaction tx = new Transaction();
        tx.setReferenceNumber(generator.generateReferenceNumber("WTH"));
        tx.setAccount(lockedAccount);
        tx.setType(TransactionType.WITHDRAWAL);
        tx.setAmount(amount);
        tx.setBalanceBefore(balanceBefore);
        tx.setBalanceAfter(balanceAfter);
        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setDescription("ATM Cash Withdrawal");
        tx.setAtmLocation("Main ATM Terminal #01");
        Transaction savedTx = transactionRepository.save(tx);

        saveIdempotency(dto.getIdempotencyKey(), "WITHDRAWAL", username, "Withdrawn " + amount);

        auditLogService.logAction(username, AuditAction.WITHDRAWAL, "Transaction", savedTx.getReferenceNumber(),
                "Withdrew ₹" + amount + ". New balance: ₹" + balanceAfter, "SUCCESS");

        notificationService.sendNotification(lockedAccount.getUser(), "Cash Withdrawal Successful",
                "Dispensed ₹" + amount + " from account " + lockedAccount.getAccountNumber() + ". Ref: " + savedTx.getReferenceNumber(),
                "SUCCESS", "/transactions/receipt/" + savedTx.getId());

        return savedTx;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Transaction transfer(String senderUsername, TransferRequestDto dto) {
        checkIdempotency(dto.getIdempotencyKey(), "TRANSFER", senderUsername);

        BankAccount senderAccount = bankAccountRepository.findByUserUsername(senderUsername)
                .orElseThrow(() -> new AccountNotFoundException("Sender account not found for user: " + senderUsername));

        if (senderAccount.isFrozen()) {
            throw new AccountFrozenException("Your account is frozen. Outgoing transfers are blocked.");
        }

        if (senderAccount.getAccountNumber().equals(dto.getRecipientAccountNumber())) {
            throw new BankingException("Cannot transfer money to your own account.");
        }

        // Verify PIN
        try {
            accountService.verifyPin(senderAccount, dto.getAtmPin());
        } catch (InvalidPinException ex) {
            fraudDetectionService.checkFailedTransactionFraudRules(senderAccount, "TRANSFER");
            auditLogService.logAction(senderUsername, AuditAction.TRANSFER, "BankAccount", senderAccount.getAccountNumber(),
                    "Invalid PIN entered for transfer attempt to " + dto.getRecipientAccountNumber(), "FAILURE");
            throw ex;
        }

        BankAccount recipientAccount = bankAccountRepository.findByAccountNumber(dto.getRecipientAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("Recipient account " + dto.getRecipientAccountNumber() + " does not exist."));

        if (!recipientAccount.isActive()) {
            throw new AccountFrozenException("Recipient account is currently inactive or frozen and cannot receive funds.");
        }

        BigDecimal amount = MoneyUtils.formatAmount(dto.getAmount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Transfer amount must be greater than zero.");
        }

        // Check Daily Transfer Limit
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        BigDecimal todayTransferred = transactionRepository.sumAmountByAccountAndTypeSince(senderAccount, TransactionType.TRANSFER_DEBIT, startOfDay);
        if (todayTransferred.add(amount).compareTo(senderAccount.getDailyTransferLimit()) > 0) {
            throw new BankingException(String.format("Daily transfer limit of ₹%s exceeded. Already transferred today: ₹%s",
                    senderAccount.getDailyTransferLimit(), todayTransferred));
        }

        // Atomic row locking in consistent ID order to prevent database deadlocks
        Long firstId = senderAccount.getId().compareTo(recipientAccount.getId()) < 0 ? senderAccount.getId() : recipientAccount.getId();
        Long secondId = senderAccount.getId().compareTo(recipientAccount.getId()) < 0 ? recipientAccount.getId() : senderAccount.getId();

        BankAccount locked1 = bankAccountRepository.findByIdForUpdate(firstId).orElseThrow();
        BankAccount locked2 = bankAccountRepository.findByIdForUpdate(secondId).orElseThrow();

        BankAccount lockedSender = locked1.getId().equals(senderAccount.getId()) ? locked1 : locked2;
        BankAccount lockedRecipient = locked1.getId().equals(recipientAccount.getId()) ? locked1 : locked2;

        if (lockedSender.getBalance().compareTo(amount) < 0) {
            auditLogService.logAction(senderUsername, AuditAction.TRANSFER, "BankAccount", senderAccount.getAccountNumber(),
                    "Insufficient balance for transfer of ₹" + amount + " to " + recipientAccount.getAccountNumber(), "FAILURE");
            throw new InsufficientBalanceException("Insufficient balance for this transfer. Available: ₹" + lockedSender.getBalance());
        }

        // Fraud Rules Check
        fraudDetectionService.checkTransferFraudRules(lockedSender, lockedRecipient, amount);

        // Deduct sender balance
        BigDecimal senderBefore = lockedSender.getBalance();
        BigDecimal senderAfter = senderBefore.subtract(amount);
        lockedSender.setBalance(senderAfter);
        bankAccountRepository.save(lockedSender);

        // Credit recipient balance
        BigDecimal recipientBefore = lockedRecipient.getBalance();
        BigDecimal recipientAfter = recipientBefore.add(amount);
        lockedRecipient.setBalance(recipientAfter);
        bankAccountRepository.save(lockedRecipient);

        String refDebit = generator.generateReferenceNumber("TRF-OUT");
        String refCredit = generator.generateReferenceNumber("TRF-IN");

        String remarks = (dto.getRemarks() != null && !dto.getRemarks().isBlank()) ? dto.getRemarks() : "Funds Transfer";

        // Sender Debit Transaction
        Transaction debitTx = new Transaction();
        debitTx.setReferenceNumber(refDebit);
        debitTx.setAccount(lockedSender);
        debitTx.setTargetAccount(lockedRecipient);
        debitTx.setType(TransactionType.TRANSFER_DEBIT);
        debitTx.setAmount(amount);
        debitTx.setBalanceBefore(senderBefore);
        debitTx.setBalanceAfter(senderAfter);
        debitTx.setStatus(TransactionStatus.SUCCESS);
        debitTx.setDescription(remarks + " to " + lockedRecipient.getAccountNumber() + " (" + lockedRecipient.getUser().getFullName() + ")");
        debitTx.setAtmLocation("Online Banking Transfer");
        Transaction savedDebitTx = transactionRepository.save(debitTx);

        // Recipient Credit Transaction
        Transaction creditTx = new Transaction();
        creditTx.setReferenceNumber(refCredit);
        creditTx.setAccount(lockedRecipient);
        creditTx.setTargetAccount(lockedSender);
        creditTx.setType(TransactionType.TRANSFER_CREDIT);
        creditTx.setAmount(amount);
        creditTx.setBalanceBefore(recipientBefore);
        creditTx.setBalanceAfter(recipientAfter);
        creditTx.setStatus(TransactionStatus.SUCCESS);
        creditTx.setDescription(remarks + " from " + lockedSender.getAccountNumber() + " (" + lockedSender.getUser().getFullName() + ")");
        creditTx.setAtmLocation("Online Banking Transfer");
        transactionRepository.save(creditTx);

        saveIdempotency(dto.getIdempotencyKey(), "TRANSFER", senderUsername, "Transferred " + amount + " to " + dto.getRecipientAccountNumber());

        auditLogService.logAction(senderUsername, AuditAction.TRANSFER, "Transaction", savedDebitTx.getReferenceNumber(),
                "Transferred ₹" + amount + " to " + lockedRecipient.getAccountNumber(), "SUCCESS");

        // Notifications
        notificationService.sendNotification(lockedSender.getUser(), "Transfer Sent",
                "Sent ₹" + amount + " to " + lockedRecipient.getUser().getFullName() + " (" + lockedRecipient.getAccountNumber() + "). Ref: " + savedDebitTx.getReferenceNumber(),
                "SUCCESS", "/transactions/receipt/" + savedDebitTx.getId());

        notificationService.sendNotification(lockedRecipient.getUser(), "Money Received",
                "Received ₹" + amount + " from " + lockedSender.getUser().getFullName() + " (" + lockedSender.getAccountNumber() + "). Ref: " + refCredit,
                "SUCCESS", "/dashboard");

        return savedDebitTx;
    }

    public List<Transaction> getRecentTransactions(BankAccount account) {
        return transactionRepository.findTop10ByAccountOrTargetAccountOrderByCreatedAtDesc(account, account);
    }

    public Page<Transaction> getTransactionHistory(BankAccount account, TransactionType type, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        return transactionRepository.findFilteredTransactions(account, type, start, end, pageable);
    }

    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new BankingException("Transaction receipt not found for ID: " + id));
    }
}
