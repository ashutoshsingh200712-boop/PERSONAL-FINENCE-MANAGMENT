package com.financeapp.dao.jdbc;

import com.financeapp.dao.SettingsDAO;
import com.financeapp.exception.DataAccessException;
import com.financeapp.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC implementation of SettingsDAO.
 */
public class JdbcSettingsDAO implements SettingsDAO {

    private static final String SQL_GET =
            "SELECT setting_value FROM system_settings WHERE setting_key = ?";

    private static final String SQL_GET_ALL =
            "SELECT setting_key, setting_value FROM system_settings";

    private static final String SQL_FIND_EXISTING =
            "SELECT id FROM system_settings WHERE setting_key = ?";

    private static final String SQL_UPDATE =
            "UPDATE system_settings SET setting_value = ?, updated_at = ? WHERE setting_key = ?";

    private static final String SQL_INSERT =
            "INSERT INTO system_settings (setting_key, setting_value, updated_at) VALUES (?, ?, ?)";

    private static final String SQL_DELETE =
            "DELETE FROM system_settings WHERE setting_key = ?";

    @Override
    public Optional<String> get(String key) {
        if (key == null) return Optional.empty();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET)) {
            stmt.setString(1, key);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString("setting_value"));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to get setting for key: " + key, e);
        }
        return Optional.empty();
    }

    @Override
    public Map<String, String> getAll() {
        Map<String, String> map = new HashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_ALL);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("setting_key"), rs.getString("setting_value"));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load all settings", e);
        }
        return map;
    }

    @Override
    public boolean set(String key, String value) {
        try (Connection conn = DBConnection.getConnection()) {
            return set(conn, key, value);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save setting: " + key, e);
        }
    }

    @Override
    public boolean set(Connection conn, String key, String value) {
        if (key == null) return false;
        try {
            boolean exists = false;
            try (PreparedStatement check = conn.prepareStatement(SQL_FIND_EXISTING)) {
                check.setString(1, key);
                try (ResultSet rs = check.executeQuery()) {
                    exists = rs.next();
                }
            }

            LocalDateTime now = LocalDateTime.now();
            if (exists) {
                try (PreparedStatement update = conn.prepareStatement(SQL_UPDATE)) {
                    update.setString(1, value);
                    update.setTimestamp(2, Timestamp.valueOf(now));
                    update.setString(3, key);
                    return update.executeUpdate() > 0;
                }
            } else {
                try (PreparedStatement insert = conn.prepareStatement(SQL_INSERT)) {
                    insert.setString(1, key);
                    insert.setString(2, value);
                    insert.setTimestamp(3, Timestamp.valueOf(now));
                    return insert.executeUpdate() > 0;
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to persist setting with connection: " + key, e);
        }
    }

    @Override
    public boolean delete(String key) {
        try (Connection conn = DBConnection.getConnection()) {
            return delete(conn, key);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete setting: " + key, e);
        }
    }

    @Override
    public boolean delete(Connection conn, String key) {
        if (key == null) return false;
        try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setString(1, key);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete setting with connection: " + key, e);
        }
    }
}
