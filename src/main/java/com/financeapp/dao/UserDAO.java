package com.financeapp.dao;

import com.financeapp.model.User;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for user management and authentication.
 */
public interface UserDAO {

    long create(User user);
    long create(Connection conn, User user);

    Optional<User> findById(long id);
    Optional<User> findByEmail(String email);
    List<User> findAll();
    List<User> findByAdvisorId(long advisorId);

    boolean update(User user);
    boolean update(Connection conn, User user);

    boolean delete(long id);
    boolean delete(Connection conn, long id);

    boolean updateFailedLogins(long userId, int count, LocalDateTime lockedUntil);
    boolean updateFailedLogins(Connection conn, long userId, int count, LocalDateTime lockedUntil);

    boolean resetFailedLogins(long userId);
    boolean resetFailedLogins(Connection conn, long userId);
}
