package com.financeapp.dao;

import com.financeapp.model.Notification;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for user alerts and notifications.
 */
public interface NotificationDAO {

    long create(Notification notification);
    long create(Connection conn, Notification notification);

    Optional<Notification> findById(long id);
    List<Notification> findByUserId(long userId);
    List<Notification> findUnreadByUserId(long userId);

    boolean markAsRead(long id);
    boolean markAsRead(Connection conn, long id);

    boolean delete(long id);
    boolean delete(Connection conn, long id);
}
