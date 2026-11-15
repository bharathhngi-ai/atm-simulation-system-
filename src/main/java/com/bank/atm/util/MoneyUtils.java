package com.bank.atm.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public final class MoneyUtils {

    private MoneyUtils() {}

    public static BigDecimal formatAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static String toCurrencyString(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        return "₹" + String.format(Locale.of("en", "IN"), "%,.2f", amount);
    }

    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        int len = accountNumber.length();
        return "********" + accountNumber.substring(len - 4);
    }
}
