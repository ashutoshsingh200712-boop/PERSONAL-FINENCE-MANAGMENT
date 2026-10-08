package com.financeapp.dao.jdbc;

import com.financeapp.dao.NotificationDAO;
import com.financeapp.exception.DataAccessException;
import com.financeapp.model.Notification;
import com.financeapp.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of NotificationDAO.
 */
public class JdbcNotificationDAO implements NotificationDAO {

    private static final String SQL_INSERT =
            "INSERT INTO notifications (user_id, title, message, type, is_read, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT id, user_id, title, message, type, is_read, created_at FROM notifications WHERE id = ?";

    private static final String SQL_FIND_BY_USER =
            "SELECT id, user_id, title, message, type, is_read, created_at FROM notifications " +
            "WHERE user_id = ? ORDER BY created_at DESC, id DESC";

    private static final String SQL_FIND_UNREAD_BY_USER =
            "SELECT id, user_id, title, message, type, is_read, created_at FROM notifications " +
            "WHERE user_id = ? AND is_read = FALSE ORDER BY created_at DESC, id DESC";

    private static final String SQL_MARK_AS_READ =
            "UPDATE notifications SET is_read = TRUE WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM notifications WHERE id = ?";

    @Override
    public long create(Notification notification) {
        try (Connection conn = DBConnection.getConnection()) {
            return create(conn, notification);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create notification: " + e.getMessage(), e);
        }
    }

    @Override
    public long create(Connection conn, Notification notification) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, notification.getUserId());
            stmt.setString(2, notification.getTitle());
            stmt.setString(3, notification.getMessage());
            stmt.setString(4, notification.getType().name());
            stmt.setBoolean(5, notification.isRead());
            LocalDateTime createdAt = notification.getCreatedAt() != null ? notification.getCreatedAt() : LocalDateTime.now();
            stmt.setTimestamp(6, Timestamp.valueOf(createdAt));

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new DataAccessException("Creating notification failed, no rows affected.");
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    notification.setId(id);
                    notification.setCreatedAt(createdAt);
                    return id;
                } else {
                    throw new DataAccessException("Creating notification failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert notification with connection: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Notification> findById(long id) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ID)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find notification by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Notification> findByUserId(long userId) {
        List<Notification> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_USER)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find notifications for user: " + userId, e);
        }
        return list;
    }

    @Override
    public List<Notification> findUnreadByUserId(long userId) {
        List<Notification> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_UNREAD_BY_USER)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find unread notifications for user: " + userId, e);
        }
        return list;
    }

    @Override
    public boolean markAsRead(long id) {
        try (Connection conn = DBConnection.getConnection()) {
            return markAsRead(conn, id);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to mark notification as read: " + id, e);
        }
    }

    @Override
    public boolean markAsRead(Connection conn, long id) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_MARK_AS_READ)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to mark notification as read with connection: " + id, e);
        }
    }

    @Override
    public boolean delete(long id) {
        try (Connection conn = DBConnection.getConnection()) {
            return delete(conn, id);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete notification: " + id, e);
        }
    }

    @Override
    public boolean delete(Connection conn, long id) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete notification with connection: " + id, e);
        }
    }

    private Notification mapRow(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getLong("id"));
        n.setUserId(rs.getLong("user_id"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setType(Notification.Type.fromString(rs.getString("type")));
        n.setRead(rs.getBoolean("is_read"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            n.setCreatedAt(ts.toLocalDateTime());
        }
        return n;
    }
}
