package com.bank.atm.exception;

public class AccountNotFoundException extends BankingException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}
