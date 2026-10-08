package com.financeapp.planner;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Advice rule triggered when category spending is below 80% of linear expected trajectory.
 */
public class SurplusRule implements AdviceRule {

    @Override
    public Optional<String> evaluate(RecalculationResult result) {
        if (result == null || result.getStatus() != PlanStatus.UNDER) {
            return Optional.empty();
        }

        BigDecimal percentage = result.getRatio().multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
        String text = String.format(
                "[SurplusRule] Spending for %s is at %s%% of expected pace (spent $%s vs expected $%s). " +
                "You have accumulated a surplus of $%s; spread daily limit is increased to $%s for the remaining %d days.",
                result.getCategoryName(),
                percentage,
                result.getSpent(),
                result.getExpectedSpent(),
                result.getSurplus(),
                result.getSpreadLimit(),
                result.getDaysLeft()
        );
        return Optional.of(text);
    }
}
