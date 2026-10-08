package com.financeapp.planner;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Strategy prioritizing user savings targets.
 * Computes savings share clamped between 20% and 40% of available income,
 * scaling other categories by (1 - share) / 0.80.
 * Exposes a warning message if the requested goal is clamped.
 */
public class SavingsFocusedStrategy implements AllocationStrategy {

    private static final BigDecimal MIN_SAVINGS_SHARE = new BigDecimal("0.20");
    private static final BigDecimal MAX_SAVINGS_SHARE = new BigDecimal("0.40");
    private static final BigDecimal BASE_NON_SAVINGS_TOTAL = new BigDecimal("0.80");

    private boolean goalClamped = false;
    private String clampWarning = null;

    @Override
    public Map<String, BigDecimal> allocate(BigDecimal available, BigDecimal savingsGoal) {
        if (available == null || available.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Available income cannot be null or negative");
        }

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        BigDecimal scaledAvailable = available.setScale(2, RoundingMode.HALF_UP);

        if (scaledAvailable.compareTo(BigDecimal.ZERO) == 0) {
            this.goalClamped = false;
            this.clampWarning = null;
            result.put("Savings", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            result.put("Food", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            result.put("Emergency", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            result.put("Entertainment", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            result.put("Other", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            return result;
        }

        // Determine savings share: clamp(goal / available, 20%, 40%)
        BigDecimal rawShare;
        if (savingsGoal == null || savingsGoal.compareTo(BigDecimal.ZERO) <= 0) {
            rawShare = MIN_SAVINGS_SHARE;
            this.goalClamped = true;
            this.clampWarning = "Savings goal not specified; clamped to default minimum 20.00%.";
        } else {
            rawShare = savingsGoal.divide(scaledAvailable, 6, RoundingMode.HALF_UP);
        }

        BigDecimal share;
        if (rawShare.compareTo(MIN_SAVINGS_SHARE) < 0) {
            share = MIN_SAVINGS_SHARE;
            this.goalClamped = true;
            this.clampWarning = String.format(
                    "Requested savings goal of $%s (%.2f%%) is below minimum safe threshold; clamped to 20.00%%.",
                    savingsGoal, rawShare.multiply(BigDecimal.valueOf(100))
            );
        } else if (rawShare.compareTo(MAX_SAVINGS_SHARE) > 0) {
            share = MAX_SAVINGS_SHARE;
            this.goalClamped = true;
            this.clampWarning = String.format(
                    "Requested savings goal of $%s (%.2f%%) exceeds maximum safe threshold; clamped to 40.00%%.",
                    savingsGoal, rawShare.multiply(BigDecimal.valueOf(100))
            );
        } else {
            share = rawShare;
            this.goalClamped = false;
            this.clampWarning = null;
        }

        // Calculate scaling factor for other categories: (1 - share) / 0.80
        BigDecimal scale = BigDecimal.ONE.subtract(share).divide(BASE_NON_SAVINGS_TOTAL, 6, RoundingMode.HALF_UP);

        // 1. Savings
        BigDecimal savingsAmount = scaledAvailable.multiply(share).setScale(2, RoundingMode.HALF_UP);
        result.put("Savings", savingsAmount);

        // 2. Food (Base: 40%)
        BigDecimal foodAmount = scaledAvailable.multiply(new BigDecimal("0.40")).multiply(scale).setScale(2, RoundingMode.HALF_UP);
        result.put("Food", foodAmount);

        // 3. Emergency (Base: 15%)
        BigDecimal emergencyAmount = scaledAvailable.multiply(new BigDecimal("0.15")).multiply(scale).setScale(2, RoundingMode.HALF_UP);
        result.put("Emergency", emergencyAmount);

        // 4. Entertainment (Base: 10%)
        BigDecimal entertainmentAmount = scaledAvailable.multiply(new BigDecimal("0.10")).multiply(scale).setScale(2, RoundingMode.HALF_UP);
        result.put("Entertainment", entertainmentAmount);

        // 5. Other (Base: 15% - absorbs rounding so sum is strictly equal to available)
        BigDecimal allocatedSoFar = savingsAmount.add(foodAmount).add(emergencyAmount).add(entertainmentAmount);
        BigDecimal otherAmount = scaledAvailable.subtract(allocatedSoFar).setScale(2, RoundingMode.HALF_UP);
        result.put("Other", otherAmount);

        return result;
    }

    public boolean isGoalClamped() {
        return goalClamped;
    }

    public Optional<String> getClampWarning() {
        return Optional.ofNullable(clampWarning);
    }
}
