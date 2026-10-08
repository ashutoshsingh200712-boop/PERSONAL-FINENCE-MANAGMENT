package com.financeapp.model;

import com.financeapp.exception.InvalidAmountException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Abstract financial transaction base class.
 */
public abstract class Transaction {

    private Long id;
    private Long userId;
    private BigDecimal amount;
    private String description;
    private LocalDate date;
    private LocalDateTime createdAt;

    protected Transaction() {
    }

    protected Transaction(Long id, Long userId, BigDecimal amount, String description,
                          LocalDate date, LocalDateTime createdAt) {
        setId(id);
        setUserId(userId);
        setAmount(amount);
        setDescription(description);
        setDate(date);
        setCreatedAt(createdAt);
    }

    /**
     * Returns the signed monetary value of this transaction.
     * Expense is negative, Income is positive.
     *
     * @return signed BigDecimal
     */
    public abstract BigDecimal signedAmount();

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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidAmountException("Transaction amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Transaction amount must be strictly greater than zero: " + amount);
        }
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null ? description.trim() : "";
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = Objects.requireNonNullElseGet(date, LocalDate::now);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
