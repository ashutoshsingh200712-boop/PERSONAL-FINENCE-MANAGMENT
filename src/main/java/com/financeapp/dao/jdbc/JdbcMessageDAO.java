package com.financeapp.dao.jdbc;

import com.financeapp.dao.MessageDAO;
import com.financeapp.exception.DataAccessException;
import com.financeapp.model.Message;
import com.financeapp.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of MessageDAO using PreparedStatements and try-with-resources.
 */
public class JdbcMessageDAO implements MessageDAO {

    private static final String SQL_INSERT =
            "INSERT INTO messages (sender_id, receiver_id, body, is_read, created_at) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT m.id, m.sender_id, m.receiver_id, m.body, m.is_read, m.created_at, " +
            "s.name AS sender_name, r.name AS receiver_name " +
            "FROM messages m " +
            "LEFT JOIN users s ON m.sender_id = s.id " +
            "LEFT JOIN users r ON m.receiver_id = r.id " +
            "WHERE m.id = ?";

    private static final String SQL_FIND_CONVERSATION =
            "SELECT m.id, m.sender_id, m.receiver_id, m.body, m.is_read, m.created_at, " +
            "s.name AS sender_name, r.name AS receiver_name " +
            "FROM messages m " +
            "LEFT JOIN users s ON m.sender_id = s.id " +
            "LEFT JOIN users r ON m.receiver_id = r.id " +
            "WHERE (m.sender_id = ? AND m.receiver_id = ?) OR (m.sender_id = ? AND m.receiver_id = ?) " +
            "ORDER BY m.created_at ASC";

    private static final String SQL_FIND_BY_RECEIVER =
            "SELECT m.id, m.sender_id, m.receiver_id, m.body, m.is_read, m.created_at, " +
            "s.name AS sender_name, r.name AS receiver_name " +
            "FROM messages m " +
            "LEFT JOIN users s ON m.sender_id = s.id " +
            "LEFT JOIN users r ON m.receiver_id = r.id " +
            "WHERE m.receiver_id = ? ORDER BY m.created_at DESC";

    private static final String SQL_FIND_UNREAD_BY_RECEIVER =
            "SELECT m.id, m.sender_id, m.receiver_id, m.body, m.is_read, m.created_at, " +
            "s.name AS sender_name, r.name AS receiver_name " +
            "FROM messages m " +
            "LEFT JOIN users s ON m.sender_id = s.id " +
            "LEFT JOIN users r ON m.receiver_id = r.id " +
            "WHERE m.receiver_id = ? AND m.is_read = FALSE ORDER BY m.created_at DESC";

    private static final String SQL_MARK_READ =
            "UPDATE messages SET is_read = TRUE WHERE id = ?";

    private static final String SQL_MARK_CONVERSATION_READ =
            "UPDATE messages SET is_read = TRUE WHERE receiver_id = ? AND sender_id = ? AND is_read = FALSE";

    private static final String SQL_DELETE =
            "DELETE FROM messages WHERE id = ?";

    @Override
    public long create(Message message) {
        try (Connection conn = DBConnection.getConnection()) {
            return create(conn, message);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create message: " + e.getMessage(), e);
        }
    }

    @Override
    public long create(Connection conn, Message message) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, message.getSenderId());
            stmt.setLong(2, message.getReceiverId());
            stmt.setString(3, message.getBody());
            stmt.setBoolean(4, message.isRead());
            LocalDateTime createdAt = message.getCreatedAt() != null ? message.getCreatedAt() : LocalDateTime.now();
            stmt.setTimestamp(5, Timestamp.valueOf(createdAt));

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new DataAccessException("Creating message failed, no rows affected.");
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    message.setId(id);
                    message.setCreatedAt(createdAt);
                    return id;
                } else {
                    throw new DataAccessException("Creating message failed, no generated key obtained.");
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert message: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Message> findById(long id) {
        try (Connection conn = DBConnection.getConnection()) {
            return findById(conn, id);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find message by id: " + id, e);
        }
    }

    @Override
    public Optional<Message> findById(Connection conn, long id) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ID)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query message by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Message> findConversation(long user1Id, long user2Id) {
        try (Connection conn = DBConnection.getConnection()) {
            return findConversation(conn, user1Id, user2Id);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load conversation: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Message> findConversation(Connection conn, long user1Id, long user2Id) {
        List<Message> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_CONVERSATION)) {
            stmt.setLong(1, user1Id);
            stmt.setLong(2, user2Id);
            stmt.setLong(3, user2Id);
            stmt.setLong(4, user1Id);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query conversation: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Message> findByReceiverId(long receiverId) {
        try (Connection conn = DBConnection.getConnection()) {
            return findByReceiverId(conn, receiverId);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find messages for receiver: " + receiverId, e);
        }
    }

    @Override
    public List<Message> findByReceiverId(Connection conn, long receiverId) {
        List<Message> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_RECEIVER)) {
            stmt.setLong(1, receiverId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query messages for receiver: " + receiverId, e);
        }
        return list;
    }

    @Override
    public List<Message> findUnreadByReceiverId(long receiverId) {
        try (Connection conn = DBConnection.getConnection()) {
            return findUnreadByReceiverId(conn, receiverId);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find unread messages for receiver: " + receiverId, e);
        }
    }

    @Override
    public List<Message> findUnreadByReceiverId(Connection conn, long receiverId) {
        List<Message> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_UNREAD_BY_RECEIVER)) {
            stmt.setLong(1, receiverId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query unread messages for receiver: " + receiverId, e);
        }
        return list;
    }

    @Override
    public boolean markAsRead(long id) {
        try (Connection conn = DBConnection.getConnection()) {
            return markAsRead(conn, id);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to mark message read: " + id, e);
        }
    }

    @Override
    public boolean markAsRead(Connection conn, long id) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_MARK_READ)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to execute markAsRead on message: " + id, e);
        }
    }

    @Override
    public boolean markConversationAsRead(long receiverId, long senderId) {
        try (Connection conn = DBConnection.getConnection()) {
            return markConversationAsRead(conn, receiverId, senderId);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to mark conversation read: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean markConversationAsRead(Connection conn, long receiverId, long senderId) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_MARK_CONVERSATION_READ)) {
            stmt.setLong(1, receiverId);
            stmt.setLong(2, senderId);
            return stmt.executeUpdate() >= 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to execute markConversationAsRead: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(long id) {
        try (Connection conn = DBConnection.getConnection()) {
            return delete(conn, id);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete message: " + id, e);
        }
    }

    @Override
    public boolean delete(Connection conn, long id) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to execute delete on message: " + id, e);
        }
    }

    private Message mapRow(ResultSet rs) throws SQLException {
        Message m = new Message();
        m.setId(rs.getLong("id"));
        m.setSenderId(rs.getLong("sender_id"));
        m.setReceiverId(rs.getLong("receiver_id"));
        m.setBody(rs.getString("body"));
        m.setRead(rs.getBoolean("is_read"));
        m.setSenderName(rs.getString("sender_name"));
        m.setReceiverName(rs.getString("receiver_name"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            m.setCreatedAt(ts.toLocalDateTime());
        }
        return m;
    }
}
