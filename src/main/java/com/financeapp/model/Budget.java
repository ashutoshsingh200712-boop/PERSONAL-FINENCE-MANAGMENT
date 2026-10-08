package com.financeapp.model;

import com.financeapp.exception.InvalidAmountException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * User-defined monthly category expenditure budget with alert threshold.
 */
public class Budget {

    private Long id;
    private Long userId;
    private Long categoryId;
    private String categoryName;
    private String budgetMonth; // Format: "YYYY-MM"
    private BigDecimal budgetLimit;
    private BigDecimal alertThreshold = new BigDecimal("80.00"); // default 80%
    private LocalDateTime createdAt;

    public Budget() {
    }

    public Budget(Long id, Long userId, Long categoryId, String budgetMonth,
                  BigDecimal budgetLimit, BigDecimal alertThreshold, LocalDateTime createdAt) {
        setId(id);
        setUserId(userId);
        setCategoryId(categoryId);
        setBudgetMonth(budgetMonth);
        setBudgetLimit(budgetLimit);
        setAlertThreshold(alertThreshold);
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

    public String getBudgetMonth() {
        return budgetMonth;
    }

    public void setBudgetMonth(String budgetMonth) {
        if (budgetMonth == null || !budgetMonth.matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
            throw new IllegalArgumentException("Budget month must be in YYYY-MM format, got: " + budgetMonth);
        }
        this.budgetMonth = budgetMonth;
    }

    public BigDecimal getBudgetLimit() {
        return budgetLimit;
    }

    public void setBudgetLimit(BigDecimal budgetLimit) {
        if (budgetLimit == null) {
            throw new InvalidAmountException("Budget limit cannot be null");
        }
        if (budgetLimit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Budget limit must be strictly positive: " + budgetLimit);
        }
        this.budgetLimit = budgetLimit.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getAlertThreshold() {
        return alertThreshold;
    }

    public void setAlertThreshold(BigDecimal alertThreshold) {
        if (alertThreshold == null || alertThreshold.compareTo(BigDecimal.ZERO) <= 0 || alertThreshold.compareTo(new BigDecimal("100.00")) > 0) {
            throw new IllegalArgumentException("Alert threshold percentage must be between 1 and 100: " + alertThreshold);
        }
        this.alertThreshold = alertThreshold.setScale(2, RoundingMode.HALF_UP);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Budget{" +
                "id=" + id +
                ", userId=" + userId +
                ", categoryId=" + categoryId +
                ", budgetMonth='" + budgetMonth + '\'' +
                ", budgetLimit=" + budgetLimit +
                ", alertThreshold=" + alertThreshold +
                '}';
    }
}
