package com.financeapp.dao.jdbc;

import com.financeapp.dao.UserDAO;
import com.financeapp.exception.DataAccessException;
import com.financeapp.exception.DuplicateEmailException;
import com.financeapp.model.*;
import com.financeapp.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of UserDAO.
 */
public class JdbcUserDAO implements UserDAO {

    private static final String SQL_INSERT =
            "INSERT INTO users (name, email, password_hash, role, active, failed_logins, locked_until, advisor_id, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT id, name, email, password_hash, role, active, failed_logins, locked_until, advisor_id, created_at " +
            "FROM users WHERE id = ?";

    private static final String SQL_FIND_BY_EMAIL =
            "SELECT id, name, email, password_hash, role, active, failed_logins, locked_until, advisor_id, created_at " +
            "FROM users WHERE email = ?";

    private static final String SQL_FIND_ALL =
            "SELECT id, name, email, password_hash, role, active, failed_logins, locked_until, advisor_id, created_at " +
            "FROM users ORDER BY id ASC";

    private static final String SQL_FIND_BY_ADVISOR =
            "SELECT id, name, email, password_hash, role, active, failed_logins, locked_until, advisor_id, created_at " +
            "FROM users WHERE advisor_id = ? ORDER BY id ASC";

    private static final String SQL_UPDATE =
            "UPDATE users SET name = ?, email = ?, password_hash = ?, role = ?, active = ?, advisor_id = ? " +
            "WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM users WHERE id = ?";

    private static final String SQL_UPDATE_FAILED_LOGINS =
            "UPDATE users SET failed_logins = ?, locked_until = ? WHERE id = ?";

    private static final String SQL_RESET_FAILED_LOGINS =
            "UPDATE users SET failed_logins = 0, locked_until = NULL WHERE id = ?";

    @Override
    public long create(User user) {
        try (Connection conn = DBConnection.getConnection()) {
            return create(conn, user);
        } catch (SQLException e) {
            handleSqlException("Failed to create user", e);
            return -1;
        }
    }

    @Override
    public long create(Connection conn, User user) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPasswordHash());
            stmt.setString(4, user.getRole().name());
            stmt.setBoolean(5, user.isActive());
            stmt.setInt(6, user.getFailedLogins());
            if (user.getLockedUntil() != null) {
                stmt.setTimestamp(7, Timestamp.valueOf(user.getLockedUntil()));
            } else {
                stmt.setNull(7, Types.TIMESTAMP);
            }
            if (user.getAdvisorId() != null) {
                stmt.setLong(8, user.getAdvisorId());
            } else {
                stmt.setNull(8, Types.BIGINT);
            }
            LocalDateTime createdAt = user.getCreatedAt() != null ? user.getCreatedAt() : LocalDateTime.now();
            stmt.setTimestamp(9, Timestamp.valueOf(createdAt));

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new DataAccessException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    user.setId(id);
                    user.setCreatedAt(createdAt);
                    return id;
                } else {
                    throw new DataAccessException("Creating user failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            handleSqlException("Failed to insert user", e);
            return -1;
        }
    }

    @Override
    public Optional<User> findById(long id) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ID)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find user by ID: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_EMAIL)) {
            stmt.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find user by email: " + email, e);
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToUser(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list all users", e);
        }
        return list;
    }

    @Override
    public List<User> findByAdvisorId(long advisorId) {
        List<User> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ADVISOR)) {
            stmt.setLong(1, advisorId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find users by advisor ID: " + advisorId, e);
        }
        return list;
    }

    @Override
    public boolean update(User user) {
        try (Connection conn = DBConnection.getConnection()) {
            return update(conn, user);
        } catch (SQLException e) {
            handleSqlException("Failed to update user", e);
            return false;
        }
    }

    @Override
    public boolean update(Connection conn, User user) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPasswordHash());
            stmt.setString(4, user.getRole().name());
            stmt.setBoolean(5, user.isActive());
            if (user.getAdvisorId() != null) {
                stmt.setLong(6, user.getAdvisorId());
            } else {
                stmt.setNull(6, Types.BIGINT);
            }
            stmt.setLong(7, user.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            handleSqlException("Failed to update user with connection", e);
            return false;
        }
    }

    @Override
    public boolean delete(long id) {
        try (Connection conn = DBConnection.getConnection()) {
            return delete(conn, id);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete user: " + id, e);
        }
    }

    @Override
    public boolean delete(Connection conn, long id) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete user with connection: " + id, e);
        }
    }

    @Override
    public boolean updateFailedLogins(long userId, int count, LocalDateTime lockedUntil) {
        try (Connection conn = DBConnection.getConnection()) {
            return updateFailedLogins(conn, userId, count, lockedUntil);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update failed logins for user: " + userId, e);
        }
    }

    @Override
    public boolean updateFailedLogins(Connection conn, long userId, int count, LocalDateTime lockedUntil) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_FAILED_LOGINS)) {
            stmt.setInt(1, count);
            if (lockedUntil != null) {
                stmt.setTimestamp(2, Timestamp.valueOf(lockedUntil));
            } else {
                stmt.setNull(2, Types.TIMESTAMP);
            }
            stmt.setLong(3, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update failed logins with connection: " + userId, e);
        }
    }

    @Override
    public boolean resetFailedLogins(long userId) {
        try (Connection conn = DBConnection.getConnection()) {
            return resetFailedLogins(conn, userId);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to reset failed logins for user: " + userId, e);
        }
    }

    @Override
    public boolean resetFailedLogins(Connection conn, long userId) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_RESET_FAILED_LOGINS)) {
            stmt.setLong(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to reset failed logins with connection: " + userId, e);
        }
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        String roleStr = rs.getString("role");
        Role role = Role.fromString(roleStr);
        User user = User.createWithRole(role);

        user.setId(rs.getLong("id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setActive(rs.getBoolean("active"));
        user.setFailedLogins(rs.getInt("failed_logins"));

        Timestamp lockedTs = rs.getTimestamp("locked_until");
        if (lockedTs != null) {
            user.setLockedUntil(lockedTs.toLocalDateTime());
        }

        long advId = rs.getLong("advisor_id");
        if (!rs.wasNull()) {
            user.setAdvisorId(advId);
        }

        Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) {
            user.setCreatedAt(createdTs.toLocalDateTime());
        }

        return user;
    }

    private void handleSqlException(String message, SQLException e) {
        if ("23000".equals(e.getSQLState()) || e.getErrorCode() == 1062) {
            throw new DuplicateEmailException("Email address already registered: " + e.getMessage(), e);
        }
        throw new DataAccessException(message + ": " + e.getMessage(), e);
    }
}
