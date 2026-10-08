package com.financeapp.model;

import com.financeapp.exception.InvalidAmountException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * An individual envelope / allocation item within an AI Smart Spending Plan.
 */
public class PlanItem {

    private Long id;
    private Long planId;
    private Long categoryId;
    private String categoryName;
    private BigDecimal allocatedAmount = BigDecimal.ZERO;
    private BigDecimal weightPercentage = BigDecimal.ZERO;
    private String notes;
    private LocalDateTime createdAt;

    public PlanItem() {
    }

    public PlanItem(Long id, Long planId, Long categoryId, BigDecimal allocatedAmount,
                    BigDecimal weightPercentage, String notes, LocalDateTime createdAt) {
        setId(id);
        setPlanId(planId);
        setCategoryId(categoryId);
        setAllocatedAmount(allocatedAmount);
        setWeightPercentage(weightPercentage);
        setNotes(notes);
        setCreatedAt(createdAt);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalArgumentException("Category ID cannot be null or non-positive");
        }
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public BigDecimal getAllocatedAmount() {
        return allocatedAmount;
    }

    public void setAllocatedAmount(BigDecimal allocatedAmount) {
        if (allocatedAmount == null || allocatedAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidAmountException("Allocated amount cannot be negative or null");
        }
        this.allocatedAmount = allocatedAmount.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getWeightPercentage() {
        return weightPercentage;
    }

    public void setWeightPercentage(BigDecimal weightPercentage) {
        if (weightPercentage == null || weightPercentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Weight percentage cannot be negative or null");
        }
        this.weightPercentage = weightPercentage.setScale(2, RoundingMode.HALF_UP);
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
        return "PlanItem{" +
                "id=" + id +
                ", planId=" + planId +
                ", categoryId=" + categoryId +
                ", allocatedAmount=" + allocatedAmount +
                ", weightPercentage=" + weightPercentage +
                '}';
    }
}
