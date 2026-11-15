package com.bank.atm.exception;

public class DuplicateTransactionException extends BankingException {
    public DuplicateTransactionException(String message) {
        super(message);
    }
}
