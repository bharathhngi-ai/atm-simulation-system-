package com.bank.atm.service;

import com.bank.atm.dto.EmergencyMoneyRequestDto;
import com.bank.atm.entity.BankAccount;
import com.bank.atm.entity.EmergencyRequest;
import com.bank.atm.entity.Transaction;
import com.bank.atm.entity.User;
import com.bank.atm.enums.AccountStatus;
import com.bank.atm.enums.EmergencyRequestStatus;
import com.bank.atm.enums.TransactionType;
import com.bank.atm.exception.BankingException;
import com.bank.atm.exception.InsufficientBalanceException;
import com.bank.atm.repository.BankAccountRepository;
import com.bank.atm.repository.EmergencyRequestRepository;
import com.bank.atm.repository.TransactionRepository;
import com.bank.atm.util.AccountNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmergencyMoneyServiceTest {

    @Mock
    private EmergencyRequestRepository emergencyRequestRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AccountNumberGenerator generator;

    @InjectMocks
    private EmergencyMoneyService emergencyMoneyService;

    private User requesterUser;
    private BankAccount requesterAccount;
    private User approverUser;
    private BankAccount approverAccount;

    @BeforeEach
    void setUp() {
        requesterUser = new User();
        requesterUser.setId(1L);
        requesterUser.setUsername("requester");
        requesterUser.setFullName("Requester User");

        requesterAccount = new BankAccount();
        requesterAccount.setId(10L);
        requesterAccount.setAccountNumber("100110001001");
        requesterAccount.setBalance(new BigDecimal("100.00"));
        requesterAccount.setStatus(AccountStatus.ACTIVE);
        requesterAccount.setUser(requesterUser);

        approverUser = new User();
        approverUser.setId(2L);
        approverUser.setUsername("approver");
        approverUser.setFullName("Approver User");

        approverAccount = new BankAccount();
        approverAccount.setId(20L);
        approverAccount.setAccountNumber("100110001002");
        approverAccount.setBalance(new BigDecimal("5000.00"));
        approverAccount.setStatus(AccountStatus.ACTIVE);
        approverAccount.setUser(approverUser);
    }

    @Test
    void testCreateRequest_Success() {
        EmergencyMoneyRequestDto dto = new EmergencyMoneyRequestDto();
        dto.setTargetAccountNumber("100110001002");
        dto.setAmount(new BigDecimal("500.00"));
        dto.setReason("Urgent car repair");

        when(bankAccountRepository.findByUserUsername("requester")).thenReturn(Optional.of(requesterAccount));
        when(bankAccountRepository.findByAccountNumber("100110001002")).thenReturn(Optional.of(approverAccount));
        when(generator.generateRequestCode()).thenReturn("EMG-12345");
        when(emergencyRequestRepository.save(any(EmergencyRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        EmergencyRequest result = emergencyMoneyService.createRequest("requester", dto);

        assertNotNull(result);
        assertEquals(EmergencyRequestStatus.PENDING, result.getStatus());
        assertEquals(new BigDecimal("500.00"), result.getAmount());
        verify(notificationService).sendNotification(eq(approverUser), any(), any(), any(), any());
    }

    @Test
    void testApproveRequest_Success() {
        EmergencyRequest req = new EmergencyRequest();
        req.setId(99L);
        req.setRequestCode("EMG-12345");
        req.setRequesterAccount(requesterAccount);
        req.setTargetAccount(approverAccount);
        req.setAmount(new BigDecimal("400.00"));
        req.setStatus(EmergencyRequestStatus.PENDING);
        req.setExpiresAt(LocalDateTime.now().plusHours(10));

        when(emergencyRequestRepository.findByIdForUpdate(99L)).thenReturn(Optional.of(req));
        when(bankAccountRepository.findByUserUsername("approver")).thenReturn(Optional.of(approverAccount));
        when(bankAccountRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(requesterAccount));
        when(bankAccountRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(approverAccount));
        when(generator.generateReferenceNumber("EMG-OUT")).thenReturn("EMG-OUT-1");
        when(generator.generateReferenceNumber("EMG-IN")).thenReturn("EMG-IN-1");
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction result = emergencyMoneyService.approveRequest("approver", 99L, "1234", "Approved");

        assertNotNull(result);
        assertEquals(EmergencyRequestStatus.APPROVED, req.getStatus());
        assertEquals(new BigDecimal("4600.00"), approverAccount.getBalance());
        assertEquals(new BigDecimal("500.00"), requesterAccount.getBalance());
        verify(accountService).verifyPin(approverAccount, "1234");
        verify(notificationService, times(2)).sendNotification(any(), any(), any(), any(), any());
    }

    @Test
    void testApproveRequest_InsufficientBalance_ThrowsException() {
        EmergencyRequest req = new EmergencyRequest();
        req.setId(99L);
        req.setRequestCode("EMG-12345");
        req.setRequesterAccount(requesterAccount);
        req.setTargetAccount(approverAccount);
        req.setAmount(new BigDecimal("10000.00")); // exceeds 5000.00
        req.setStatus(EmergencyRequestStatus.PENDING);
        req.setExpiresAt(LocalDateTime.now().plusHours(10));

        when(emergencyRequestRepository.findByIdForUpdate(99L)).thenReturn(Optional.of(req));
        when(bankAccountRepository.findByUserUsername("approver")).thenReturn(Optional.of(approverAccount));
        when(bankAccountRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(requesterAccount));
        when(bankAccountRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(approverAccount));

        assertThrows(InsufficientBalanceException.class, () ->
                emergencyMoneyService.approveRequest("approver", 99L, "1234", "Note"));
    }

    @Test
    void testDuplicateApproval_ThrowsException() {
        EmergencyRequest req = new EmergencyRequest();
        req.setId(99L);
        req.setTargetAccount(approverAccount);
        req.setStatus(EmergencyRequestStatus.APPROVED); // Already approved

        when(emergencyRequestRepository.findByIdForUpdate(99L)).thenReturn(Optional.of(req));
        when(bankAccountRepository.findByUserUsername("approver")).thenReturn(Optional.of(approverAccount));

        assertThrows(BankingException.class, () ->
                emergencyMoneyService.approveRequest("approver", 99L, "1234", "Note"));
    }
}
