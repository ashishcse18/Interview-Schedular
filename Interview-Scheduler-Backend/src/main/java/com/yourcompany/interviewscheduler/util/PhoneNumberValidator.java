package com.yourcompany.interviewscheduler.util;

public class PhoneNumberValidator {

    /**
     * Validates and normalizes phone numbers to E.164 format.
     * Expects: +<country_code><digits> (e.g. +919876543210)
     */
    public static String validateAndNormalize(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty");
        }

        // Remove all whitespace, dashes, brackets, and parentheses
        String cleaned = phoneNumber.replaceAll("[\\s\\-\\(\\)]", "");

        // Standardize leading 00 to +
        if (cleaned.startsWith("00")) {
            cleaned = "+" + cleaned.substring(2);
        }

        // If it's a clean digit string of length 11 or 12 without +, assume it might be missing +
        if (!cleaned.startsWith("+")) {
            if (cleaned.matches("\\d{10,15}")) {
                // If it is 10 digits (common for India local format, e.g. 9876543210), 
                // we should caution or add a default country code. But standard E.164 requires country code.
                // Let's assume if it is 10 digits, we default to prepend "+91" (India country code as standard default),
                // otherwise prepend "+" to make it international.
                if (cleaned.length() == 10) {
                    cleaned = "+91" + cleaned;
                } else {
                    cleaned = "+" + cleaned;
                }
            } else {
                throw new IllegalArgumentException("Phone number must contain only digits and start with a country code");
            }
        }

        // Verify E.164 compliance (+ followed by 10 to 15 digits)
        if (!cleaned.matches("^\\+\\d{10,15}$")) {
            throw new IllegalArgumentException("Invalid phone number format. Must be +<country_code><number> (10-15 digits)");
        }

        return cleaned;
    }
}
