package com.financeapp.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Monthly AI Smart Spending Plan for a user.
 */
public class SpendingPlan {

    public enum Status {
        DRAFT,
        ACTIVE,
        ARCHIVED;

        public static Status fromString(String str) {
            if (str == null || str.trim().isEmpty()) {
                return ACTIVE;
            }
            try {
                return Status.valueOf(str.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return ACTIVE;
            }
        }
    }

    private Long id;
    private Long userId;
    private String planMonth; // Format: "YYYY-MM"
    private BigDecimal totalIncome = BigDecimal.ZERO;
    private BigDecimal totalFixedExpenses = BigDecimal.ZERO;
    private BigDecimal disposableIncome = BigDecimal.ZERO;
    private Status status = Status.ACTIVE;
    private List<PlanItem> items = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SpendingPlan() {
    }

    public SpendingPlan(Long id, Long userId, String planMonth, BigDecimal totalIncome,
                        BigDecimal totalFixedExpenses, BigDecimal disposableIncome,
                        Status status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        setId(id);
        setUserId(userId);
        setPlanMonth(planMonth);
        setTotalIncome(totalIncome);
        setTotalFixedExpenses(totalFixedExpenses);
        setDisposableIncome(disposableIncome);
        setStatus(status);
        setCreatedAt(createdAt);
        setUpdatedAt(updatedAt);
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

    public String getPlanMonth() {
        return planMonth;
    }

    public void setPlanMonth(String planMonth) {
        if (planMonth == null || !planMonth.matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
            throw new IllegalArgumentException("Plan month must be in YYYY-MM format, got: " + planMonth);
        }
        this.planMonth = planMonth;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = (totalIncome != null) ? totalIncome.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    public BigDecimal getTotalFixedExpenses() {
        return totalFixedExpenses;
    }

    public void setTotalFixedExpenses(BigDecimal totalFixedExpenses) {
        this.totalFixedExpenses = (totalFixedExpenses != null) ? totalFixedExpenses.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    public BigDecimal getDisposableIncome() {
        return disposableIncome;
    }

    public void setDisposableIncome(BigDecimal disposableIncome) {
        this.disposableIncome = (disposableIncome != null) ? disposableIncome.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = (status != null) ? status : Status.ACTIVE;
    }

    public List<PlanItem> getItems() {
        return items;
    }

    public void setItems(List<PlanItem> items) {
        this.items = (items != null) ? items : new ArrayList<>();
    }

    public void addItem(PlanItem item) {
        if (item != null) {
            this.items.add(item);
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "SpendingPlan{" +
                "id=" + id +
                ", userId=" + userId +
                ", planMonth='" + planMonth + '\'' +
                ", disposableIncome=" + disposableIncome +
                ", status=" + status +
                ", itemsCount=" + items.size() +
                '}';
    }
}
