package com.bank.atm.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trusted_contacts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_owner_contact_account", columnNames = {"owner_account_id", "contact_account_id"})
})
public class TrustedContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_account_id", nullable = false)
    private BankAccount ownerAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_account_id", nullable = false)
    private BankAccount contactAccount;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(length = 50)
    private String relationship;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public TrustedContact() {}

    public TrustedContact(BankAccount ownerAccount, BankAccount contactAccount, String nickname, String relationship) {
        this.ownerAccount = ownerAccount;
        this.contactAccount = contactAccount;
        this.nickname = nickname;
        this.relationship = relationship;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BankAccount getOwnerAccount() {
        return ownerAccount;
    }

    public void setOwnerAccount(BankAccount ownerAccount) {
        this.ownerAccount = ownerAccount;
    }

    public BankAccount getContactAccount() {
        return contactAccount;
    }

    public void setContactAccount(BankAccount contactAccount) {
        this.contactAccount = contactAccount;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
