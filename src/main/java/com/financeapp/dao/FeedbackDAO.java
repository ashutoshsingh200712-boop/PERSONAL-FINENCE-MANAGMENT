package com.financeapp.dao;

import com.financeapp.model.Feedback;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for user feedback tickets and admin replies.
 */
public interface FeedbackDAO {

    long create(Feedback feedback);
    long create(Connection conn, Feedback feedback);

    Optional<Feedback> findById(long id);
    Optional<Feedback> findById(Connection conn, long id);

    List<Feedback> findAll();
    List<Feedback> findAll(Connection conn);

    List<Feedback> findByUserId(long userId);
    List<Feedback> findByUserId(Connection conn, long userId);

    boolean reply(long id, String adminReply);
    boolean reply(Connection conn, long id, String adminReply);

    boolean updateStatus(long id, Feedback.Status status);
    boolean updateStatus(Connection conn, long id, Feedback.Status status);

    boolean replyAndClose(long id, String adminReply, Feedback.Status status);
    boolean replyAndClose(Connection conn, long id, String adminReply, Feedback.Status status);

    boolean delete(long id);
    boolean delete(Connection conn, long id);
}
