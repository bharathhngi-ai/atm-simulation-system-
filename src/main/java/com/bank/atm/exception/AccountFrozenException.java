package com.bank.atm.exception;

public class AccountFrozenException extends BankingException {
    public AccountFrozenException(String message) {
        super(message);
    }
}
