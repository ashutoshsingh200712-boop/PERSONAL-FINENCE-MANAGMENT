package com.financeapp.dao;

import com.financeapp.model.Goal;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for user financial goals.
 */
public interface GoalDAO {

    long create(Goal goal);
    long create(Connection conn, Goal goal);

    Optional<Goal> findById(long id);
    Optional<Goal> findById(Connection conn, long id);

    List<Goal> findByUserId(long userId);
    List<Goal> findByUserId(Connection conn, long userId);

    boolean update(Goal goal);
    boolean update(Connection conn, Goal goal);

    boolean delete(long id);
    boolean delete(Connection conn, long id);
}
