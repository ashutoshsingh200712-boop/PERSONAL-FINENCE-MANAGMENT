package com.financeapp.dao.jdbc;

import com.financeapp.dao.IncomeDAO;
import com.financeapp.exception.DataAccessException;
import com.financeapp.model.Income;
import com.financeapp.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of IncomeDAO.
 */
public class JdbcIncomeDAO implements IncomeDAO {

    private static final String SQL_INSERT =
            "INSERT INTO income (user_id, source, amount, frequency, income_date, notes, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT id, user_id, source, amount, frequency, income_date, notes, created_at FROM income WHERE id = ?";

    private static final String SQL_FIND_BY_USER =
            "SELECT id, user_id, source, amount, frequency, income_date, notes, created_at FROM income " +
            "WHERE user_id = ? ORDER BY income_date DESC, id DESC";

    private static final String SQL_FIND_BY_USER_MONTH =
            "SELECT id, user_id, source, amount, frequency, income_date, notes, created_at FROM income " +
            "WHERE user_id = ? AND income_date >= ? AND income_date <= ? ORDER BY income_date DESC";

    private static final String SQL_UPDATE =
            "UPDATE income SET source = ?, amount = ?, frequency = ?, income_date = ?, notes = ? WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM income WHERE id = ?";

    @Override
    public long create(Income income) {
        try (Connection conn = DBConnection.getConnection()) {
            return create(conn, income);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create income: " + e.getMessage(), e);
        }
    }

    @Override
    public long create(Connection conn, Income income) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, income.getUserId());
            stmt.setString(2, income.getSource());
            stmt.setBigDecimal(3, income.getAmount());
            stmt.setString(4, income.getFrequency().name());
            stmt.setDate(5, Date.valueOf(income.getDate()));
            stmt.setString(6, income.getNotes());
            LocalDateTime createdAt = income.getCreatedAt() != null ? income.getCreatedAt() : LocalDateTime.now();
            stmt.setTimestamp(7, Timestamp.valueOf(createdAt));

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new DataAccessException("Creating income failed, no rows affected.");
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    income.setId(id);
                    income.setCreatedAt(createdAt);
                    return id;
                } else {
                    throw new DataAccessException("Creating income failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert income with connection: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Income> findById(long id) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ID)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToIncome(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find income by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Income> findByUserId(long userId) {
        List<Income> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_USER)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToIncome(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find income by user id: " + userId, e);
        }
        return list;
    }

    @Override
    public List<Income> findByUserAndMonth(long userId, String month) {
        YearMonth ym = YearMonth.parse(month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<Income> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_USER_MONTH)) {
            stmt.setLong(1, userId);
            stmt.setDate(2, Date.valueOf(start));
            stmt.setDate(3, Date.valueOf(end));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToIncome(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find income by month: " + month, e);
        }
        return list;
    }

    @Override
    public boolean update(Income income) {
        try (Connection conn = DBConnection.getConnection()) {
            return update(conn, income);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update income: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Connection conn, Income income) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {
            stmt.setString(1, income.getSource());
            stmt.setBigDecimal(2, income.getAmount());
            stmt.setString(3, income.getFrequency().name());
            stmt.setDate(4, Date.valueOf(income.getDate()));
            stmt.setString(5, income.getNotes());
            stmt.setLong(6, income.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update income with connection: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(long id) {
        try (Connection conn = DBConnection.getConnection()) {
            return delete(conn, id);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete income: " + id, e);
        }
    }

    @Override
    public boolean delete(Connection conn, long id) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete income with connection: " + id, e);
        }
    }

    private Income mapRowToIncome(ResultSet rs) throws SQLException {
        Income inc = new Income();
        inc.setId(rs.getLong("id"));
        inc.setUserId(rs.getLong("user_id"));
        inc.setSource(rs.getString("source"));
        inc.setAmount(rs.getBigDecimal("amount"));
        inc.setFrequency(Income.Frequency.fromString(rs.getString("frequency")));

        Date d = rs.getDate("income_date");
        if (d != null) {
            inc.setDate(d.toLocalDate());
        }

        inc.setNotes(rs.getString("notes"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            inc.setCreatedAt(ts.toLocalDateTime());
        }

        return inc;
    }
}
