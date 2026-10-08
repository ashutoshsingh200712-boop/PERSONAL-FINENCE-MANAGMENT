package com.financeapp.model;

import java.time.LocalDateTime;

/**
 * Audit log entry capturing critical security and administrative actions.
 */
public class AuditLog {

    private Long id;
    private Long userId;
    private String action;
    private String entityType;
    private Long entityId;
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;

    public AuditLog() {
    }

    public AuditLog(Long id, Long userId, String action, String entityType,
                    Long entityId, String details, String ipAddress, LocalDateTime createdAt) {
        setId(id);
        setUserId(userId);
        setAction(action);
        setEntityType(entityType);
        setEntityId(entityId);
        setDetails(details);
        setIpAddress(ipAddress);
        setCreatedAt(createdAt);
    }

    public AuditLog(Long userId, String action, String entityType,
                    Long entityId, String details, String ipAddress, LocalDateTime createdAt) {
        this(null, userId, action, entityType, entityId, details, ipAddress, createdAt);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        if (action == null || action.trim().isEmpty()) {
            throw new IllegalArgumentException("Audit action cannot be empty");
        }
        this.action = action.trim();
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        if (entityType == null || entityType.trim().isEmpty()) {
            throw new IllegalArgumentException("Audit entity type cannot be empty");
        }
        this.entityType = entityType.trim();
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + id +
                ", userId=" + userId +
                ", action='" + action + '\'' +
                ", entityType='" + entityType + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
