package com.financeapp.dao;

import com.financeapp.model.Income;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for user income sources.
 */
public interface IncomeDAO {

    long create(Income income);
    long create(Connection conn, Income income);

    Optional<Income> findById(long id);
    List<Income> findByUserId(long userId);
    List<Income> findByUserAndMonth(long userId, String month);

    boolean update(Income income);
    boolean update(Connection conn, Income income);

    boolean delete(long id);
    boolean delete(Connection conn, long id);
}
