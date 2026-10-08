package com.financeapp.model;

import java.time.LocalDateTime;

/**
 * System and advisor notification for users.
 */
public class Notification {

    public enum Type {
        INFO,
        WARNING,
        ALERT,
        ADVICE;

        public static Type fromString(String str) {
            if (str == null || str.trim().isEmpty()) {
                return INFO;
            }
            try {
                return Type.valueOf(str.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return INFO;
            }
        }
    }

    private Long id;
    private Long userId;
    private String title;
    private String message;
    private Type type = Type.INFO;
    private boolean read = false;
    private LocalDateTime createdAt;

    public Notification() {
    }

    public Notification(Long id, Long userId, String title, String message,
                        Type type, boolean read, LocalDateTime createdAt) {
        setId(id);
        setUserId(userId);
        setTitle(title);
        setMessage(message);
        setType(type);
        setRead(read);
        setCreatedAt(createdAt);
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
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be valid");
        }
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Notification title cannot be empty");
        }
        this.title = title.trim();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Notification message cannot be empty");
        }
        this.message = message.trim();
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = (type != null) ? type : Type.INFO;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", userId=" + userId +
                ", title='" + title + '\'' +
                ", type=" + type +
                ", read=" + read +
                '}';
    }
}
