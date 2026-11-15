package com.bank.atm.service;

import com.bank.atm.entity.BankAccount;
import com.bank.atm.entity.FraudAlert;
import com.bank.atm.entity.User;
import com.bank.atm.enums.AccountStatus;
import com.bank.atm.enums.FraudAlertSeverity;
import com.bank.atm.enums.FraudAlertStatus;
import com.bank.atm.enums.TransactionType;
import com.bank.atm.repository.BankAccountRepository;
import com.bank.atm.repository.FraudAlertRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private FraudAlertRepository fraudAlertRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private AccountNumberGenerator generator;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private FraudDetectionService fraudDetectionService;

    private User user;
    private BankAccount account;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("victim");

        account = new BankAccount();
        account.setId(10L);
        account.setAccountNumber("100110001001");
        account.setStatus(AccountStatus.ACTIVE);
        account.setUser(user);
        user.setBankAccount(account);
    }

    @Test
    void testRecordFailedLoginAlert_AtFiveAttempts_AutoFreezesAccount() {
        when(generator.generateAlertCode()).thenReturn("FRD-LOGIN-1");
        when(fraudAlertRepository.save(any(FraudAlert.class))).thenAnswer(inv -> inv.getArgument(0));

        FraudAlert alert = fraudDetectionService.recordFailedLoginAlert(user, 5);

        assertNotNull(alert);
        assertEquals(FraudAlertSeverity.CRITICAL, alert.getSeverity());
        assertEquals(AccountStatus.FROZEN, account.getStatus());
        verify(bankAccountRepository).save(account);
    }

    @Test
    void testCheckWithdrawalFraudRules_RapidWithdrawalsTriggered() {
        when(generator.generateAlertCode()).thenReturn("FRD-RAPID-1");
        when(transactionRepository.countByAccountAndTypeAndCreatedAtAfter(eq(account), eq(TransactionType.WITHDRAWAL), any()))
                .thenReturn(3L);

        fraudDetectionService.checkWithdrawalFraudRules(account, new BigDecimal("100.00"));

        verify(fraudAlertRepository).save(any(FraudAlert.class));
        verify(notificationService).sendNotification(eq(user), any(), any(), any(), any());
    }

    @Test
    void testCheckWithdrawalFraudRules_LargeWithdrawalTriggered() {
        when(generator.generateAlertCode()).thenReturn("FRD-LARGE-1");
        when(transactionRepository.countByAccountAndTypeAndCreatedAtAfter(eq(account), eq(TransactionType.WITHDRAWAL), any()))
                .thenReturn(0L);

        fraudDetectionService.checkWithdrawalFraudRules(account, new BigDecimal("15000.00"));

        verify(fraudAlertRepository).save(any(FraudAlert.class));
    }

    @Test
    void testResolveAlert_Success() {
        FraudAlert alert = new FraudAlert("FRD-01", account, user, "TEST_RULE", "Test", FraudAlertSeverity.HIGH);
        when(fraudAlertRepository.findByAlertCode("FRD-01")).thenReturn(Optional.of(alert));

        fraudDetectionService.resolveAlert("FRD-01", FraudAlertStatus.RESOLVED, "Customer verified", "admin", false);

        assertEquals(FraudAlertStatus.RESOLVED, alert.getStatus());
        assertEquals("Customer verified", alert.getResolutionNotes());
        assertEquals("admin", alert.getResolvedBy());
        verify(fraudAlertRepository).save(alert);
    }
}
