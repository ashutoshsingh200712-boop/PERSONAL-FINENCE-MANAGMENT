package com.financeapp.planner;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Advice rule triggered when the remaining budget is critically low (less than or equal to 10% of budget or exhausted)
 * while days remain in the billing period.
 */
public class LowBalanceRule implements AdviceRule {

    private static final BigDecimal LOW_THRESHOLD = new BigDecimal("0.10");

    @Override
    public Optional<String> evaluate(RecalculationResult result) {
        if (result == null || result.getDaysLeft() <= 0) {
            return Optional.empty();
        }

        BigDecimal remaining = result.getBudget().subtract(result.getSpent());
        BigDecimal lowBalanceThreshold = result.getBudget().multiply(LOW_THRESHOLD).setScale(2, RoundingMode.HALF_UP);

        if (remaining.compareTo(lowBalanceThreshold) <= 0) {
            String text = String.format(
                    "[LowBalanceRule] Alert: Remaining budget for %s is critically low at $%s with %d days left. " +
                    "Exercise strict expenditure control to avoid overdrawing.",
                    result.getCategoryName(),
                    remaining.setScale(2, RoundingMode.HALF_UP),
                    result.getDaysLeft()
            );
            return Optional.of(text);
        }

        return Optional.empty();
    }
}
