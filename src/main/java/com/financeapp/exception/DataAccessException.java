package com.financeapp.exception;

import java.sql.SQLException;

/**
 * Unchecked exception wrapping SQL and data layer failures to avoid leaking JDBC specifics into business services.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, SQLException cause) {
        super(message, cause);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
