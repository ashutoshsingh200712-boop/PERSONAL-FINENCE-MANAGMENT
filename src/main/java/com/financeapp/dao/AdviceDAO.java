package com.financeapp.dao;

import com.financeapp.model.Advice;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for financial advice and advisor recommendations.
 */
public interface AdviceDAO {

    long create(Advice advice);
    long create(Connection conn, Advice advice);

    Optional<Advice> findById(long id);
    Optional<Advice> findById(Connection conn, long id);

    List<Advice> findByUserId(long userId);
    List<Advice> findByUserId(Connection conn, long userId);

    List<Advice> findByPlanId(long planId);
    List<Advice> findByPlanId(Connection conn, long planId);

    boolean updateStatus(long id, Advice.Status status);
    boolean updateStatus(Connection conn, long id, Advice.Status status);

    boolean updateAdvisorComment(long id, String comment);
    boolean updateAdvisorComment(Connection conn, long id, String comment);

    boolean delete(long id);
    boolean delete(Connection conn, long id);
}
