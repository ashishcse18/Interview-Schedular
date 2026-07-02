package com.yourcompany.interviewscheduler.exception;

public class NonRetryableWhatsAppException extends RuntimeException {
    public NonRetryableWhatsAppException(String message) {
        super(message);
    }

    public NonRetryableWhatsAppException(String message, Throwable cause) {
        super(message, cause);
    }
}
