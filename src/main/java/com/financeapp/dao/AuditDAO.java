package com.financeapp.dao;

import com.financeapp.model.AuditLog;

import java.sql.Connection;
import java.util.List;

/**
 * Data Access Object interface for platform security and event auditing.
 */
public interface AuditDAO {

    long log(AuditLog auditLog);
    long log(Connection conn, AuditLog auditLog);

    List<AuditLog> findByUserId(long userId, int limit, int offset);
    List<AuditLog> findRecent(int limit);
}
