package com.financeapp.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility model calculating and categorizing budget utilisation levels.
 * Utilisation = (spent / budget) * 100
 * Levels:
 *  - Info: at 70% (70.00% to 89.99%)
 *  - Warning: at 90% (90.00% to 99.99%)
 *  - Critical: at 100%+ (>= 100.00%)
 *  - Normal: below 70%
 */
public class BudgetUtilisation {

    public enum Level {
        NORMAL("Normal", "success"),
        INFO("Info (>= 70%)", "info"),
        WARNING("Warning (>= 90%)", "warning"),
        CRITICAL("Critical (>= 100%)", "danger");

        private final String label;
        private final String cssClass;

        Level(String label, String cssClass) {
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

    private final BigDecimal spent;
    private final BigDecimal budget;
    private final BigDecimal percentage;
    private final Level level;

    public BudgetUtilisation(BigDecimal spent, BigDecimal budget) {
        this.spent = (spent != null) ? spent.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.budget = (budget != null) ? budget.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.percentage = calculatePercentage(this.spent, this.budget);
        this.level = determineLevel(this.percentage);
    }

    public static BigDecimal calculatePercentage(BigDecimal spent, BigDecimal budget) {
        if (spent == null || spent.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (budget == null || budget.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal("100.00");
        }
        return spent.multiply(BigDecimal.valueOf(100))
                .divide(budget, 2, RoundingMode.HALF_UP);
    }

    public static Level determineLevel(BigDecimal percentage) {
        if (percentage == null) {
            return Level.NORMAL;
        }
        if (percentage.compareTo(new BigDecimal("100.00")) >= 0) {
            return Level.CRITICAL;
        } else if (percentage.compareTo(new BigDecimal("90.00")) >= 0) {
            return Level.WARNING;
        } else if (percentage.compareTo(new BigDecimal("70.00")) >= 0) {
            return Level.INFO;
        } else {
            return Level.NORMAL;
        }
    }

    public static Level calculateLevel(BigDecimal spent, BigDecimal budget) {
        return determineLevel(calculatePercentage(spent, budget));
    }

    public BigDecimal getSpent() {
        return spent;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public Level getLevel() {
        return level;
    }

    public boolean isCritical() {
        return level == Level.CRITICAL;
    }

    public boolean isWarning() {
        return level == Level.WARNING;
    }

    public boolean isInfo() {
        return level == Level.INFO;
    }
}
