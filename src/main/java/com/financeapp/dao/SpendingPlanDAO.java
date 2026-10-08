package com.financeapp.dao;

import com.financeapp.model.SpendingPlan;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for monthly AI Spending Plans and their associated plan items.
 */
public interface SpendingPlanDAO {

    long create(SpendingPlan plan);
    long create(Connection conn, SpendingPlan plan);

    Optional<SpendingPlan> findById(long id);
    Optional<SpendingPlan> findByUserAndMonth(long userId, String month);
    List<SpendingPlan> findByUserId(long userId);

    boolean update(SpendingPlan plan);
    boolean update(Connection conn, SpendingPlan plan);

    boolean delete(long id);
    boolean delete(Connection conn, long id);
}
