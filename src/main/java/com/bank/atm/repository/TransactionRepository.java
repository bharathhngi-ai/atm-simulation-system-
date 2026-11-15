package com.bank.atm.repository;

import com.bank.atm.entity.BankAccount;
import com.bank.atm.entity.Transaction;
import com.bank.atm.enums.TransactionStatus;
import com.bank.atm.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    List<Transaction> findTop10ByAccountOrTargetAccountOrderByCreatedAtDesc(BankAccount account, BankAccount targetAccount);

    @Query("SELECT t FROM Transaction t WHERE t.account = :account OR t.targetAccount = :account ORDER BY t.createdAt DESC")
    Page<Transaction> findAllByAccountInvolved(@Param("account") BankAccount account, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE (t.account = :account OR t.targetAccount = :account) " +
           "AND (:type IS NULL OR t.type = :type) " +
           "AND (:startDate IS NULL OR t.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR t.createdAt <= :endDate) " +
           "ORDER BY t.createdAt DESC")
    Page<Transaction> findFilteredTransactions(
            @Param("account") BankAccount account,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT t FROM Transaction t " +
           "WHERE (:type IS NULL OR t.type = :type) " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:accountNumber IS NULL OR t.account.accountNumber = :accountNumber OR (t.targetAccount IS NOT NULL AND t.targetAccount.accountNumber = :accountNumber)) " +
           "ORDER BY t.createdAt DESC")
    Page<Transaction> findAdminFilteredTransactions(
            @Param("type") TransactionType type,
            @Param("status") TransactionStatus status,
            @Param("accountNumber") String accountNumber,
            Pageable pageable);

    long countByAccountAndTypeAndCreatedAtAfter(BankAccount account, TransactionType type, LocalDateTime after);

    long countByAccountAndStatusAndCreatedAtAfter(BankAccount account, TransactionStatus status, LocalDateTime after);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.account = :account AND t.type = :type AND t.createdAt >= :after AND t.status = 'SUCCESS'")
    BigDecimal sumAmountByAccountAndTypeSince(@Param("account") BankAccount account, @Param("type") TransactionType type, @Param("after") LocalDateTime after);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.type = :type AND t.status = 'SUCCESS'")
    BigDecimal sumTotalAmountByType(@Param("type") TransactionType type);

    long countByStatus(TransactionStatus status);
}
