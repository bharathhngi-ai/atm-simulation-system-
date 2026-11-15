package com.bank.atm.exception;

public class FraudDetectedException extends BankingException {
    public FraudDetectedException(String message) {
        super(message);
    }
}
