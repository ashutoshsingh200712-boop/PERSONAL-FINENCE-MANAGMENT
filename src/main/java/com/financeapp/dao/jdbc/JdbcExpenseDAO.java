package com.financeapp.dao.jdbc;

import com.financeapp.dao.ExpenseDAO;
import com.financeapp.exception.DataAccessException;
import com.financeapp.model.CategorySpendingSummary;
import com.financeapp.model.Expense;
import com.financeapp.util.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of ExpenseDAO.
 */
public class JdbcExpenseDAO implements ExpenseDAO {

    private static final String SQL_INSERT =
            "INSERT INTO expenses (user_id, category_id, amount, expense_date, description, payment_method, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT e.id, e.user_id, e.category_id, c.name AS category_name, e.amount, e.expense_date, e.description, e.payment_method, e.created_at " +
            "FROM expenses e LEFT JOIN categories c ON e.category_id = c.id WHERE e.id = ?";

    private static final String SQL_FIND_BY_USER =
            "SELECT e.id, e.user_id, e.category_id, c.name AS category_name, e.amount, e.expense_date, e.description, e.payment_method, e.created_at " +
            "FROM expenses e LEFT JOIN categories c ON e.category_id = c.id WHERE e.user_id = ? " +
            "ORDER BY e.expense_date DESC, e.id DESC LIMIT ? OFFSET ?";

    private static final String SQL_UPDATE =
            "UPDATE expenses SET category_id = ?, amount = ?, expense_date = ?, description = ?, payment_method = ? " +
            "WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM expenses WHERE id = ?";

    private static final String SQL_GROUP_BY_CATEGORY_MONTH =
            "SELECT c.id AS cat_id, c.name AS cat_name, COALESCE(SUM(e.amount), 0) AS total_amount, COUNT(e.id) AS tx_count " +
            "FROM expenses e " +
            "JOIN categories c ON e.category_id = c.id " +
            "WHERE e.user_id = ? AND e.expense_date >= ? AND e.expense_date <= ? " +
            "GROUP BY c.id, c.name " +
            "ORDER BY total_amount DESC";

    @Override
    public long create(Expense expense) {
        try (Connection conn = DBConnection.getConnection()) {
            return create(conn, expense);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create expense: " + e.getMessage(), e);
        }
    }

    @Override
    public long create(Connection conn, Expense expense) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, expense.getUserId());
            stmt.setLong(2, expense.getCategoryId());
            stmt.setBigDecimal(3, expense.getAmount());
            stmt.setDate(4, Date.valueOf(expense.getDate()));
            stmt.setString(5, expense.getDescription());
            stmt.setString(6, expense.getPaymentMethod());
            LocalDateTime createdAt = expense.getCreatedAt() != null ? expense.getCreatedAt() : LocalDateTime.now();
            stmt.setTimestamp(7, Timestamp.valueOf(createdAt));

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new DataAccessException("Creating expense failed, no rows affected.");
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    expense.setId(id);
                    expense.setCreatedAt(createdAt);
                    return id;
                } else {
                    throw new DataAccessException("Creating expense failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert expense with connection: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Expense> findById(long id) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ID)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToExpense(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find expense by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Expense> findByUserId(long userId, int limit, int offset) {
        List<Expense> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_USER)) {
            stmt.setLong(1, userId);
            stmt.setInt(2, Math.max(limit, 1));
            stmt.setInt(3, Math.max(offset, 0));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToExpense(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find expenses by user id: " + userId, e);
        }
        return list;
    }

    @Override
    public List<Expense> findByMonthAndCategory(long userId, String month, Long categoryId, int limit, int offset) {
        try (Connection conn = DBConnection.getConnection()) {
            return findByMonthAndCategory(conn, userId, month, categoryId, limit, offset);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to filter expenses by month and category", e);
        }
    }

    @Override
    public List<Expense> findByMonthAndCategory(Connection conn, long userId, String month, Long categoryId, int limit, int offset) {
        YearMonth ym = parseYearMonth(month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        StringBuilder sql = new StringBuilder(
                "SELECT e.id, e.user_id, e.category_id, c.name AS category_name, e.amount, e.expense_date, e.description, e.payment_method, e.created_at " +
                "FROM expenses e LEFT JOIN categories c ON e.category_id = c.id " +
                "WHERE e.user_id = ? AND e.expense_date >= ? AND e.expense_date <= ? "
        );

        if (categoryId != null && categoryId > 0) {
            sql.append("AND e.category_id = ? ");
        }
        sql.append("ORDER BY e.expense_date DESC, e.id DESC LIMIT ? OFFSET ?");

        List<Expense> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            stmt.setLong(paramIndex++, userId);
            stmt.setDate(paramIndex++, Date.valueOf(start));
            stmt.setDate(paramIndex++, Date.valueOf(end));
            if (categoryId != null && categoryId > 0) {
                stmt.setLong(paramIndex++, categoryId);
            }
            stmt.setInt(paramIndex++, Math.max(limit, 1));
            stmt.setInt(paramIndex, Math.max(offset, 0));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToExpense(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to filter expenses by month and category with connection: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public int countByMonthAndCategory(long userId, String month, Long categoryId) {
        YearMonth ym = parseYearMonth(month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(e.id) FROM expenses e WHERE e.user_id = ? AND e.expense_date >= ? AND e.expense_date <= ? "
        );

        if (categoryId != null && categoryId > 0) {
            sql.append("AND e.category_id = ?");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            stmt.setLong(paramIndex++, userId);
            stmt.setDate(paramIndex++, Date.valueOf(start));
            stmt.setDate(paramIndex++, Date.valueOf(end));
            if (categoryId != null && categoryId > 0) {
                stmt.setLong(paramIndex, categoryId);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to count expenses by month and category", e);
        }
        return 0;
    }

    @Override
    public List<CategorySpendingSummary> getCategorySpendingInMonth(long userId, String month) {
        YearMonth ym = parseYearMonth(month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<CategorySpendingSummary> summaries = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GROUP_BY_CATEGORY_MONTH)) {
            stmt.setLong(1, userId);
            stmt.setDate(2, Date.valueOf(start));
            stmt.setDate(3, Date.valueOf(end));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    CategorySpendingSummary summary = new CategorySpendingSummary();
                    summary.setCategoryId(rs.getLong("cat_id"));
                    summary.setCategoryName(rs.getString("cat_name"));
                    summary.setTotalAmount(rs.getBigDecimal("total_amount"));
                    summary.setTransactionCount(rs.getInt("tx_count"));
                    summaries.add(summary);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve category spending summary: " + e.getMessage(), e);
        }
        return summaries;
    }

    @Override
    public boolean update(Expense expense) {
        try (Connection conn = DBConnection.getConnection()) {
            return update(conn, expense);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update expense: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Connection conn, Expense expense) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {
            stmt.setLong(1, expense.getCategoryId());
            stmt.setBigDecimal(2, expense.getAmount());
            stmt.setDate(3, Date.valueOf(expense.getDate()));
            stmt.setString(4, expense.getDescription());
            stmt.setString(5, expense.getPaymentMethod());
            stmt.setLong(6, expense.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update expense with connection: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(long id) {
        try (Connection conn = DBConnection.getConnection()) {
            return delete(conn, id);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete expense: " + id, e);
        }
    }

    @Override
    public boolean delete(Connection conn, long id) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete expense with connection: " + id, e);
        }
    }

    private Expense mapRowToExpense(ResultSet rs) throws SQLException {
        Expense expense = new Expense();
        expense.setId(rs.getLong("id"));
        expense.setUserId(rs.getLong("user_id"));
        expense.setCategoryId(rs.getLong("category_id"));
        expense.setCategoryName(rs.getString("category_name"));
        expense.setAmount(rs.getBigDecimal("amount"));

        Date expDate = rs.getDate("expense_date");
        if (expDate != null) {
            expense.setDate(expDate.toLocalDate());
        }

        expense.setDescription(rs.getString("description"));
        expense.setPaymentMethod(rs.getString("payment_method"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            expense.setCreatedAt(ts.toLocalDateTime());
        }

        return expense;
    }

    private YearMonth parseYearMonth(String month) {
        if (month == null || !month.matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
            return YearMonth.now();
        }
        return YearMonth.parse(month);
    }
}
