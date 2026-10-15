package com.financeapp.dao.jdbc;

import com.financeapp.dao.FeedbackDAO;
import com.financeapp.exception.DataAccessException;
import com.financeapp.model.Feedback;
import com.financeapp.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of FeedbackDAO using PreparedStatements and try-with-resources.
 */
public class JdbcFeedbackDAO implements FeedbackDAO {

    private static final String SQL_INSERT =
            "INSERT INTO feedback (user_id, subject, body, status, admin_reply, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT f.id, f.user_id, f.subject, f.body, f.status, f.admin_reply, f.created_at, " +
            "u.name AS user_name, u.email AS user_email " +
            "FROM feedback f " +
            "LEFT JOIN users u ON f.user_id = u.id " +
            "WHERE f.id = ?";

    private static final String SQL_FIND_ALL =
            "SELECT f.id, f.user_id, f.subject, f.body, f.status, f.admin_reply, f.created_at, " +
            "u.name AS user_name, u.email AS user_email " +
            "FROM feedback f " +
            "LEFT JOIN users u ON f.user_id = u.id " +
            "ORDER BY f.created_at DESC";

    private static final String SQL_FIND_BY_USER_ID =
            "SELECT f.id, f.user_id, f.subject, f.body, f.status, f.admin_reply, f.created_at, " +
            "u.name AS user_name, u.email AS user_email " +
            "FROM feedback f " +
            "LEFT JOIN users u ON f.user_id = u.id " +
            "WHERE f.user_id = ? " +
            "ORDER BY f.created_at DESC";

    private static final String SQL_REPLY =
            "UPDATE feedback SET admin_reply = ? WHERE id = ?";

    private static final String SQL_UPDATE_STATUS =
            "UPDATE feedback SET status = ? WHERE id = ?";

    private static final String SQL_REPLY_AND_CLOSE =
            "UPDATE feedback SET admin_reply = ?, status = ? WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM feedback WHERE id = ?";

    @Override
    public long create(Feedback feedback) {
        try (Connection conn = DBConnection.getConnection()) {
            return create(conn, feedback);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create feedback: " + e.getMessage(), e);
        }
    }

    @Override
    public long create(Connection conn, Feedback feedback) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, feedback.getUserId());
            stmt.setString(2, feedback.getSubject());
            stmt.setString(3, feedback.getBody());
            stmt.setString(4, feedback.getStatus() != null ? feedback.getStatus().name() : Feedback.Status.OPEN.name());
            if (feedback.getAdminReply() != null) {
                stmt.setString(5, feedback.getAdminReply());
            } else {
                stmt.setNull(5, Types.VARCHAR);
            }
            LocalDateTime createdAt = feedback.getCreatedAt() != null ? feedback.getCreatedAt() : LocalDateTime.now();
            stmt.setTimestamp(6, Timestamp.valueOf(createdAt));

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new DataAccessException("Creating feedback failed, no rows affected.");
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    feedback.setId(id);
                    feedback.setCreatedAt(createdAt);
                    return id;
                } else {
                    throw new DataAccessException("Creating feedback failed, no generated key obtained.");
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert feedback: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Feedback> findById(long id) {
        try (Connection conn = DBConnection.getConnection()) {
            return findById(conn, id);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find feedback by id: " + id, e);
        }
    }

    @Override
    public Optional<Feedback> findById(Connection conn, long id) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ID)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query feedback by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Feedback> findAll() {
        try (Connection conn = DBConnection.getConnection()) {
            return findAll(conn);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load all feedback: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Feedback> findAll(Connection conn) {
        List<Feedback> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query all feedback: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Feedback> findByUserId(long userId) {
        try (Connection conn = DBConnection.getConnection()) {
            return findByUserId(conn, userId);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find feedback for user: " + userId, e);
        }
    }

    @Override
    public List<Feedback> findByUserId(Connection conn, long userId) {
        List<Feedback> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_USER_ID)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query feedback for user: " + userId, e);
        }
        return list;
    }

    @Override
    public boolean reply(long id, String adminReply) {
        try (Connection conn = DBConnection.getConnection()) {
            return reply(conn, id, adminReply);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to reply to feedback: " + id, e);
        }
    }

    @Override
    public boolean reply(Connection conn, long id, String adminReply) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_REPLY)) {
            stmt.setString(1, adminReply);
            stmt.setLong(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to execute reply to feedback: " + id, e);
        }
    }

    @Override
    public boolean updateStatus(long id, Feedback.Status status) {
        try (Connection conn = DBConnection.getConnection()) {
            return updateStatus(conn, id, status);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update feedback status: " + id, e);
        }
    }

    @Override
    public boolean updateStatus(Connection conn, long id, Feedback.Status status) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_STATUS)) {
            stmt.setString(1, status != null ? status.name() : Feedback.Status.OPEN.name());
            stmt.setLong(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to execute update status on feedback: " + id, e);
        }
    }

    @Override
    public boolean replyAndClose(long id, String adminReply, Feedback.Status status) {
        try (Connection conn = DBConnection.getConnection()) {
            return replyAndClose(conn, id, adminReply, status);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to reply and update feedback: " + id, e);
        }
    }

    @Override
    public boolean replyAndClose(Connection conn, long id, String adminReply, Feedback.Status status) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_REPLY_AND_CLOSE)) {
            stmt.setString(1, adminReply);
            stmt.setString(2, status != null ? status.name() : Feedback.Status.CLOSED.name());
            stmt.setLong(3, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to execute replyAndClose on feedback: " + id, e);
        }
    }

    @Override
    public boolean delete(long id) {
        try (Connection conn = DBConnection.getConnection()) {
            return delete(conn, id);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete feedback: " + id, e);
        }
    }

    @Override
    public boolean delete(Connection conn, long id) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to execute delete on feedback: " + id, e);
        }
    }

    private Feedback mapRow(ResultSet rs) throws SQLException {
        Feedback f = new Feedback();
        f.setId(rs.getLong("id"));
        f.setUserId(rs.getLong("user_id"));
        f.setSubject(rs.getString("subject"));
        f.setBody(rs.getString("body"));
        f.setStatus(Feedback.Status.fromString(rs.getString("status")));
        f.setAdminReply(rs.getString("admin_reply"));
        f.setUserName(rs.getString("user_name"));
        f.setUserEmail(rs.getString("user_email"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            f.setCreatedAt(ts.toLocalDateTime());
        }
        return f;
    }
}
