package com.financeapp.planner;

import java.util.Optional;

/**
 * Rule interface for explainable heuristic financial advice.
 */
public interface AdviceRule {

    /**
     * Evaluates budget and spending trajectory metrics to produce explainable guidance.
     *
     * @param result the recalculation result containing metrics
     * @return explainable advice text including the firing rule and numbers, or empty if not triggered
     */
    Optional<String> evaluate(RecalculationResult result);
}
