package com.financeapp.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.io.Closeable;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DBConnection and Cryptography Utility Tests")
class DBConnectionTest {

    @Test
    @DisplayName("Should successfully load JDBC Driver and configuration properties")
    void testConfigurationLoading() {
        assertNotNull(DBConnection.getDriver(), "Driver should not be null");
        assertEquals("com.mysql.cj.jdbc.Driver", DBConnection.getDriver());

        assertNotNull(DBConnection.getUrl(), "Database URL should not be null");
        assertTrue(DBConnection.getUrl().startsWith("jdbc:mysql://"), "URL should be MySQL JDBC URL");

        assertNotNull(DBConnection.getUsername(), "Username should not be null");
    }

    @Test
    @DisplayName("closeQuietly should handle nulls and exceptions gracefully")
    void testCloseQuietly() {
        assertDoesNotThrow(() -> DBConnection.closeQuietly((AutoCloseable[]) null));
        assertDoesNotThrow(() -> DBConnection.closeQuietly((AutoCloseable) null));

        AutoCloseable throwingCloseable = () -> {
            throw new IOException("Simulated close exception");
        };

        assertDoesNotThrow(() -> DBConnection.closeQuietly(throwingCloseable));
    }

    @Test
    @DisplayName("Should verify BCrypt password hashes match expected seed passwords")
    void testBCryptSeedPasswordVerification() {
        String adminHash = "$2a$10$htdjpO4It8oNPfOGPnzXe.ebyQ2znSERLUqhfzw.4SqvHberNARiu";
        String advisorHash = "$2a$10$g/y/wX0NbW39d5uSXJOkvesj.DDddQsK9AmEfRCM5FETSSJXQYZDy";
        String userHash = "$2a$10$jdJCA.ME491j/egT51eX5.Cfncuer6h5rdOXoQARAczZb3F7Q/mBa";

        assertTrue(BCrypt.checkpw("AdminPassword123!", adminHash), "Admin password hash must match");
        assertTrue(BCrypt.checkpw("AdvisorPassword123!", advisorHash), "Advisor password hash must match");
        assertTrue(BCrypt.checkpw("UserPassword123!", userHash), "User password hash must match");
        assertFalse(BCrypt.checkpw("WrongPassword!", userHash), "Invalid password should fail verification");
    }
}
