package com.financeapp.planner;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Functional strategy interface for computing category envelope budget allocations.
 */
@FunctionalInterface
public interface AllocationStrategy {

    /**
     * Computes the budget distribution across categories for a given available disposable income.
     *
     * @param available   total disposable income (income - fixed expenses)
     * @param savingsGoal target savings amount requested by user (may be null)
     * @return map of category names to allocated monetary amounts in exact insertion order
     */
    Map<String, BigDecimal> allocate(BigDecimal available, BigDecimal savingsGoal);
}
