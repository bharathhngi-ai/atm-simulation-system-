package com.bank.atm.service;

import com.bank.atm.dto.PinChangeRequestDto;
import com.bank.atm.entity.BankAccount;
import com.bank.atm.entity.User;
import com.bank.atm.enums.AccountStatus;
import com.bank.atm.exception.InvalidPinException;
import com.bank.atm.repository.BankAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AccountService accountService;

    private User user;
    private BankAccount account;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("john");

        account = new BankAccount();
        account.setId(10L);
        account.setAccountNumber("100110001001");
        account.setAtmPin("$2a$10$encodedOldPin");
        account.setBalance(new BigDecimal("2500.00"));
        account.setStatus(AccountStatus.ACTIVE);
        account.setUser(user);
    }

    @Test
    void testChangePin_Success() {
        PinChangeRequestDto dto = new PinChangeRequestDto();
        dto.setCurrentPin("1234");
        dto.setNewPin("5678");
        dto.setConfirmPin("5678");

        when(bankAccountRepository.findByUserUsername("john")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("1234", "$2a$10$encodedOldPin")).thenReturn(true);
        when(passwordEncoder.matches("5678", "$2a$10$encodedOldPin")).thenReturn(false);
        when(passwordEncoder.encode("5678")).thenReturn("$2a$10$encodedNewPin");

        accountService.changePin("john", dto);

        assertEquals("$2a$10$encodedNewPin", account.getAtmPin());
        verify(bankAccountRepository).save(account);
        verify(notificationService).sendNotification(eq(user), any(), any(), any(), any());
    }

    @Test
    void testChangePin_InvalidCurrentPin_ThrowsException() {
        PinChangeRequestDto dto = new PinChangeRequestDto();
        dto.setCurrentPin("9999");
        dto.setNewPin("5678");
        dto.setConfirmPin("5678");

        when(bankAccountRepository.findByUserUsername("john")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("9999", "$2a$10$encodedOldPin")).thenReturn(false);

        assertThrows(InvalidPinException.class, () -> accountService.changePin("john", dto));
    }

    @Test
    void testFreezeAccount_Success() {
        when(bankAccountRepository.findByUserUsername("john")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("1234", "$2a$10$encodedOldPin")).thenReturn(true);

        accountService.freezeAccount("john", "Lost ATM card", "1234");

        assertEquals(AccountStatus.FROZEN, account.getStatus());
        assertEquals("Lost ATM card", account.getFrozenReason());
        assertNotNull(account.getFrozenAt());
        verify(bankAccountRepository).save(account);
    }

    @Test
    void testUnfreezeAccount_Success() {
        account.setStatus(AccountStatus.FROZEN);
        account.setFrozenReason("Security check");

        when(bankAccountRepository.findByUserUsername("john")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("1234", "$2a$10$encodedOldPin")).thenReturn(true);

        accountService.unfreezeAccount("john", "1234");

        assertEquals(AccountStatus.ACTIVE, account.getStatus());
        assertNull(account.getFrozenReason());
        verify(bankAccountRepository).save(account);
    }
}
