package com.financeapp.dao;

import com.financeapp.model.Message;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for direct advisor-client messages.
 */
public interface MessageDAO {

    long create(Message message);
    long create(Connection conn, Message message);

    Optional<Message> findById(long id);
    Optional<Message> findById(Connection conn, long id);

    List<Message> findConversation(long user1Id, long user2Id);
    List<Message> findConversation(Connection conn, long user1Id, long user2Id);

    List<Message> findByReceiverId(long receiverId);
    List<Message> findByReceiverId(Connection conn, long receiverId);

    List<Message> findUnreadByReceiverId(long receiverId);
    List<Message> findUnreadByReceiverId(Connection conn, long receiverId);

    boolean markAsRead(long id);
    boolean markAsRead(Connection conn, long id);

    boolean markConversationAsRead(long receiverId, long senderId);
    boolean markConversationAsRead(Connection conn, long receiverId, long senderId);

    boolean delete(long id);
    boolean delete(Connection conn, long id);
}
