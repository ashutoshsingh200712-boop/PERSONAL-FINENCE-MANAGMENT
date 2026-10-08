package com.financeapp.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Concrete transaction representing money outflows (expenses).
 */
public class Expense extends Transaction {

    private Long categoryId;
    private String categoryName;
    private String paymentMethod = "CASH";

    public Expense() {
    }

    public Expense(Long id, Long userId, Long categoryId, BigDecimal amount,
                   LocalDate expenseDate, String description, String paymentMethod,
                   LocalDateTime createdAt) {
        super(id, userId, amount, description, expenseDate, createdAt);
        setCategoryId(categoryId);
        setPaymentMethod(paymentMethod);
    }

    @Override
    public BigDecimal signedAmount() {
        return getAmount() != null ? getAmount().negate() : BigDecimal.ZERO;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalArgumentException("Expense category ID must be a positive integer");
        }
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = (paymentMethod != null && !paymentMethod.trim().isEmpty())
                ? paymentMethod.trim().toUpperCase() : "CASH";
    }

    @Override
    public String toString() {
        return "Expense{" +
                "id=" + getId() +
                ", userId=" + getUserId() +
                ", categoryId=" + categoryId +
                ", amount=" + getAmount() +
                ", date=" + getDate() +
                ", description='" + getDescription() + '\'' +
                ", paymentMethod='" + paymentMethod + '\'' +
                '}';
    }
}
