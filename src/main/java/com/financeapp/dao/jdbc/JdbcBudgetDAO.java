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
 * JDBC implementation of BudgetDAO.
 */
public class JdbcBudgetDAO implements BudgetDAO {

    private static final String SQL_INSERT =
            "INSERT INTO budgets (user_id, category_id, budget_month, budget_limit, alert_threshold, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT b.id, b.user_id, b.category_id, c.name AS category_name, b.budget_month, b.budget_limit, b.alert_threshold, b.created_at " +
            "FROM budgets b LEFT JOIN categories c ON b.category_id = c.id WHERE b.id = ?";

    private static final String SQL_FIND_BY_USER_CAT_MONTH =
            "SELECT b.id, b.user_id, b.category_id, c.name AS category_name, b.budget_month, b.budget_limit, b.alert_threshold, b.created_at " +
            "FROM budgets b LEFT JOIN categories c ON b.category_id = c.id " +
            "WHERE b.user_id = ? AND b.category_id = ? AND b.budget_month = ?";

    private static final String SQL_FIND_BY_USER_MONTH =
            "SELECT b.id, b.user_id, b.category_id, c.name AS category_name, b.budget_month, b.budget_limit, b.alert_threshold, b.created_at " +
            "FROM budgets b LEFT JOIN categories c ON b.category_id = c.id " +
            "WHERE b.user_id = ? AND b.budget_month = ? ORDER BY c.name ASC";

    private static final String SQL_UPDATE =
            "UPDATE budgets SET budget_limit = ?, alert_threshold = ? WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM budgets WHERE id = ?";

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
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, budget.getUserId());
            stmt.setLong(2, budget.getCategoryId());
            stmt.setString(3, budget.getBudgetMonth());
            stmt.setBigDecimal(4, budget.getBudgetLimit());
            stmt.setBigDecimal(5, budget.getAlertThreshold());
            LocalDateTime createdAt = budget.getCreatedAt() != null ? budget.getCreatedAt() : LocalDateTime.now();
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
            throw new DataAccessException("Failed to insert budget with connection: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Budget> findById(long id) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ID)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToBudget(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find budget by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Budget> findByUserCategoryAndMonth(long userId, long categoryId, String month) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_USER_CAT_MONTH)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, categoryId);
            stmt.setString(3, month);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToBudget(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find budget by user, category and month", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Budget> findByUserAndMonth(long userId, String month) {
        List<Budget> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_USER_MONTH)) {
            stmt.setLong(1, userId);
            stmt.setString(2, month);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToBudget(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find budgets for month: " + month, e);
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
        try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {
            stmt.setBigDecimal(1, budget.getBudgetLimit());
            stmt.setBigDecimal(2, budget.getAlertThreshold());
            stmt.setLong(3, budget.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update budget with connection: " + e.getMessage(), e);
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

    private Budget mapRowToBudget(ResultSet rs) throws SQLException {
        Budget b = new Budget();
        b.setId(rs.getLong("id"));
        b.setUserId(rs.getLong("user_id"));
        b.setCategoryId(rs.getLong("category_id"));
        b.setCategoryName(rs.getString("category_name"));
        b.setBudgetMonth(rs.getString("budget_month"));
        b.setBudgetLimit(rs.getBigDecimal("budget_limit"));
        b.setAlertThreshold(rs.getBigDecimal("alert_threshold"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            b.setCreatedAt(ts.toLocalDateTime());
        }
        return b;
    }
}
