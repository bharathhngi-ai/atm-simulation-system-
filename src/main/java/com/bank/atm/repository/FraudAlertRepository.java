package com.bank.atm.repository;

import com.bank.atm.entity.BankAccount;
import com.bank.atm.entity.FraudAlert;
import com.bank.atm.enums.FraudAlertSeverity;
import com.bank.atm.enums.FraudAlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {

    Optional<FraudAlert> findByAlertCode(String alertCode);

    List<FraudAlert> findByAccountOrderByCreatedAtDesc(BankAccount account);

    List<FraudAlert> findTop5ByAccountOrderByCreatedAtDesc(BankAccount account);

    Page<FraudAlert> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT fa FROM FraudAlert fa WHERE (:status IS NULL OR fa.status = :status) " +
           "AND (:severity IS NULL OR fa.severity = :severity) ORDER BY fa.createdAt DESC")
    Page<FraudAlert> findFilteredAlerts(
            @Param("status") FraudAlertStatus status,
            @Param("severity") FraudAlertSeverity severity,
            Pageable pageable);

    long countByStatus(FraudAlertStatus status);

    long countBySeverity(FraudAlertSeverity severity);
}
