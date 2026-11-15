package com.bank.atm.exception;

public class InvalidPinException extends BankingException {
    public InvalidPinException(String message) {
        super(message);
    }
}
