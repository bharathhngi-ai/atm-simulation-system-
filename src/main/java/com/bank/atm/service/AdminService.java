package com.bank.atm.service;

import com.bank.atm.entity.BankAccount;
import com.bank.atm.entity.User;
import com.bank.atm.enums.AccountStatus;
import com.bank.atm.enums.FraudAlertStatus;
import com.bank.atm.enums.TransactionStatus;
import com.bank.atm.enums.TransactionType;
import com.bank.atm.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final EmergencyRequestRepository emergencyRequestRepository;

    public AdminService(UserRepository userRepository,
                        BankAccountRepository bankAccountRepository,
                        TransactionRepository transactionRepository,
                        FraudAlertRepository fraudAlertRepository,
                        EmergencyRequestRepository emergencyRequestRepository) {
        this.userRepository = userRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.emergencyRequestRepository = emergencyRequestRepository;
    }

    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Page<BankAccount> getAllAccounts(Pageable pageable) {
        return bankAccountRepository.findAll(pageable);
    }

    public Map<String, Object> getSystemStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalUsers = userRepository.count();
        long totalAccounts = bankAccountRepository.count();
        long activeAccounts = bankAccountRepository.countByStatus(AccountStatus.ACTIVE);
        long frozenAccounts = bankAccountRepository.countByStatus(AccountStatus.FROZEN);

        BigDecimal totalBankBalance = bankAccountRepository.getTotalBankBalance();
        if (totalBankBalance == null) totalBankBalance = BigDecimal.ZERO;

        BigDecimal totalDeposits = transactionRepository.sumTotalAmountByType(TransactionType.DEPOSIT);
        BigDecimal totalWithdrawals = transactionRepository.sumTotalAmountByType(TransactionType.WITHDRAWAL);
        BigDecimal totalTransfers = transactionRepository.sumTotalAmountByType(TransactionType.TRANSFER_DEBIT);

        long totalTransactions = transactionRepository.count();
        long successfulTx = transactionRepository.countByStatus(TransactionStatus.SUCCESS);
        long failedTx = transactionRepository.countByStatus(TransactionStatus.FAILED);

        long openFraudAlerts = fraudAlertRepository.countByStatus(FraudAlertStatus.OPEN);
        long totalFraudAlerts = fraudAlertRepository.count();

        stats.put("totalUsers", totalUsers);
        stats.put("totalAccounts", totalAccounts);
        stats.put("activeAccounts", activeAccounts);
        stats.put("frozenAccounts", frozenAccounts);
        stats.put("totalBankBalance", totalBankBalance);
        stats.put("totalDeposits", totalDeposits != null ? totalDeposits : BigDecimal.ZERO);
        stats.put("totalWithdrawals", totalWithdrawals != null ? totalWithdrawals : BigDecimal.ZERO);
        stats.put("totalTransfers", totalTransfers != null ? totalTransfers : BigDecimal.ZERO);
        stats.put("totalTransactions", totalTransactions);
        stats.put("successfulTx", successfulTx);
        stats.put("failedTx", failedTx);
        stats.put("openFraudAlerts", openFraudAlerts);
        stats.put("totalFraudAlerts", totalFraudAlerts);

        return stats;
    }
}
