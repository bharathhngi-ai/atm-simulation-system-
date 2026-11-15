package com.bank.atm.util;

import org.springframework.stereotype.Component;
import java.security.SecureRandom;

@Component
public class AccountNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    public String generateAccountNumber() {
        // Generates 12-digit account number starting with 1001
        long suffix = 10000000L + RANDOM.nextInt(90000000);
        return "1001" + suffix;
    }

    public String generateReferenceNumber(String prefix) {
        long randomPart = 100000L + RANDOM.nextInt(900000);
        return prefix + "-" + System.currentTimeMillis() % 100000000 + "-" + randomPart;
    }

    public String generateRequestCode() {
        long code = 10000000L + RANDOM.nextInt(90000000);
        return "EMG-" + code;
    }

    public String generateAlertCode() {
        long code = 100000L + RANDOM.nextInt(900000);
        return "FRD-" + code;
    }
}
