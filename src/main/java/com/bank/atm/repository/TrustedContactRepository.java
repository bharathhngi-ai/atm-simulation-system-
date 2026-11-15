package com.bank.atm.repository;

import com.bank.atm.entity.BankAccount;
import com.bank.atm.entity.TrustedContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrustedContactRepository extends JpaRepository<TrustedContact, Long> {
    List<TrustedContact> findByOwnerAccountOrderByNicknameAsc(BankAccount ownerAccount);
    Optional<TrustedContact> findByOwnerAccountAndContactAccount(BankAccount ownerAccount, BankAccount contactAccount);
    boolean existsByOwnerAccountAndContactAccount(BankAccount ownerAccount, BankAccount contactAccount);
    Optional<TrustedContact> findByIdAndOwnerAccount(Long id, BankAccount ownerAccount);
}
