package com.bank.atm.service;

import com.bank.atm.dto.ProfileUpdateDto;
import com.bank.atm.dto.RegisterRequestDto;
import com.bank.atm.entity.BankAccount;
import com.bank.atm.entity.Role;
import com.bank.atm.entity.Transaction;
import com.bank.atm.entity.User;
import com.bank.atm.enums.*;
import com.bank.atm.exception.BankingException;
import com.bank.atm.repository.BankAccountRepository;
import com.bank.atm.repository.RoleRepository;
import com.bank.atm.repository.TransactionRepository;
import com.bank.atm.repository.UserRepository;
import com.bank.atm.util.AccountNumberGenerator;
import com.bank.atm.util.MoneyUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountNumberGenerator generator;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       BankAccountRepository bankAccountRepository,
                       TransactionRepository transactionRepository,
                       PasswordEncoder passwordEncoder,
                       AccountNumberGenerator generator,
                       AuditLogService auditLogService,
                       NotificationService notificationService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
        this.generator = generator;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Transactional
    public User registerCustomer(RegisterRequestDto dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BankingException("Password and confirm password do not match");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new BankingException("Username '" + dto.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BankingException("Email address is already registered");
        }
        if (userRepository.existsByPhoneNumber(dto.getPhoneNumber())) {
            throw new BankingException("Phone number is already registered");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setAddress(dto.getAddress());
        user.setEnabled(true);
        user.setAccountNonLocked(true);

        Role customerRole = roleRepository.findByName(RoleType.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(new Role(RoleType.ROLE_CUSTOMER)));
        user.getRoles().add(customerRole);

        User savedUser = userRepository.save(user);

        // Create Bank Account
        BankAccount account = new BankAccount();
        account.setAccountNumber(generateUniqueAccountNumber());
        account.setAccountType(dto.getAccountType());
        account.setAtmPin(passwordEncoder.encode(dto.getAtmPin()));
        account.setStatus(AccountStatus.ACTIVE);
        BigDecimal initialBal = MoneyUtils.formatAmount(dto.getInitialDeposit());
        account.setBalance(initialBal);
        account.setUser(savedUser);

        BankAccount savedAccount = bankAccountRepository.save(account);
        savedUser.setBankAccount(savedAccount);

        // Record Initial Deposit Transaction
        Transaction tx = new Transaction();
        tx.setReferenceNumber(generator.generateReferenceNumber("DEP"));
        tx.setAccount(savedAccount);
        tx.setType(TransactionType.DEPOSIT);
        tx.setAmount(initialBal);
        tx.setBalanceBefore(BigDecimal.ZERO.setScale(2));
        tx.setBalanceAfter(initialBal);
        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setDescription("Account Opening Deposit");
        tx.setAtmLocation("Main Online Branch");
        transactionRepository.save(tx);

        auditLogService.logAction(savedUser.getUsername(), AuditAction.USER_REGISTER, "User", savedUser.getId().toString(),
                "Registered new account: " + savedAccount.getAccountNumber(), "SUCCESS");

        notificationService.sendNotification(savedUser, "Welcome to Smart ATM Banking!",
                "Your account " + savedAccount.getAccountNumber() + " is active with an opening balance of ₹" + initialBal,
                "SUCCESS", "/dashboard");

        return savedUser;
    }

    private String generateUniqueAccountNumber() {
        String accNum;
        do {
            accNum = generator.generateAccountNumber();
        } while (bankAccountRepository.existsByAccountNumber(accNum));
        return accNum;
    }

    @Transactional
    public void updateProfile(String username, ProfileUpdateDto dto) {
        User user = getUserByUsername(username);

        if (!user.getEmail().equalsIgnoreCase(dto.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
            throw new BankingException("Email address is already in use by another user");
        }
        if (!user.getPhoneNumber().equals(dto.getPhoneNumber()) && userRepository.existsByPhoneNumber(dto.getPhoneNumber())) {
            throw new BankingException("Phone number is already in use by another user");
        }

        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setAddress(dto.getAddress());
        userRepository.save(user);

        auditLogService.logAction(username, AuditAction.PROFILE_UPDATED, "User", user.getId().toString(),
                "Updated personal profile details", "SUCCESS");
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BankingException("User not found: " + username));
    }

    @Transactional
    public void recordFailedLogin(String username, FraudDetectionService fraudService) {
        userRepository.findByUsername(username).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= 5) {
                user.setAccountNonLocked(false);
                user.setLockTime(LocalDateTime.now());
            }
            userRepository.save(user);

            auditLogService.logAction(username, AuditAction.USER_LOGIN_FAILED, "User", user.getId().toString(),
                    "Failed login attempt #" + attempts, "FAILURE");

            if (attempts >= 3) {
                fraudService.recordFailedLoginAlert(user, attempts);
            }
        });
    }

    @Transactional
    public void resetFailedLoginAttempts(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            if (user.getFailedLoginAttempts() > 0) {
                user.setFailedLoginAttempts(0);
                userRepository.save(user);
            }
        });
    }
}
