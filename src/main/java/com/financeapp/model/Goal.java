package com.financeapp.model;

import com.financeapp.exception.InvalidAmountException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Model representing a user financial savings goal.
 */
public class Goal {

    private Long id;
    private Long userId;
    private String name;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount = BigDecimal.ZERO;
    private LocalDate targetDate;
    private LocalDateTime createdAt;

    public Goal() {
    }

    public Goal(Long id, Long userId, String name, BigDecimal targetAmount,
                BigDecimal currentAmount, LocalDate targetDate, LocalDateTime createdAt) {
        this.id = id;
        setUserId(userId);
        setName(name);
        setTargetAmount(targetAmount);
        setCurrentAmount(currentAmount);
        setTargetDate(targetDate);
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
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be valid");
        }
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Goal name cannot be blank");
        }
        this.name = name.trim();
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Target amount must be strictly positive");
        }
        this.targetAmount = targetAmount.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(BigDecimal currentAmount) {
        if (currentAmount == null) {
            currentAmount = BigDecimal.ZERO;
        }
        if (currentAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidAmountException("Current amount cannot be negative");
        }
        this.currentAmount = currentAmount.setScale(2, RoundingMode.HALF_UP);
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        if (targetDate == null) {
            throw new IllegalArgumentException("Target date cannot be null");
        }
        this.targetDate = targetDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public BigDecimal getRemainingAmount() {
        if (targetAmount == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal current = (currentAmount != null) ? currentAmount : BigDecimal.ZERO;
        BigDecimal diff = targetAmount.subtract(current);
        return diff.compareTo(BigDecimal.ZERO) > 0 ? diff : BigDecimal.ZERO;
    }

    public BigDecimal getProgressPercentage() {
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal current = (currentAmount != null) ? currentAmount : BigDecimal.ZERO;
        return current.multiply(BigDecimal.valueOf(100))
                .divide(targetAmount, 1, RoundingMode.HALF_UP);
    }
}
