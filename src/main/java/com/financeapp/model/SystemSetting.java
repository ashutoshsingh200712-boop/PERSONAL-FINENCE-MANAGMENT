package com.financeapp.model;

import java.time.LocalDateTime;

/**
 * Platform configuration parameter or heuristic setting.
 */
public class SystemSetting {

    private Long id;
    private String key;
    private String value;
    private String description;
    private LocalDateTime updatedAt;

    public SystemSetting() {
    }

    public SystemSetting(Long id, String key, String value, String description, LocalDateTime updatedAt) {
        setId(id);
        setKey(key);
        setValue(value);
        setDescription(description);
        setUpdatedAt(updatedAt);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Setting key cannot be empty");
        }
        this.key = key.trim();
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value != null ? value : "";
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "SystemSetting{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
