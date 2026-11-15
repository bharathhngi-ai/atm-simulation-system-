package com.bank.atm.repository;

import com.bank.atm.entity.BankAccount;
import com.bank.atm.entity.EmergencyRequest;
import com.bank.atm.enums.EmergencyRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmergencyRequestRepository extends JpaRepository<EmergencyRequest, Long> {

    Optional<EmergencyRequest> findByRequestCode(String requestCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT er FROM EmergencyRequest er WHERE er.id = :id")
    Optional<EmergencyRequest> findByIdForUpdate(@Param("id") Long id);

    List<EmergencyRequest> findByTargetAccountOrderByCreatedAtDesc(BankAccount targetAccount);

    List<EmergencyRequest> findByRequesterAccountOrderByCreatedAtDesc(BankAccount requesterAccount);

    List<EmergencyRequest> findByTargetAccountAndStatusOrderByCreatedAtDesc(BankAccount targetAccount, EmergencyRequestStatus status);

    long countByTargetAccountAndStatus(BankAccount targetAccount, EmergencyRequestStatus status);

    Page<EmergencyRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(EmergencyRequestStatus status);
}
