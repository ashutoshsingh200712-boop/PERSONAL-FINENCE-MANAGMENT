package com.financeapp.dao;

import com.financeapp.model.Budget;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for category budget limits and thresholds.
 */
public interface BudgetDAO {

    long create(Budget budget);
    long create(Connection conn, Budget budget);

    Optional<Budget> findById(long id);
    Optional<Budget> findByUserCategoryAndMonth(long userId, long categoryId, String month);
    List<Budget> findByUserAndMonth(long userId, String month);

    boolean update(Budget budget);
    boolean update(Connection conn, Budget budget);

    boolean delete(long id);
    boolean delete(Connection conn, long id);
}
