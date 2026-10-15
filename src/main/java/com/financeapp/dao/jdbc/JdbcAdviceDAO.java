package com.financeapp.dao.jdbc;

import com.financeapp.dao.AdviceDAO;
import com.financeapp.exception.DataAccessException;
import com.financeapp.model.Advice;
import com.financeapp.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of AdviceDAO using PreparedStatements and try-with-resources.
 */
public class JdbcAdviceDAO implements AdviceDAO {

    private static final String SQL_INSERT =
            "INSERT INTO advice (user_id, plan_id, rule_name, title, explanation, status, advisor_comment, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT id, user_id, plan_id, rule_name, title, explanation, status, advisor_comment, created_at " +
            "FROM advice WHERE id = ?";

    private static final String SQL_FIND_BY_USER_ID =
            "SELECT id, user_id, plan_id, rule_name, title, explanation, status, advisor_comment, created_at " +
            "FROM advice WHERE user_id = ? ORDER BY created_at DESC";

    private static final String SQL_FIND_BY_PLAN_ID =
            "SELECT id, user_id, plan_id, rule_name, title, explanation, status, advisor_comment, created_at " +
            "FROM advice WHERE plan_id = ? ORDER BY created_at DESC";

    private static final String SQL_UPDATE_STATUS =
            "UPDATE advice SET status = ? WHERE id = ?";

    private static final String SQL_UPDATE_ADVISOR_COMMENT =
            "UPDATE advice SET advisor_comment = ? WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM advice WHERE id = ?";

    @Override
    public long create(Advice advice) {
        try (Connection conn = DBConnection.getConnection()) {
            return create(conn, advice);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create advice: " + e.getMessage(), e);
        }
    }

    @Override
    public long create(Connection conn, Advice advice) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, advice.getUserId());
            if (advice.getPlanId() != null) {
                stmt.setLong(2, advice.getPlanId());
            } else {
                stmt.setNull(2, Types.BIGINT);
            }
            stmt.setString(3, advice.getRuleName() != null ? advice.getRuleName() : "GENERAL");
            stmt.setString(4, advice.getTitle());
            stmt.setString(5, advice.getExplanation());
            stmt.setString(6, advice.getStatus() != null ? advice.getStatus().name() : Advice.Status.NEW.name());
            if (advice.getAdvisorComment() != null) {
                stmt.setString(7, advice.getAdvisorComment());
            } else {
                stmt.setNull(7, Types.VARCHAR);
            }
            LocalDateTime createdAt = advice.getCreatedAt() != null ? advice.getCreatedAt() : LocalDateTime.now();
            stmt.setTimestamp(8, Timestamp.valueOf(createdAt));

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new DataAccessException("Creating advice failed, no rows affected.");
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    advice.setId(id);
                    advice.setCreatedAt(createdAt);
                    return id;
                } else {
                    throw new DataAccessException("Creating advice failed, no generated key obtained.");
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert advice: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Advice> findById(long id) {
        try (Connection conn = DBConnection.getConnection()) {
            return findById(conn, id);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find advice by id: " + id, e);
        }
    }

    @Override
    public Optional<Advice> findById(Connection conn, long id) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ID)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query advice by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Advice> findByUserId(long userId) {
        try (Connection conn = DBConnection.getConnection()) {
            return findByUserId(conn, userId);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find advice for user: " + userId, e);
        }
    }

    @Override
    public List<Advice> findByUserId(Connection conn, long userId) {
        List<Advice> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_USER_ID)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query advice for user: " + userId, e);
        }
        return list;
    }

    @Override
    public List<Advice> findByPlanId(long planId) {
        try (Connection conn = DBConnection.getConnection()) {
            return findByPlanId(conn, planId);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find advice for plan: " + planId, e);
        }
    }

    @Override
    public List<Advice> findByPlanId(Connection conn, long planId) {
        List<Advice> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_PLAN_ID)) {
            stmt.setLong(1, planId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query advice for plan: " + planId, e);
        }
        return list;
    }

    @Override
    public boolean updateStatus(long id, Advice.Status status) {
        try (Connection conn = DBConnection.getConnection()) {
            return updateStatus(conn, id, status);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update advice status: " + id, e);
        }
    }

    @Override
    public boolean updateStatus(Connection conn, long id, Advice.Status status) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_STATUS)) {
            stmt.setString(1, status != null ? status.name() : Advice.Status.NEW.name());
            stmt.setLong(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to execute update advice status: " + id, e);
        }
    }

    @Override
    public boolean updateAdvisorComment(long id, String comment) {
        try (Connection conn = DBConnection.getConnection()) {
            return updateAdvisorComment(conn, id, comment);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update advisor comment on advice: " + id, e);
        }
    }

    @Override
    public boolean updateAdvisorComment(Connection conn, long id, String comment) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_ADVISOR_COMMENT)) {
            stmt.setString(1, comment);
            stmt.setLong(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to execute update comment on advice: " + id, e);
        }
    }

    @Override
    public boolean delete(long id) {
        try (Connection conn = DBConnection.getConnection()) {
            return delete(conn, id);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete advice: " + id, e);
        }
    }

    @Override
    public boolean delete(Connection conn, long id) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to execute delete on advice: " + id, e);
        }
    }

    private Advice mapRow(ResultSet rs) throws SQLException {
        Advice a = new Advice();
        a.setId(rs.getLong("id"));
        a.setUserId(rs.getLong("user_id"));
        long planId = rs.getLong("plan_id");
        if (!rs.wasNull()) {
            a.setPlanId(planId);
        }
        a.setRuleName(rs.getString("rule_name"));
        a.setTitle(rs.getString("title"));
        a.setExplanation(rs.getString("explanation"));
        a.setStatus(Advice.Status.fromString(rs.getString("status")));
        a.setAdvisorComment(rs.getString("advisor_comment"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            a.setCreatedAt(ts.toLocalDateTime());
        }
        return a;
    }
}
