package com.financeapp.dao.jdbc;

import com.financeapp.dao.GoalDAO;
import com.financeapp.exception.DataAccessException;
import com.financeapp.model.Goal;
import com.financeapp.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of GoalDAO using PreparedStatements and try-with-resources.
 */
public class JdbcGoalDAO implements GoalDAO {

    private static final String SQL_INSERT =
            "INSERT INTO goals (user_id, name, target_amount, current_amount, target_date, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT id, user_id, name, target_amount, current_amount, target_date, created_at FROM goals WHERE id = ?";

    private static final String SQL_FIND_BY_USER_ID =
            "SELECT id, user_id, name, target_amount, current_amount, target_date, created_at FROM goals WHERE user_id = ? ORDER BY target_date ASC";

    private static final String SQL_UPDATE =
            "UPDATE goals SET name = ?, target_amount = ?, current_amount = ?, target_date = ? WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM goals WHERE id = ?";

    @Override
    public long create(Goal goal) {
        try (Connection conn = DBConnection.getConnection()) {
            return create(conn, goal);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create goal: " + e.getMessage(), e);
        }
    }

    @Override
    public long create(Connection conn, Goal goal) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, goal.getUserId());
            stmt.setString(2, goal.getName());
            stmt.setBigDecimal(3, goal.getTargetAmount());
            stmt.setBigDecimal(4, goal.getCurrentAmount());
            stmt.setDate(5, Date.valueOf(goal.getTargetDate()));
            LocalDateTime createdAt = goal.getCreatedAt() != null ? goal.getCreatedAt() : LocalDateTime.now();
            stmt.setTimestamp(6, Timestamp.valueOf(createdAt));

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new DataAccessException("Creating goal failed, no rows affected.");
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    goal.setId(id);
                    goal.setCreatedAt(createdAt);
                    return id;
                } else {
                    throw new DataAccessException("Creating goal failed, no generated key obtained.");
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert goal: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Goal> findById(long id) {
        try (Connection conn = DBConnection.getConnection()) {
            return findById(conn, id);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find goal by id: " + id, e);
        }
    }

    @Override
    public Optional<Goal> findById(Connection conn, long id) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ID)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query goal by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Goal> findByUserId(long userId) {
        try (Connection conn = DBConnection.getConnection()) {
            return findByUserId(conn, userId);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find goals for user: " + userId, e);
        }
    }

    @Override
    public List<Goal> findByUserId(Connection conn, long userId) {
        List<Goal> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_USER_ID)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query goals for user: " + userId, e);
        }
        return list;
    }

    @Override
    public boolean update(Goal goal) {
        try (Connection conn = DBConnection.getConnection()) {
            return update(conn, goal);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update goal: " + goal.getId(), e);
        }
    }

    @Override
    public boolean update(Connection conn, Goal goal) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {
            stmt.setString(1, goal.getName());
            stmt.setBigDecimal(2, goal.getTargetAmount());
            stmt.setBigDecimal(3, goal.getCurrentAmount());
            stmt.setDate(4, Date.valueOf(goal.getTargetDate()));
            stmt.setLong(5, goal.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to execute update on goal: " + goal.getId(), e);
        }
    }

    @Override
    public boolean delete(long id) {
        try (Connection conn = DBConnection.getConnection()) {
            return delete(conn, id);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete goal: " + id, e);
        }
    }

    @Override
    public boolean delete(Connection conn, long id) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to execute delete on goal: " + id, e);
        }
    }

    private Goal mapRow(ResultSet rs) throws SQLException {
        Goal g = new Goal();
        g.setId(rs.getLong("id"));
        g.setUserId(rs.getLong("user_id"));
        g.setName(rs.getString("name"));
        g.setTargetAmount(rs.getBigDecimal("target_amount"));
        g.setCurrentAmount(rs.getBigDecimal("current_amount"));
        Date d = rs.getDate("target_date");
        if (d != null) {
            g.setTargetDate(d.toLocalDate());
        }
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            g.setCreatedAt(ts.toLocalDateTime());
        }
        return g;
    }
}
