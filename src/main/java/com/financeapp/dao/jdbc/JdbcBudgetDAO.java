package com.financeapp.dao.jdbc;

import com.financeapp.dao.BudgetDAO;
import com.financeapp.exception.DataAccessException;
import com.financeapp.model.Budget;
import com.financeapp.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of BudgetDAO with resilient source tracking and transaction support.
 */
public class JdbcBudgetDAO implements BudgetDAO {

    private static final String SQL_INSERT_WITH_SOURCE =
            "INSERT INTO budgets (user_id, category_id, budget_month, budget_limit, alert_threshold, source, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_INSERT_LEGACY =
            "INSERT INTO budgets (user_id, category_id, budget_month, budget_limit, alert_threshold, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT b.*, c.name AS category_name " +
            "FROM budgets b LEFT JOIN categories c ON b.category_id = c.id WHERE b.id = ?";

    private static final String SQL_FIND_BY_USER_CAT_MONTH =
            "SELECT b.*, c.name AS category_name " +
            "FROM budgets b LEFT JOIN categories c ON b.category_id = c.id " +
            "WHERE b.user_id = ? AND b.category_id = ? AND b.budget_month = ?";

    private static final String SQL_FIND_BY_USER_MONTH =
            "SELECT b.*, c.name AS category_name " +
            "FROM budgets b LEFT JOIN categories c ON b.category_id = c.id " +
            "WHERE b.user_id = ? AND b.budget_month = ? ORDER BY c.name ASC";

    private static final String SQL_UPDATE_WITH_SOURCE =
            "UPDATE budgets SET budget_limit = ?, alert_threshold = ?, source = ? WHERE id = ?";

    private static final String SQL_UPDATE_LEGACY =
            "UPDATE budgets SET budget_limit = ?, alert_threshold = ? WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM budgets WHERE id = ?";

    private static volatile boolean supportsSourceColumn = true;

    @Override
    public long create(Budget budget) {
        try (Connection conn = DBConnection.getConnection()) {
            return create(conn, budget);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create budget: " + e.getMessage(), e);
        }
    }

    @Override
    public long create(Connection conn, Budget budget) {
        LocalDateTime createdAt = budget.getCreatedAt() != null ? budget.getCreatedAt() : LocalDateTime.now();
        if (supportsSourceColumn) {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_WITH_SOURCE, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setLong(1, budget.getUserId());
                stmt.setLong(2, budget.getCategoryId());
                stmt.setString(3, budget.getBudgetMonth());
                stmt.setBigDecimal(4, budget.getBudgetLimit());
                stmt.setBigDecimal(5, budget.getAlertThreshold());
                stmt.setString(6, budget.getSource());
                stmt.setTimestamp(7, Timestamp.valueOf(createdAt));

                int affected = stmt.executeUpdate();
                if (affected == 0) {
                    throw new DataAccessException("Creating budget failed, no rows affected.");
                }

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        long id = keys.getLong(1);
                        budget.setId(id);
                        budget.setCreatedAt(createdAt);
                        return id;
                    }
                }
            } catch (SQLException e) {
                // If column doesn't exist, flag legacy mode and try fallback
                if (isMissingColumnError(e)) {
                    supportsSourceColumn = false;
                } else {
                    throw new DataAccessException("Failed to insert budget: " + e.getMessage(), e);
                }
            }
        }

        // Legacy fallback without source column
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_LEGACY, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, budget.getUserId());
            stmt.setLong(2, budget.getCategoryId());
            stmt.setString(3, budget.getBudgetMonth());
            stmt.setBigDecimal(4, budget.getBudgetLimit());
            stmt.setBigDecimal(5, budget.getAlertThreshold());
            stmt.setTimestamp(6, Timestamp.valueOf(createdAt));

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new DataAccessException("Creating budget failed, no rows affected.");
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    budget.setId(id);
                    budget.setCreatedAt(createdAt);
                    return id;
                } else {
                    throw new DataAccessException("Creating budget failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert legacy budget: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Budget> findById(long id) {
        try (Connection conn = DBConnection.getConnection()) {
            return findById(conn, id);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find budget by id: " + id, e);
        }
    }

    @Override
    public Optional<Budget> findById(Connection conn, long id) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ID)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToBudget(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query budget by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Budget> findByUserCategoryAndMonth(long userId, long categoryId, String month) {
        try (Connection conn = DBConnection.getConnection()) {
            return findByUserCategoryAndMonth(conn, userId, categoryId, month);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find budget by user, category and month", e);
        }
    }

    @Override
    public Optional<Budget> findByUserCategoryAndMonth(Connection conn, long userId, long categoryId, String month) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_USER_CAT_MONTH)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, categoryId);
            stmt.setString(3, month);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToBudget(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query budget by user, category and month", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Budget> findByUserAndMonth(long userId, String month) {
        try (Connection conn = DBConnection.getConnection()) {
            return findByUserAndMonth(conn, userId, month);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find budgets for month: " + month, e);
        }
    }

    @Override
    public List<Budget> findByUserAndMonth(Connection conn, long userId, String month) {
        List<Budget> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_USER_MONTH)) {
            stmt.setLong(1, userId);
            stmt.setString(2, month);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToBudget(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query budgets for month: " + month, e);
        }
        return list;
    }

    @Override
    public boolean update(Budget budget) {
        try (Connection conn = DBConnection.getConnection()) {
            return update(conn, budget);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update budget: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Connection conn, Budget budget) {
        if (supportsSourceColumn) {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_WITH_SOURCE)) {
                stmt.setBigDecimal(1, budget.getBudgetLimit());
                stmt.setBigDecimal(2, budget.getAlertThreshold());
                stmt.setString(3, budget.getSource());
                stmt.setLong(4, budget.getId());
                return stmt.executeUpdate() > 0;
            } catch (SQLException e) {
                if (isMissingColumnError(e)) {
                    supportsSourceColumn = false;
                } else {
                    throw new DataAccessException("Failed to update budget: " + e.getMessage(), e);
                }
            }
        }

        try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_LEGACY)) {
            stmt.setBigDecimal(1, budget.getBudgetLimit());
            stmt.setBigDecimal(2, budget.getAlertThreshold());
            stmt.setLong(3, budget.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to execute legacy budget update: " + e.getMessage(), e);
        }
    }

    @Override
    public long upsert(Budget budget) {
        try (Connection conn = DBConnection.getConnection()) {
            return upsert(conn, budget);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to upsert budget: " + e.getMessage(), e);
        }
    }

    @Override
    public long upsert(Connection conn, Budget budget) {
        Optional<Budget> existing = findByUserCategoryAndMonth(conn, budget.getUserId(), budget.getCategoryId(), budget.getBudgetMonth());
        if (existing.isPresent()) {
            Budget b = existing.get();
            b.setBudgetLimit(budget.getBudgetLimit());
            b.setAlertThreshold(budget.getAlertThreshold());
            b.setSource(budget.getSource());
            update(conn, b);
            budget.setId(b.getId());
            return b.getId();
        } else {
            return create(conn, budget);
        }
    }

    @Override
    public boolean delete(long id) {
        try (Connection conn = DBConnection.getConnection()) {
            return delete(conn, id);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete budget: " + id, e);
        }
    }

    @Override
    public boolean delete(Connection conn, long id) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete budget with connection: " + id, e);
        }
    }

    private boolean isMissingColumnError(SQLException e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return msg.contains("unknown column 'source'") ||
                msg.contains("column \"source\" not found") ||
                "42S22".equalsIgnoreCase(e.getSQLState());
    }

    private Budget mapRowToBudget(ResultSet rs) throws SQLException {
        Budget b = new Budget();
        b.setId(rs.getLong("id"));
        b.setUserId(rs.getLong("user_id"));
        b.setCategoryId(rs.getLong("category_id"));
        try {
            b.setCategoryName(rs.getString("category_name"));
        } catch (SQLException ignored) {}
        b.setBudgetMonth(rs.getString("budget_month"));
        b.setBudgetLimit(rs.getBigDecimal("budget_limit"));
        b.setAlertThreshold(rs.getBigDecimal("alert_threshold"));
        try {
            String src = rs.getString("source");
            if (src != null) {
                b.setSource(src);
            }
        } catch (SQLException ignored) {}
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            b.setCreatedAt(ts.toLocalDateTime());
        }
        return b;
    }
}
