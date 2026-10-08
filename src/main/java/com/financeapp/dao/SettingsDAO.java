package com.financeapp.dao;

import java.sql.Connection;
import java.util.Map;
import java.util.Optional;

/**
 * Data Access Object interface for system configuration and heuristic planner settings.
 */
public interface SettingsDAO {

    Optional<String> get(String key);
    Map<String, String> getAll();

    boolean set(String key, String value);
    boolean set(Connection conn, String key, String value);

    boolean delete(String key);
    boolean delete(Connection conn, String key);
}
