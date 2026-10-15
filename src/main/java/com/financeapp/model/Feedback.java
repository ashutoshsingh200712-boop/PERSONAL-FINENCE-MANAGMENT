package com.financeapp.model;

import java.time.LocalDateTime;

/**
 * Model representing user feedback and support tickets.
 */
public class Feedback {

    public enum Status {
        OPEN,
        CLOSED;

        public static Status fromString(String val) {
            if (val == null) return OPEN;
            try {
                return Status.valueOf(val.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return OPEN;
            }
        }
    }

    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private String subject;
    private String body;
    private Status status = Status.OPEN;
    private String adminReply;
    private LocalDateTime createdAt;

    public Feedback() {
    }

    public Feedback(Long id, Long userId, String subject, String body, Status status,
                    String adminReply, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.subject = subject;
        this.body = body;
        this.status = (status != null) ? status : Status.OPEN;
        this.adminReply = adminReply;
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getAdminReply() {
        return adminReply;
    }

    public void setAdminReply(String adminReply) {
        this.adminReply = adminReply;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
