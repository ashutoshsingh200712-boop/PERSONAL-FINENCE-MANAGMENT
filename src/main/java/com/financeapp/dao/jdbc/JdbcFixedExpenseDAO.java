package com.financeapp.dao.jdbc;

import com.financeapp.dao.FixedExpenseDAO;
import com.financeapp.exception.DataAccessException;
import com.financeapp.model.FixedExpense;
import com.financeapp.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of FixedExpenseDAO.
 */
public class JdbcFixedExpenseDAO implements FixedExpenseDAO {

    private static final String SQL_INSERT =
            "INSERT INTO fixed_expenses (user_id, category_id, title, amount, due_day, is_active, notes, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT fe.id, fe.user_id, fe.category_id, c.name AS category_name, fe.title, fe.amount, fe.due_day, fe.is_active, fe.notes, fe.created_at " +
            "FROM fixed_expenses fe LEFT JOIN categories c ON fe.category_id = c.id WHERE fe.id = ?";

    private static final String SQL_FIND_BY_USER =
            "SELECT fe.id, fe.user_id, fe.category_id, c.name AS category_name, fe.title, fe.amount, fe.due_day, fe.is_active, fe.notes, fe.created_at " +
            "FROM fixed_expenses fe LEFT JOIN categories c ON fe.category_id = c.id WHERE fe.user_id = ? ORDER BY fe.due_day ASC, fe.id ASC";

    private static final String SQL_FIND_ACTIVE_BY_USER =
            "SELECT fe.id, fe.user_id, fe.category_id, c.name AS category_name, fe.title, fe.amount, fe.due_day, fe.is_active, fe.notes, fe.created_at " +
            "FROM fixed_expenses fe LEFT JOIN categories c ON fe.category_id = c.id WHERE fe.user_id = ? AND fe.is_active = TRUE ORDER BY fe.due_day ASC";

    private static final String SQL_UPDATE =
            "UPDATE fixed_expenses SET category_id = ?, title = ?, amount = ?, due_day = ?, is_active = ?, notes = ? " +
            "WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM fixed_expenses WHERE id = ?";

    @Override
    public long create(FixedExpense fe) {
        try (Connection conn = DBConnection.getConnection()) {
            return create(conn, fe);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create fixed expense: " + e.getMessage(), e);
        }
    }

    @Override
    public long create(Connection conn, FixedExpense fe) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, fe.getUserId());
            stmt.setLong(2, fe.getCategoryId());
            stmt.setString(3, fe.getTitle());
            stmt.setBigDecimal(4, fe.getAmount());
            stmt.setInt(5, fe.getDueDay());
            stmt.setBoolean(6, fe.isActive());
            stmt.setString(7, fe.getNotes());
            LocalDateTime createdAt = fe.getCreatedAt() != null ? fe.getCreatedAt() : LocalDateTime.now();
            stmt.setTimestamp(8, Timestamp.valueOf(createdAt));

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new DataAccessException("Creating fixed expense failed, no rows affected.");
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    fe.setId(id);
                    fe.setCreatedAt(createdAt);
                    return id;
                } else {
                    throw new DataAccessException("Creating fixed expense failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert fixed expense with connection: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<FixedExpense> findById(long id) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ID)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToFixedExpense(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find fixed expense by ID: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<FixedExpense> findByUserId(long userId) {
        List<FixedExpense> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_USER)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToFixedExpense(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find fixed expenses by user ID: " + userId, e);
        }
        return list;
    }

    @Override
    public List<FixedExpense> findActiveByUserId(long userId) {
        List<FixedExpense> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_ACTIVE_BY_USER)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToFixedExpense(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find active fixed expenses by user ID: " + userId, e);
        }
        return list;
    }

    @Override
    public boolean update(FixedExpense fe) {
        try (Connection conn = DBConnection.getConnection()) {
            return update(conn, fe);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update fixed expense: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Connection conn, FixedExpense fe) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {
            stmt.setLong(1, fe.getCategoryId());
            stmt.setString(2, fe.getTitle());
            stmt.setBigDecimal(3, fe.getAmount());
            stmt.setInt(4, fe.getDueDay());
            stmt.setBoolean(5, fe.isActive());
            stmt.setString(6, fe.getNotes());
            stmt.setLong(7, fe.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update fixed expense with connection: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(long id) {
        try (Connection conn = DBConnection.getConnection()) {
            return delete(conn, id);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete fixed expense: " + id, e);
        }
    }

    @Override
    public boolean delete(Connection conn, long id) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete fixed expense with connection: " + id, e);
        }
    }

    private FixedExpense mapRowToFixedExpense(ResultSet rs) throws SQLException {
        FixedExpense fe = new FixedExpense();
        fe.setId(rs.getLong("id"));
        fe.setUserId(rs.getLong("user_id"));
        fe.setCategoryId(rs.getLong("category_id"));
        fe.setCategoryName(rs.getString("category_name"));
        fe.setTitle(rs.getString("title"));
        fe.setAmount(rs.getBigDecimal("amount"));
        fe.setDueDay(rs.getInt("due_day"));
        fe.setActive(rs.getBoolean("is_active"));
        fe.setNotes(rs.getString("notes"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            fe.setCreatedAt(ts.toLocalDateTime());
        }

        return fe;
    }
}
