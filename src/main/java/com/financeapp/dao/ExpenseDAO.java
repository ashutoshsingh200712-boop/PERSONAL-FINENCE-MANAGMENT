package com.financeapp.dao;

import com.financeapp.model.CategorySpendingSummary;
import com.financeapp.model.Expense;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for daily and discretionary expense management.
 */
public interface ExpenseDAO {

    long create(Expense expense);
    long create(Connection conn, Expense expense);

    Optional<Expense> findById(long id);
    List<Expense> findByUserId(long userId, int limit, int offset);

    /**
     * Filters expenses by billing month (YYYY-MM) and optional category with pagination.
     */
    List<Expense> findByMonthAndCategory(long userId, String month, Long categoryId, int limit, int offset);
    List<Expense> findByMonthAndCategory(Connection conn, long userId, String month, Long categoryId, int limit, int offset);

    /**
     * Returns total count of filtered expenses for pagination calculation.
     */
    int countByMonthAndCategory(long userId, String month, Long categoryId);

    /**
     * Aggregates total spent per category in a given billing month (YYYY-MM).
     */
    List<CategorySpendingSummary> getCategorySpendingInMonth(long userId, String month);

    boolean update(Expense expense);
    boolean update(Connection conn, Expense expense);

    boolean delete(long id);
    boolean delete(Connection conn, long id);
}
