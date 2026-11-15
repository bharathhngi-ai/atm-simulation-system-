package com.bank.atm.service;

import com.bank.atm.dto.EmergencyMoneyRequestDto;
import com.bank.atm.entity.BankAccount;
import com.bank.atm.entity.EmergencyRequest;
import com.bank.atm.entity.Transaction;
import com.bank.atm.enums.AccountStatus;
import com.bank.atm.enums.AuditAction;
import com.bank.atm.enums.EmergencyRequestStatus;
import com.bank.atm.enums.TransactionStatus;
import com.bank.atm.enums.TransactionType;
import com.bank.atm.exception.AccountFrozenException;
import com.bank.atm.exception.AccountNotFoundException;
import com.bank.atm.exception.BankingException;
import com.bank.atm.exception.InsufficientBalanceException;
import com.bank.atm.repository.BankAccountRepository;
import com.bank.atm.repository.EmergencyRequestRepository;
import com.bank.atm.repository.TransactionRepository;
import com.bank.atm.util.AccountNumberGenerator;
import com.bank.atm.util.MoneyUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmergencyMoneyService {

    private final EmergencyRequestRepository emergencyRequestRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final AccountNumberGenerator generator;

    public EmergencyMoneyService(EmergencyRequestRepository emergencyRequestRepository,
                                 BankAccountRepository bankAccountRepository,
                                 TransactionRepository transactionRepository,
                                 AccountService accountService,
                                 AuditLogService auditLogService,
                                 NotificationService notificationService,
                                 AccountNumberGenerator generator) {
        this.emergencyRequestRepository = emergencyRequestRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.generator = generator;
    }

    @Transactional
    public EmergencyRequest createRequest(String requesterUsername, EmergencyMoneyRequestDto dto) {
        BankAccount requesterAccount = bankAccountRepository.findByUserUsername(requesterUsername)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for user: " + requesterUsername));

        if (requesterAccount.isFrozen()) {
            throw new AccountFrozenException("Your account is frozen. Cannot create emergency requests.");
        }

        if (requesterAccount.getAccountNumber().equals(dto.getTargetAccountNumber())) {
            throw new BankingException("You cannot request emergency funds from your own account.");
        }

        BankAccount targetAccount = bankAccountRepository.findByAccountNumber(dto.getTargetAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("Target account " + dto.getTargetAccountNumber() + " not found."));

        if (!targetAccount.isActive()) {
            throw new BankingException("Target recipient account is not active.");
        }

        BigDecimal amount = MoneyUtils.formatAmount(dto.getAmount());

        EmergencyRequest request = new EmergencyRequest();
        request.setRequestCode(generator.generateRequestCode());
        request.setRequesterAccount(requesterAccount);
        request.setTargetAccount(targetAccount);
        request.setAmount(amount);
        request.setReason(dto.getReason());
        request.setStatus(EmergencyRequestStatus.PENDING);
        request.setExpiresAt(LocalDateTime.now().plusHours(24));

        EmergencyRequest saved = emergencyRequestRepository.save(request);

        auditLogService.logAction(requesterUsername, AuditAction.EMERGENCY_REQUEST_CREATED, "EmergencyRequest", saved.getRequestCode(),
                "Requested ₹" + amount + " from " + targetAccount.getAccountNumber() + " - Reason: " + dto.getReason(), "SUCCESS");

        notificationService.sendNotification(targetAccount.getUser(), "Emergency Money Request Received",
                String.format("%s (%s) requested an emergency transfer of ₹%s. Reason: %s",
                        requesterAccount.getUser().getFullName(), requesterAccount.getAccountNumber(), amount, dto.getReason()),
                "WARNING", "/emergency/received");

        return saved;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Transaction approveRequest(String approverUsername, Long requestId, String atmPin, String note) {
        EmergencyRequest request = emergencyRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new BankingException("Emergency request not found"));

        BankAccount approverAccount = bankAccountRepository.findByUserUsername(approverUsername)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for user: " + approverUsername));

        if (!request.getTargetAccount().getId().equals(approverAccount.getId())) {
            throw new BankingException("You are not authorized to approve this request.");
        }

        if (request.getStatus() != EmergencyRequestStatus.PENDING) {
            throw new BankingException("Request has already been processed with status: " + request.getStatus());
        }

        if (request.isExpired()) {
            request.setStatus(EmergencyRequestStatus.EXPIRED);
            emergencyRequestRepository.save(request);
            throw new BankingException("This emergency money request has expired.");
        }

        if (approverAccount.isFrozen()) {
            throw new AccountFrozenException("Your account is frozen. You cannot approve transfers.");
        }

        // Verify PIN
        accountService.verifyPin(approverAccount, atmPin);

        BigDecimal amount = request.getAmount();

        // Lock accounts in consistent order
        BankAccount requesterAccount = request.getRequesterAccount();
        Long firstId = approverAccount.getId().compareTo(requesterAccount.getId()) < 0 ? approverAccount.getId() : requesterAccount.getId();
        Long secondId = approverAccount.getId().compareTo(requesterAccount.getId()) < 0 ? requesterAccount.getId() : approverAccount.getId();

        BankAccount locked1 = bankAccountRepository.findByIdForUpdate(firstId).orElseThrow();
        BankAccount locked2 = bankAccountRepository.findByIdForUpdate(secondId).orElseThrow();

        BankAccount lockedApprover = locked1.getId().equals(approverAccount.getId()) ? locked1 : locked2;
        BankAccount lockedRequester = locked1.getId().equals(requesterAccount.getId()) ? locked1 : locked2;

        if (lockedApprover.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance to approve emergency funds of ₹" + amount + ". Your balance: ₹" + lockedApprover.getBalance());
        }

        if (!lockedRequester.isActive()) {
            throw new BankingException("Requester's account is no longer active.");
        }

        // Transfer funds
        BigDecimal approverBefore = lockedApprover.getBalance();
        BigDecimal approverAfter = approverBefore.subtract(amount);
        lockedApprover.setBalance(approverAfter);
        bankAccountRepository.save(lockedApprover);

        BigDecimal requesterBefore = lockedRequester.getBalance();
        BigDecimal requesterAfter = requesterBefore.add(amount);
        lockedRequester.setBalance(requesterAfter);
        bankAccountRepository.save(lockedRequester);

        String debitRef = generator.generateReferenceNumber("EMG-OUT");
        String creditRef = generator.generateReferenceNumber("EMG-IN");

        // Approver Debit Transaction
        Transaction debitTx = new Transaction();
        debitTx.setReferenceNumber(debitRef);
        debitTx.setAccount(lockedApprover);
        debitTx.setTargetAccount(lockedRequester);
        debitTx.setType(TransactionType.EMERGENCY_DEBIT);
        debitTx.setAmount(amount);
        debitTx.setBalanceBefore(approverBefore);
        debitTx.setBalanceAfter(approverAfter);
        debitTx.setStatus(TransactionStatus.SUCCESS);
        debitTx.setDescription("Emergency Assistance to " + lockedRequester.getAccountNumber() + " (" + lockedRequester.getUser().getFullName() + ") - " + request.getReason());
        debitTx.setAtmLocation("Emergency Transfer Portal");
        Transaction savedDebitTx = transactionRepository.save(debitTx);

        // Requester Credit Transaction
        Transaction creditTx = new Transaction();
        creditTx.setReferenceNumber(creditRef);
        creditTx.setAccount(lockedRequester);
        creditTx.setTargetAccount(lockedApprover);
        creditTx.setType(TransactionType.EMERGENCY_CREDIT);
        creditTx.setAmount(amount);
        creditTx.setBalanceBefore(requesterBefore);
        creditTx.setBalanceAfter(requesterAfter);
        creditTx.setStatus(TransactionStatus.SUCCESS);
        creditTx.setDescription("Emergency Funds from " + lockedApprover.getAccountNumber() + " (" + lockedApprover.getUser().getFullName() + ") - " + request.getReason());
        creditTx.setAtmLocation("Emergency Transfer Portal");
        transactionRepository.save(creditTx);

        // Update Request State
        request.setStatus(EmergencyRequestStatus.APPROVED);
        request.setDecisionNote(note != null && !note.isBlank() ? note : "Approved emergency assistance");
        request.setTransactionReference(debitRef);
        request.setDecidedAt(LocalDateTime.now());
        emergencyRequestRepository.save(request);

        auditLogService.logAction(approverUsername, AuditAction.EMERGENCY_REQUEST_APPROVED, "EmergencyRequest", request.getRequestCode(),
                "Approved emergency transfer of ₹" + amount + " to " + lockedRequester.getAccountNumber(), "SUCCESS");

        notificationService.sendNotification(lockedRequester.getUser(), "Emergency Money Request Approved!",
                String.format("Good news! %s approved your emergency request for ₹%s. Funds have been credited to your account.",
                        lockedApprover.getUser().getFullName(), amount),
                "SUCCESS", "/dashboard");

        notificationService.sendNotification(lockedApprover.getUser(), "Emergency Transfer Sent",
                String.format("You approved emergency funds of ₹%s to %s. Ref: %s",
                        amount, lockedRequester.getUser().getFullName(), debitRef),
                "SUCCESS", "/transactions/receipt/" + savedDebitTx.getId());

        return savedDebitTx;
    }

    @Transactional
    public void rejectRequest(String rejecterUsername, Long requestId, String reason) {
        EmergencyRequest request = emergencyRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new BankingException("Emergency request not found"));

        BankAccount approverAccount = bankAccountRepository.findByUserUsername(rejecterUsername)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for user: " + rejecterUsername));

        if (!request.getTargetAccount().getId().equals(approverAccount.getId())) {
            throw new BankingException("You are not authorized to reject this request.");
        }

        if (request.getStatus() != EmergencyRequestStatus.PENDING) {
            throw new BankingException("Request has already been processed with status: " + request.getStatus());
        }

        request.setStatus(EmergencyRequestStatus.REJECTED);
        request.setDecisionNote(reason != null && !reason.isBlank() ? reason : "Declined by recipient");
        request.setDecidedAt(LocalDateTime.now());
        emergencyRequestRepository.save(request);

        auditLogService.logAction(rejecterUsername, AuditAction.EMERGENCY_REQUEST_REJECTED, "EmergencyRequest", request.getRequestCode(),
                "Rejected emergency request from " + request.getRequesterAccount().getAccountNumber() + " - Note: " + reason, "SUCCESS");

        notificationService.sendNotification(request.getRequesterAccount().getUser(), "Emergency Request Declined",
                String.format("Your emergency money request of ₹%s to %s was declined: %s",
                        request.getAmount(), approverAccount.getUser().getFullName(), request.getDecisionNote()),
                "DANGER", "/emergency/request");
    }

    public List<EmergencyRequest> getReceivedRequests(BankAccount account) {
        return emergencyRequestRepository.findByTargetAccountOrderByCreatedAtDesc(account);
    }

    public List<EmergencyRequest> getSentRequests(BankAccount account) {
        return emergencyRequestRepository.findByRequesterAccountOrderByCreatedAtDesc(account);
    }

    public long getPendingReceivedCount(BankAccount account) {
        return emergencyRequestRepository.countByTargetAccountAndStatus(account, EmergencyRequestStatus.PENDING);
    }

    public Page<EmergencyRequest> getAllRequests(Pageable pageable) {
        return emergencyRequestRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
