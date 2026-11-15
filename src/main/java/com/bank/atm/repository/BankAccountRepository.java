package com.bank.atm.repository;

import com.bank.atm.entity.BankAccount;
import com.bank.atm.entity.User;
import com.bank.atm.enums.AccountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    Optional<BankAccount> findByAccountNumber(String accountNumber);
    Optional<BankAccount> findByUser(User user);
    Optional<BankAccount> findByUserUsername(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BankAccount b WHERE b.accountNumber = :accountNumber")
    Optional<BankAccount> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BankAccount b WHERE b.id = :id")
    Optional<BankAccount> findByIdForUpdate(@Param("id") Long id);

    boolean existsByAccountNumber(String accountNumber);

    List<BankAccount> findByStatus(AccountStatus status);

    @Query("SELECT SUM(b.balance) FROM BankAccount b")
    BigDecimal getTotalBankBalance();

    long countByStatus(AccountStatus status);
}
