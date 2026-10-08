package com.financeapp.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to manage database connections using JDBC.
 * Reads configuration from classpath 'db.properties' with fallback or override
 * from environment variables and system properties.
 */
public final class DBConnection {

    private static final Logger LOGGER = Logger.getLogger(DBConnection.class.getName());
    private static final String PROPERTIES_FILE = "db.properties";

    private static final String driver;
    private static final String url;
    private static final String username;
    private static final String password;

    static {
        Properties props = new Properties();

        // 1. Attempt loading from classpath db.properties
        try (InputStream input = DBConnection.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input != null) {
                props.load(input);
                LOGGER.info("Loaded database configuration from classpath: " + PROPERTIES_FILE);
            } else {
                LOGGER.warning("Resource '" + PROPERTIES_FILE + "' not found on classpath. Falling back to defaults/env.");
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error reading '" + PROPERTIES_FILE + "': " + e.getMessage(), e);
        }

        // 2. Resolve parameters with hierarchy: Environment Variable -> System Property -> db.properties -> Default
        driver = resolveConfig("DB_DRIVER", "db.driver", props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver"));
        url = resolveConfig("DB_URL", "db.url", props.getProperty("db.url", "jdbc:mysql://localhost:3306/finance_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8"));
        username = resolveConfig("DB_USER", "db.username", props.getProperty("db.username", "root"));
        password = resolveConfig("DB_PASSWORD", "db.password", props.getProperty("db.password", ""));

        // 3. Register JDBC driver class
        try {
            Class.forName(driver);
            LOGGER.info("Successfully registered JDBC driver: " + driver);
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Failed to load JDBC driver: " + driver, e);
            throw new ExceptionInInitializerError("JDBC Driver not found: " + driver);
        }
    }

    private static java.util.function.Supplier<Connection> customConnectionSupplier = null;

    private DBConnection() {
        // Utility class: prevent instantiation
    }

    private static String resolveConfig(String envKey, String sysPropKey, String defaultValue) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue.trim();
        }

        String sysValue = System.getProperty(sysPropKey);
        if (sysValue != null && !sysValue.trim().isEmpty()) {
            return sysValue.trim();
        }

        return defaultValue;
    }

    /**
     * Sets a custom connection supplier (primarily for automated integration tests).
     */
    public static void setCustomConnectionSupplier(java.util.function.Supplier<Connection> supplier) {
        customConnectionSupplier = supplier;
    }

    /**
     * Resets the custom connection supplier back to default DriverManager resolution.
     */
    public static void resetCustomConnectionSupplier() {
        customConnectionSupplier = null;
    }

    /**
     * Obtains a new database connection.
     *
     * @return an active {@link Connection}
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        if (customConnectionSupplier != null) {
            return customConnectionSupplier.get();
        }
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Closes AutoCloseable resources (Connection, Statement, ResultSet) safely without throwing exceptions.
     *
     * @param closeables array of resources to close
     */
    public static void closeQuietly(AutoCloseable... closeables) {
        if (closeables == null) {
            return;
        }
        for (AutoCloseable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (Exception e) {
                    LOGGER.log(Level.FINEST, "Error closing resource quietly: " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Rolls back a connection transaction safely without throwing exceptions.
     *
     * @param connection the connection to rollback
     */
    public static void rollbackQuietly(Connection connection) {
        if (connection != null) {
            try {
                if (!connection.isClosed() && !connection.getAutoCommit()) {
                    connection.rollback();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error rolling back transaction quietly: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Tests if a database connection can be established.
     *
     * @return true if connection succeeds and is valid, false otherwise
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && conn.isValid(2);
        } catch (SQLException e) {
            LOGGER.log(Level.FINE, "Database ping failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns the configured JDBC URL (for diagnostics/logging).
     */
    public static String getUrl() {
        return url;
    }

    /**
     * Returns the configured database username.
     */
    public static String getUsername() {
        return username;
    }

    /**
     * Returns the registered JDBC driver class name.
     */
    public static String getDriver() {
        return driver;
    }
}
