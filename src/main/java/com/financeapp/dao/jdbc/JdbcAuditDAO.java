package com.financeapp.dao.jdbc;

import com.financeapp.dao.AuditDAO;
import com.financeapp.exception.DataAccessException;
import com.financeapp.model.AuditLog;
import com.financeapp.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of AuditDAO.
 */
public class JdbcAuditDAO implements AuditDAO {

    private static final String SQL_INSERT =
            "INSERT INTO audit_logs (user_id, action, entity_type, entity_id, details, ip_address, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_USER =
            "SELECT id, user_id, action, entity_type, entity_id, details, ip_address, created_at FROM audit_logs " +
            "WHERE user_id = ? ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?";

    private static final String SQL_FIND_RECENT =
            "SELECT id, user_id, action, entity_type, entity_id, details, ip_address, created_at FROM audit_logs " +
            "ORDER BY created_at DESC, id DESC LIMIT ?";

    @Override
    public long log(AuditLog auditLog) {
        try (Connection conn = DBConnection.getConnection()) {
            return log(conn, auditLog);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to record audit log: " + e.getMessage(), e);
        }
    }

    @Override
    public long log(Connection conn, AuditLog auditLog) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            if (auditLog.getUserId() != null) {
                stmt.setLong(1, auditLog.getUserId());
            } else {
                stmt.setNull(1, Types.BIGINT);
            }
            stmt.setString(2, auditLog.getAction());
            stmt.setString(3, auditLog.getEntityType());
            if (auditLog.getEntityId() != null) {
                stmt.setLong(4, auditLog.getEntityId());
            } else {
                stmt.setNull(4, Types.BIGINT);
            }
            stmt.setString(5, auditLog.getDetails());
            stmt.setString(6, auditLog.getIpAddress());
            LocalDateTime createdAt = auditLog.getCreatedAt() != null ? auditLog.getCreatedAt() : LocalDateTime.now();
            stmt.setTimestamp(7, Timestamp.valueOf(createdAt));

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new DataAccessException("Creating audit log failed, no rows affected.");
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    auditLog.setId(id);
                    auditLog.setCreatedAt(createdAt);
                    return id;
                } else {
                    throw new DataAccessException("Creating audit log failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert audit log with connection: " + e.getMessage(), e);
        }
    }

    @Override
    public List<AuditLog> findByUserId(long userId, int limit, int offset) {
        List<AuditLog> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_USER)) {
            stmt.setLong(1, userId);
            stmt.setInt(2, Math.max(limit, 1));
            stmt.setInt(3, Math.max(offset, 0));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find audit logs for user: " + userId, e);
        }
        return list;
    }

    @Override
    public List<AuditLog> findRecent(int limit) {
        List<AuditLog> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_RECENT)) {
            stmt.setInt(1, Math.max(limit, 1));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find recent audit logs", e);
        }
        return list;
    }

    private AuditLog mapRow(ResultSet rs) throws SQLException {
        AuditLog a = new AuditLog();
        a.setId(rs.getLong("id"));
        long uid = rs.getLong("user_id");
        if (!rs.wasNull()) {
            a.setUserId(uid);
        }
        a.setAction(rs.getString("action"));
        a.setEntityType(rs.getString("entity_type"));
        long eid = rs.getLong("entity_id");
        if (!rs.wasNull()) {
            a.setEntityId(eid);
        }
        a.setDetails(rs.getString("details"));
        a.setIpAddress(rs.getString("ip_address"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            a.setCreatedAt(ts.toLocalDateTime());
        }
        return a;
    }
}
