package com.financeapp.exception;

/**
 * Thrown when a monetary amount is invalid (e.g., negative or null when positive is required).
 */
public class InvalidAmountException extends RuntimeException {

    public InvalidAmountException(String message) {
        super(message);
    }

    public InvalidAmountException(String message, Throwable cause) {
        super(message, cause);
    }
}
