package com.bank.atm.config;

import com.bank.atm.entity.*;
import com.bank.atm.enums.*;
import com.bank.atm.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final TrustedContactRepository trustedContactRepository;
    private final EmergencyRequestRepository emergencyRequestRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           RoleRepository roleRepository,
                           BankAccountRepository bankAccountRepository,
                           TransactionRepository transactionRepository,
                           TrustedContactRepository trustedContactRepository,
                           EmergencyRequestRepository emergencyRequestRepository,
                           FraudAlertRepository fraudAlertRepository,
                           NotificationRepository notificationRepository,
                           AuditLogRepository auditLogRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.trustedContactRepository = trustedContactRepository;
        this.emergencyRequestRepository = emergencyRequestRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.notificationRepository = notificationRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Checking initial banking data seed...");

        Role customerRole = roleRepository.findByName(RoleType.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(new Role(RoleType.ROLE_CUSTOMER)));
        Role adminRole = roleRepository.findByName(RoleType.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(RoleType.ROLE_ADMIN)));

        // 1. Seed Admin
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFullName("Banking Administrator");
            admin.setEmail("admin@smartbank.com");
            admin.setPhoneNumber("+1 800-555-0199");
            admin.setAddress("Headquarters - Security Ops, 100 Financial Blvd");
            admin.getRoles().add(adminRole);
            userRepository.save(admin);
            log.info("Created Admin user: admin / admin123");
        }

        // 2. Seed Customer John Doe
        BankAccount johnAccount = null;
        if (!userRepository.existsByUsername("john_doe")) {
            User john = new User();
            john.setUsername("john_doe");
            john.setPassword(passwordEncoder.encode("password123"));
            john.setFullName("Johnathan Doe");
            john.setEmail("john.doe@example.com");
            john.setPhoneNumber("+1 555-010-1001");
            john.setAddress("742 Evergreen Terrace, Springfield");
            john.getRoles().add(customerRole);
            User savedJohn = userRepository.save(john);

            johnAccount = new BankAccount();
            johnAccount.setAccountNumber("100110001001");
            johnAccount.setAccountType(AccountType.SAVINGS);
            johnAccount.setBalance(new BigDecimal("15450.00"));
            johnAccount.setAtmPin(passwordEncoder.encode("1234"));
            johnAccount.setStatus(AccountStatus.ACTIVE);
            johnAccount.setUser(savedJohn);
            johnAccount = bankAccountRepository.save(johnAccount);
            savedJohn.setBankAccount(johnAccount);

            // Initial transactions for John
            Transaction t1 = new Transaction();
            t1.setReferenceNumber("DEP-INIT-1001");
            t1.setAccount(johnAccount);
            t1.setType(TransactionType.DEPOSIT);
            t1.setAmount(new BigDecimal("15000.00"));
            t1.setBalanceBefore(BigDecimal.ZERO);
            t1.setBalanceAfter(new BigDecimal("15000.00"));
            t1.setStatus(TransactionStatus.SUCCESS);
            t1.setDescription("Opening Cash Deposit");
            t1.setAtmLocation("Downtown Branch ATM #01");
            transactionRepository.save(t1);

            Transaction t2 = new Transaction();
            t2.setReferenceNumber("DEP-PAY-1002");
            t2.setAccount(johnAccount);
            t2.setType(TransactionType.DEPOSIT);
            t2.setAmount(new BigDecimal("450.00"));
            t2.setBalanceBefore(new BigDecimal("15000.00"));
            t2.setBalanceAfter(new BigDecimal("15450.00"));
            t2.setStatus(TransactionStatus.SUCCESS);
            t2.setDescription("Salary Dividend Credit");
            t2.setAtmLocation("Online Automated Deposit");
            transactionRepository.save(t2);

            notificationRepository.save(new Notification(savedJohn, "Welcome to Smart ATM Banking",
                    "Your account 100110001001 is active with starting balance ₹15,450.00", "SUCCESS", "/dashboard"));
        } else {
            johnAccount = bankAccountRepository.findByAccountNumber("100110001001").orElse(null);
        }

        // 3. Seed Customer Jane Smith
        BankAccount janeAccount = null;
        if (!userRepository.existsByUsername("jane_smith")) {
            User jane = new User();
            jane.setUsername("jane_smith");
            jane.setPassword(passwordEncoder.encode("password123"));
            jane.setFullName("Jane Smith");
            jane.setEmail("jane.smith@example.com");
            jane.setPhoneNumber("+1 555-010-1002");
            jane.setAddress("123 Maple Street, Metropolis");
            jane.getRoles().add(customerRole);
            User savedJane = userRepository.save(jane);

            janeAccount = new BankAccount();
            janeAccount.setAccountNumber("100110001002");
            janeAccount.setAccountType(AccountType.CHECKING);
            janeAccount.setBalance(new BigDecimal("8200.00"));
            janeAccount.setAtmPin(passwordEncoder.encode("1234"));
            janeAccount.setStatus(AccountStatus.ACTIVE);
            janeAccount.setUser(savedJane);
            janeAccount = bankAccountRepository.save(janeAccount);
            savedJane.setBankAccount(janeAccount);

            Transaction tj1 = new Transaction();
            tj1.setReferenceNumber("DEP-INIT-1003");
            tj1.setAccount(janeAccount);
            tj1.setType(TransactionType.DEPOSIT);
            tj1.setAmount(new BigDecimal("8200.00"));
            tj1.setBalanceBefore(BigDecimal.ZERO);
            tj1.setBalanceAfter(new BigDecimal("8200.00"));
            tj1.setStatus(TransactionStatus.SUCCESS);
            tj1.setDescription("Opening Deposit");
            tj1.setAtmLocation("Metropolis Mall ATM #04");
            transactionRepository.save(tj1);

            notificationRepository.save(new Notification(savedJane, "Welcome to Smart ATM Banking",
                    "Your checking account 100110001002 is active with balance ₹8,200.00", "SUCCESS", "/dashboard"));
        } else {
            janeAccount = bankAccountRepository.findByAccountNumber("100110001002").orElse(null);
        }

        // 4. Seed Trusted Contact between John and Jane
        if (johnAccount != null && janeAccount != null) {
            if (!trustedContactRepository.existsByOwnerAccountAndContactAccount(johnAccount, janeAccount)) {
                trustedContactRepository.save(new TrustedContact(johnAccount, janeAccount, "Jane Smith (Sister)", "Family"));
            }
            if (!trustedContactRepository.existsByOwnerAccountAndContactAccount(janeAccount, johnAccount)) {
                trustedContactRepository.save(new TrustedContact(janeAccount, johnAccount, "John Doe (Brother)", "Family"));
            }

            // Seed a sample pending Emergency Request from Jane to John
            if (emergencyRequestRepository.countByTargetAccountAndStatus(johnAccount, EmergencyRequestStatus.PENDING) == 0) {
                EmergencyRequest er = new EmergencyRequest();
                er.setRequestCode("EMG-77441100");
                er.setRequesterAccount(janeAccount);
                er.setTargetAccount(johnAccount);
                er.setAmount(new BigDecimal("1200.00"));
                er.setReason("Urgent medical prescription & car brake replacement");
                er.setStatus(EmergencyRequestStatus.PENDING);
                er.setExpiresAt(LocalDateTime.now().plusHours(20));
                emergencyRequestRepository.save(er);

                notificationRepository.save(new Notification(johnAccount.getUser(), "Emergency Money Request Received",
                        "Jane Smith (100110001002) requested ₹1,200.00 for urgent medical/repair expenses.", "WARNING", "/emergency/received"));
            }
        }

        // 5. Seed a sample rule-based Fraud Alert for Admin overview
        if (fraudAlertRepository.count() == 0 && johnAccount != null) {
            FraudAlert alert = new FraudAlert(
                    "FRD-998822",
                    johnAccount,
                    johnAccount.getUser(),
                    "LARGE_TRANSACTION_MONITORING",
                    "Account 100110001001 performed initial high-volume opening deposit verification (₹15,000.00).",
                    FraudAlertSeverity.LOW
            );
            fraudAlertRepository.save(alert);
        }

        // 6. Seed initial audit log
        if (auditLogRepository.count() == 0) {
            AuditLog auditLog = new AuditLog("SYSTEM", AuditAction.USER_REGISTER, "SystemBootstrap", "1",
                    "Database initialized and pre-seeded successfully with demo accounts.", "127.0.0.1", "SUCCESS");
            auditLogRepository.save(auditLog);
        }

        log.info("ATM Smart Banking seed completed successfully.");
    }
}
