package com.financeapp.dao;

import com.financeapp.model.FixedExpense;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for fixed recurring commitments (rent, utilities, loans).
 */
public interface FixedExpenseDAO {

    long create(FixedExpense fixedExpense);
    long create(Connection conn, FixedExpense fixedExpense);

    Optional<FixedExpense> findById(long id);
    List<FixedExpense> findByUserId(long userId);
    List<FixedExpense> findActiveByUserId(long userId);
    List<FixedExpense> findActiveByDueDay(int dueDay);
    List<FixedExpense> findActiveByDueDay(Connection conn, int dueDay);
    List<FixedExpense> findAllActive();
    List<FixedExpense> findAllActive(Connection conn);

    boolean update(FixedExpense fixedExpense);
    boolean update(Connection conn, FixedExpense fixedExpense);

    boolean delete(long id);
    boolean delete(Connection conn, long id);
}
