package com.financeapp.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * Result model representing the feasibility evaluation of a financial goal.
 * Feasibility = (target - current) / months remaining, compared with average monthly surplus.
 */
public class GoalFeasibilityResult {

    public enum Status {
        ACHIEVED("Achieved", "success"),
        FEASIBLE("Feasible", "success"),
        AT_RISK("At Risk", "warning"),
        NOT_FEASIBLE("Not Feasible", "danger");

        private final String label;
        private final String cssClass;

        Status(String label, String cssClass) {
            this.label = label;
            this.cssClass = cssClass;
        }

        public String getLabel() {
            return label;
        }

        public String getCssClass() {
            return cssClass;
        }
    }

    private final Goal goal;
    private final BigDecimal targetAmount;
    private final BigDecimal currentAmount;
    private final BigDecimal remainingAmount;
    private final long monthsRemaining;
    private final BigDecimal requiredMonthlySavings;
    private final BigDecimal averageMonthlySurplus;
    private final Status status;
    private final String explanation;

    public GoalFeasibilityResult(Goal goal, BigDecimal targetAmount, BigDecimal currentAmount,
                                 long monthsRemaining, BigDecimal requiredMonthlySavings,
                                 BigDecimal averageMonthlySurplus, Status status, String explanation) {
        this.goal = goal;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.remainingAmount = (targetAmount != null && currentAmount != null)
                ? targetAmount.subtract(currentAmount).max(BigDecimal.ZERO)
                : BigDecimal.ZERO;
        this.monthsRemaining = monthsRemaining;
        this.requiredMonthlySavings = requiredMonthlySavings;
        this.averageMonthlySurplus = averageMonthlySurplus;
        this.status = status;
        this.explanation = explanation;
    }

    /**
     * Pure calculation engine for goal feasibility evaluation.
     */
    public static GoalFeasibilityResult compute(Goal goal, LocalDate currentDate, BigDecimal averageMonthlySurplus) {
        BigDecimal target = (goal != null && goal.getTargetAmount() != null)
                ? goal.getTargetAmount() : BigDecimal.ZERO;
        BigDecimal current = (goal != null && goal.getCurrentAmount() != null)
                ? goal.getCurrentAmount() : BigDecimal.ZERO;
        LocalDate targetDate = (goal != null && goal.getTargetDate() != null)
                ? goal.getTargetDate() : currentDate.plusMonths(1);

        return compute(goal, target, current, targetDate, currentDate, averageMonthlySurplus);
    }

    public static GoalFeasibilityResult compute(Goal goal, BigDecimal target, BigDecimal current,
                                                LocalDate targetDate, LocalDate currentDate,
                                                BigDecimal averageMonthlySurplus) {
        if (target == null) target = BigDecimal.ZERO;
        if (current == null) current = BigDecimal.ZERO;
        if (currentDate == null) currentDate = LocalDate.now();
        if (targetDate == null) targetDate = currentDate.plusMonths(1);
        if (averageMonthlySurplus == null) averageMonthlySurplus = BigDecimal.ZERO;

        BigDecimal remaining = target.subtract(current);

        // 1. Goal already achieved
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            String explanation = String.format("Goal Achieved! You have successfully reached your target of $%s.",
                    target.setScale(2, RoundingMode.HALF_UP));
            return new GoalFeasibilityResult(goal, target, current, 0, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    averageMonthlySurplus, Status.ACHIEVED, explanation);
        }

        // 2. Compute months remaining
        YearMonth startYm = YearMonth.from(currentDate);
        YearMonth targetYm = YearMonth.from(targetDate);
        long months = ChronoUnit.MONTHS.between(startYm, targetYm);
        if (months <= 0) {
            months = 1;
        }

        // 3. Required monthly savings = (target - current) / months remaining
        BigDecimal required = remaining.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);

        // 4. Compare with average monthly surplus
        Status status;
        String explanation;

        if (averageMonthlySurplus.compareTo(required) >= 0) {
            status = Status.FEASIBLE;
            explanation = String.format(
                    "Feasible: You need to save $%s/month over %d month(s). Your average monthly surplus of $%s comfortably covers this goal.",
                    required, months, averageMonthlySurplus.setScale(2, RoundingMode.HALF_UP)
            );
        } else if (averageMonthlySurplus.compareTo(BigDecimal.ZERO) > 0) {
            status = Status.AT_RISK;
            BigDecimal shortfall = required.subtract(averageMonthlySurplus).setScale(2, RoundingMode.HALF_UP);
            explanation = String.format(
                    "At Risk: You need to save $%s/month over %d month(s), but your average monthly surplus is only $%s (monthly shortfall of $%s). Consider extending your target date or trimming expenses.",
                    required, months, averageMonthlySurplus.setScale(2, RoundingMode.HALF_UP), shortfall
            );
        } else {
            status = Status.NOT_FEASIBLE;
            explanation = String.format(
                    "Not Feasible: Your current monthly expenses meet or exceed your income (monthly surplus: $%s). To achieve this goal, you need to save $%s/month over %d month(s). Please reduce discretionary spending to generate a positive surplus.",
                    averageMonthlySurplus.setScale(2, RoundingMode.HALF_UP), required, months
            );
        }

        return new GoalFeasibilityResult(goal, target, current, months, required, averageMonthlySurplus, status, explanation);
    }

    public Goal getGoal() {
        return goal;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public BigDecimal getCurrentAmount() {
        return currentAmount;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public long getMonthsRemaining() {
        return monthsRemaining;
    }

    public BigDecimal getRequiredMonthlySavings() {
        return requiredMonthlySavings;
    }

    public BigDecimal getAverageMonthlySurplus() {
        return averageMonthlySurplus;
    }

    public Status getStatus() {
        return status;
    }

    public String getExplanation() {
        return explanation;
    }

    public boolean isFeasible() {
        return status == Status.FEASIBLE || status == Status.ACHIEVED;
    }
}
