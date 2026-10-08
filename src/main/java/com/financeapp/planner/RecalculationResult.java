package com.financeapp.planner;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Result data transfer object encapsulating mid-month pace recalculation, limits, and advice.
 */
public class RecalculationResult {

    private String categoryName;
    private BigDecimal budget = BigDecimal.ZERO;
    private BigDecimal spent = BigDecimal.ZERO;
    private int daysInMonth;
    private int dayOfMonth;
    private int daysLeft;
    private BigDecimal initialDailyLimit = BigDecimal.ZERO;
    private BigDecimal expectedSpent = BigDecimal.ZERO;
    private BigDecimal ratio = BigDecimal.ZERO;
    private PlanStatus status = PlanStatus.ON_TRACK;
    private BigDecimal adjustedDailyLimit = BigDecimal.ZERO;
    private BigDecimal surplus = BigDecimal.ZERO;
    private BigDecimal spreadLimit = BigDecimal.ZERO;
    private List<String> adviceList = new ArrayList<>();

    public RecalculationResult() {
    }

    public static RecalculationResult compute(String categoryName, BigDecimal budget, BigDecimal spent,
                                              int dayOfMonth, int daysInMonth) {
        RecalculationResult res = new RecalculationResult();
        res.categoryName = (categoryName != null) ? categoryName : "Category";
        res.budget = (budget != null) ? budget.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        res.spent = (spent != null) ? spent.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        res.daysInMonth = Math.max(daysInMonth, 1);
        res.dayOfMonth = Math.max(dayOfMonth, 1);
        res.daysLeft = Math.max(res.daysInMonth - res.dayOfMonth, 0);

        // Daily pace over whole month
        res.initialDailyLimit = res.budget.divide(BigDecimal.valueOf(res.daysInMonth), 2, RoundingMode.HALF_UP);

        // Expected spent through current day of month
        res.expectedSpent = res.initialDailyLimit.multiply(BigDecimal.valueOf(res.dayOfMonth)).setScale(2, RoundingMode.HALF_UP);

        // Spending pace ratio: spent / expectedSpent
        if (res.expectedSpent.compareTo(BigDecimal.ZERO) > 0) {
            res.ratio = res.spent.divide(res.expectedSpent, 4, RoundingMode.HALF_UP);
        } else {
            res.ratio = BigDecimal.ONE;
        }

        res.status = PlanStatus.fromRatio(res.ratio);

        // Remaining budget
        BigDecimal remainingBudget = res.budget.subtract(res.spent);
        BigDecimal nonNegativeRemaining = remainingBudget.compareTo(BigDecimal.ZERO) > 0
                ? remainingBudget : BigDecimal.ZERO;

        // Adjusted daily limit = max(budget - spent, 0) / daysLeft
        if (res.daysLeft > 0) {
            res.adjustedDailyLimit = nonNegativeRemaining.divide(BigDecimal.valueOf(res.daysLeft), 2, RoundingMode.HALF_UP);
            res.spreadLimit = remainingBudget.divide(BigDecimal.valueOf(res.daysLeft), 2, RoundingMode.HALF_UP);
        } else {
            res.adjustedDailyLimit = nonNegativeRemaining.setScale(2, RoundingMode.HALF_UP);
            res.spreadLimit = remainingBudget.setScale(2, RoundingMode.HALF_UP);
        }

        // Surplus = max(expectedSpent - spent, 0)
        if (res.expectedSpent.compareTo(res.spent) > 0) {
            res.surplus = res.expectedSpent.subtract(res.spent).setScale(2, RoundingMode.HALF_UP);
        } else {
            res.surplus = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return res;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public BigDecimal getSpent() {
        return spent;
    }

    public int getDaysInMonth() {
        return daysInMonth;
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public int getDaysLeft() {
        return daysLeft;
    }

    public BigDecimal getInitialDailyLimit() {
        return initialDailyLimit;
    }

    public BigDecimal getExpectedSpent() {
        return expectedSpent;
    }

    public BigDecimal getRatio() {
        return ratio;
    }

    public PlanStatus getStatus() {
        return status;
    }

    public BigDecimal getAdjustedDailyLimit() {
        return adjustedDailyLimit;
    }

    public BigDecimal getSurplus() {
        return surplus;
    }

    public BigDecimal getSpreadLimit() {
        return spreadLimit;
    }

    public List<String> getAdviceList() {
        return adviceList;
    }

    public void addAdvice(String advice) {
        if (advice != null && !advice.trim().isEmpty()) {
            this.adviceList.add(advice.trim());
        }
    }

    @Override
    public String toString() {
        return "RecalculationResult{" +
                "category='" + categoryName + '\'' +
                ", status=" + status +
                ", spent=" + spent +
                ", budget=" + budget +
                ", adjustedDailyLimit=" + adjustedDailyLimit +
                ", surplus=" + surplus +
                ", spreadLimit=" + spreadLimit +
                '}';
    }
}
