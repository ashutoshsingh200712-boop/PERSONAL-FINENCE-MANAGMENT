package com.financeapp.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Data Transfer Object representing aggregated expense spending per category within a billing month.
 */
public class CategorySpendingSummary {

    private Long categoryId;
    private String categoryName;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private int transactionCount = 0;

    public CategorySpendingSummary() {
    }

    public CategorySpendingSummary(Long categoryId, String categoryName, BigDecimal totalAmount, int transactionCount) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        setTotalAmount(totalAmount);
        this.transactionCount = transactionCount;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount != null ? totalAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    @Override
    public String toString() {
        return "CategorySpendingSummary{" +
                "categoryId=" + categoryId +
                ", categoryName='" + categoryName + '\'' +
                ", totalAmount=" + totalAmount +
                ", transactionCount=" + transactionCount +
                '}';
    }
}
