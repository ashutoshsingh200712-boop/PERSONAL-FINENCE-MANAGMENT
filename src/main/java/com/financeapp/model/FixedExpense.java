package com.financeapp.model;

import com.financeapp.exception.InvalidAmountException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Inflexible recurring living obligations (e.g. rent, bills, debt payments).
 */
public class FixedExpense {

    private Long id;
    private Long userId;
    private Long categoryId;
    private String categoryName;
    private String title;
    private BigDecimal amount;
    private int dueDay = 1;
    private boolean active = true;
    private String notes;
    private LocalDateTime createdAt;

    public FixedExpense() {
    }

    public FixedExpense(Long id, Long userId, Long categoryId, String title,
                        BigDecimal amount, int dueDay, boolean active, String notes,
                        LocalDateTime createdAt) {
        setId(id);
        setUserId(userId);
        setCategoryId(categoryId);
        setTitle(title);
        setAmount(amount);
        setDueDay(dueDay);
        setActive(active);
        setNotes(notes);
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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalArgumentException("Category ID must be valid");
        }
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Fixed expense title cannot be empty");
        }
        this.title = title.trim();
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidAmountException("Fixed expense amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Fixed expense amount must be greater than zero: " + amount);
        }
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public int getDueDay() {
        return dueDay;
    }

    public void setDueDay(int dueDay) {
        if (dueDay < 1 || dueDay > 31) {
            throw new IllegalArgumentException("Due day must be between 1 and 31");
        }
        this.dueDay = dueDay;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes != null ? notes.trim() : "";
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "FixedExpense{" +
                "id=" + id +
                ", userId=" + userId +
                ", title='" + title + '\'' +
                ", amount=" + amount +
                ", dueDay=" + dueDay +
                ", active=" + active +
                '}';
    }
}
