package com.bank.atm.service;

import com.bank.atm.dto.TrustedContactDto;
import com.bank.atm.entity.BankAccount;
import com.bank.atm.entity.TrustedContact;
import com.bank.atm.enums.AuditAction;
import com.bank.atm.exception.AccountNotFoundException;
import com.bank.atm.exception.BankingException;
import com.bank.atm.repository.BankAccountRepository;
import com.bank.atm.repository.TrustedContactRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TrustedContactService {

    private final TrustedContactRepository trustedContactRepository;
    private final BankAccountRepository bankAccountRepository;
    private final AuditLogService auditLogService;

    public TrustedContactService(TrustedContactRepository trustedContactRepository,
                                 BankAccountRepository bankAccountRepository,
                                 AuditLogService auditLogService) {
        this.trustedContactRepository = trustedContactRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.auditLogService = auditLogService;
    }

    public List<TrustedContact> getContacts(BankAccount ownerAccount) {
        return trustedContactRepository.findByOwnerAccountOrderByNicknameAsc(ownerAccount);
    }

    @Transactional
    public TrustedContact addContact(String username, TrustedContactDto dto) {
        BankAccount ownerAccount = bankAccountRepository.findByUserUsername(username)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for user: " + username));

        if (ownerAccount.getAccountNumber().equals(dto.getContactAccountNumber())) {
            throw new BankingException("You cannot add your own account as a trusted contact.");
        }

        BankAccount contactAccount = bankAccountRepository.findByAccountNumber(dto.getContactAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("Contact account " + dto.getContactAccountNumber() + " not found."));

        if (trustedContactRepository.existsByOwnerAccountAndContactAccount(ownerAccount, contactAccount)) {
            throw new BankingException("This account is already in your trusted contacts list.");
        }

        TrustedContact contact = new TrustedContact(ownerAccount, contactAccount, dto.getNickname(), dto.getRelationship());
        TrustedContact saved = trustedContactRepository.save(contact);

        auditLogService.logAction(username, AuditAction.CONTACT_ADDED, "TrustedContact", saved.getId().toString(),
                "Added contact: " + dto.getNickname() + " (" + dto.getContactAccountNumber() + ")", "SUCCESS");

        return saved;
    }

    @Transactional
    public void deleteContact(String username, Long contactId) {
        BankAccount ownerAccount = bankAccountRepository.findByUserUsername(username)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for user: " + username));

        TrustedContact contact = trustedContactRepository.findByIdAndOwnerAccount(contactId, ownerAccount)
                .orElseThrow(() -> new BankingException("Contact not found or does not belong to you."));

        trustedContactRepository.delete(contact);

        auditLogService.logAction(username, AuditAction.CONTACT_REMOVED, "TrustedContact", contactId.toString(),
                "Removed contact: " + contact.getNickname(), "SUCCESS");
    }
}
