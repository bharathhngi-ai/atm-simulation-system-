package com.bank.atm.service;

import com.bank.atm.dto.DepositRequestDto;
import com.bank.atm.dto.TransferRequestDto;
import com.bank.atm.dto.WithdrawRequestDto;
import com.bank.atm.entity.BankAccount;
import com.bank.atm.entity.Transaction;
import com.bank.atm.entity.User;
import com.bank.atm.enums.AccountStatus;
import com.bank.atm.enums.TransactionType;
import com.bank.atm.exception.AccountFrozenException;
import com.bank.atm.exception.InsufficientBalanceException;
import com.bank.atm.repository.BankAccountRepository;
import com.bank.atm.repository.IdempotencyRecordRepository;
import com.bank.atm.repository.TransactionRepository;
import com.bank.atm.util.AccountNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private FraudDetectionService fraudDetectionService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AccountNumberGenerator generator;

    @InjectMocks
    private TransactionService transactionService;

    private User user;
    private BankAccount account;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setFullName("Test User");

        account = new BankAccount();
        account.setId(10L);
        account.setAccountNumber("100110001001");
        account.setBalance(new BigDecimal("1000.00"));
        account.setStatus(AccountStatus.ACTIVE);
        account.setUser(user);
    }

    @Test
    void testDeposit_Success() {
        DepositRequestDto dto = new DepositRequestDto();
        dto.setAmount(new BigDecimal("500.00"));
        dto.setDescription("Test Deposit");

        when(bankAccountRepository.findByUserUsername("testuser")).thenReturn(Optional.of(account));
        when(bankAccountRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(account));
        when(generator.generateReferenceNumber("DEP")).thenReturn("DEP-12345");
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction result = transactionService.deposit("testuser", dto);

        assertNotNull(result);
        assertEquals(new BigDecimal("1500.00"), account.getBalance());
        assertEquals(TransactionType.DEPOSIT, result.getType());
        verify(transactionRepository).save(any(Transaction.class));
        verify(auditLogService).logAction(eq("testuser"), any(), any(), any(), any(), eq("SUCCESS"));
    }

    @Test
    void testDeposit_AccountFrozen_ThrowsException() {
        account.setStatus(AccountStatus.FROZEN);
        when(bankAccountRepository.findByUserUsername("testuser")).thenReturn(Optional.of(account));

        DepositRequestDto dto = new DepositRequestDto();
        dto.setAmount(new BigDecimal("100.00"));

        assertThrows(AccountFrozenException.class, () -> transactionService.deposit("testuser", dto));
    }

    @Test
    void testWithdraw_Success() {
        WithdrawRequestDto dto = new WithdrawRequestDto();
        dto.setAmount(new BigDecimal("200.00"));
        dto.setAtmPin("1234");

        when(bankAccountRepository.findByUserUsername("testuser")).thenReturn(Optional.of(account));
        when(bankAccountRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(account));
        when(transactionRepository.sumAmountByAccountAndTypeSince(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(generator.generateReferenceNumber("WTH")).thenReturn("WTH-12345");
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction result = transactionService.withdraw("testuser", dto);

        assertNotNull(result);
        assertEquals(new BigDecimal("800.00"), account.getBalance());
        verify(accountService).verifyPin(account, "1234");
        verify(fraudDetectionService).checkWithdrawalFraudRules(account, new BigDecimal("200.00"));
    }

    @Test
    void testWithdraw_InsufficientBalance_ThrowsException() {
        WithdrawRequestDto dto = new WithdrawRequestDto();
        dto.setAmount(new BigDecimal("2000.00"));
        dto.setAtmPin("1234");

        when(bankAccountRepository.findByUserUsername("testuser")).thenReturn(Optional.of(account));
        when(bankAccountRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(account));
        when(transactionRepository.sumAmountByAccountAndTypeSince(any(), any(), any())).thenReturn(BigDecimal.ZERO);

        assertThrows(InsufficientBalanceException.class, () -> transactionService.withdraw("testuser", dto));
    }

    @Test
    void testTransfer_Success() {
        User recipientUser = new User();
        recipientUser.setId(2L);
        recipientUser.setUsername("recipient");
        recipientUser.setFullName("Recipient User");

        BankAccount recipientAccount = new BankAccount();
        recipientAccount.setId(20L);
        recipientAccount.setAccountNumber("100110001002");
        recipientAccount.setBalance(new BigDecimal("500.00"));
        recipientAccount.setStatus(AccountStatus.ACTIVE);
        recipientAccount.setUser(recipientUser);

        TransferRequestDto dto = new TransferRequestDto();
        dto.setRecipientAccountNumber("100110001002");
        dto.setAmount(new BigDecimal("300.00"));
        dto.setAtmPin("1234");
        dto.setRemarks("Rent Share");

        when(bankAccountRepository.findByUserUsername("testuser")).thenReturn(Optional.of(account));
        when(bankAccountRepository.findByAccountNumber("100110001002")).thenReturn(Optional.of(recipientAccount));
        when(transactionRepository.sumAmountByAccountAndTypeSince(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(bankAccountRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(account));
        when(bankAccountRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(recipientAccount));
        when(generator.generateReferenceNumber("TRF-OUT")).thenReturn("TRF-OUT-1");
        when(generator.generateReferenceNumber("TRF-IN")).thenReturn("TRF-IN-1");
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction result = transactionService.transfer("testuser", dto);

        assertNotNull(result);
        assertEquals(new BigDecimal("700.00"), account.getBalance());
        assertEquals(new BigDecimal("800.00"), recipientAccount.getBalance());
        verify(accountService).verifyPin(account, "1234");
        verify(notificationService, times(2)).sendNotification(any(), any(), any(), any(), any());
    }
}
