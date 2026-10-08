package com.financeapp.planner;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Advice rule triggered when category spending pace exceeds 110% of linear expected trajectory.
 */
public class OverspendingRule implements AdviceRule {

    @Override
    public Optional<String> evaluate(RecalculationResult result) {
        if (result == null || result.getStatus() != PlanStatus.OVER) {
            return Optional.empty();
        }

        BigDecimal percentage = result.getRatio().multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
        String text = String.format(
                "[OverspendingRule] Spending for %s is at %s%% of expected pace (spent $%s vs expected $%s). " +
                "To remain within monthly budget, new adjusted daily limit is $%s for the remaining %d days.",
                result.getCategoryName(),
                percentage,
                result.getSpent(),
                result.getExpectedSpent(),
                result.getAdjustedDailyLimit(),
                result.getDaysLeft()
        );
        return Optional.of(text);
    }
}
