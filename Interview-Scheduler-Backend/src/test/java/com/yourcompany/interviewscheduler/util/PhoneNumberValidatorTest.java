package com.yourcompany.interviewscheduler.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhoneNumberValidatorTest {

    @Test
    void testValidPhoneNumbers() {
        assertEquals("+919876543210", PhoneNumberValidator.validateAndNormalize("+919876543210"));
        assertEquals("+919876543210", PhoneNumberValidator.validateAndNormalize("919876543210"));
        assertEquals("+919876543210", PhoneNumberValidator.validateAndNormalize("9876543210")); // Prepend default +91 for 10-digit
        assertEquals("+14155238886", PhoneNumberValidator.validateAndNormalize("+1 (415) 523-8886"));
        assertEquals("+14155238886", PhoneNumberValidator.validateAndNormalize("0014155238886"));
    }

    @Test
    void testInvalidPhoneNumbers() {
        assertThrows(IllegalArgumentException.class, () -> PhoneNumberValidator.validateAndNormalize("1234"));
        assertThrows(IllegalArgumentException.class, () -> PhoneNumberValidator.validateAndNormalize("abc-xyz"));
        assertThrows(IllegalArgumentException.class, () -> PhoneNumberValidator.validateAndNormalize(null));
        assertThrows(IllegalArgumentException.class, () -> PhoneNumberValidator.validateAndNormalize(""));
    }
}
