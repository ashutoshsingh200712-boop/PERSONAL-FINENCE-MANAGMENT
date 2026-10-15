package com.financeapp.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Goal Feasibility Unit Tests")
class GoalFeasibilityTest {

    @Test
    @DisplayName("Feasible: Monthly surplus strictly covers required savings")
    void testGoalFeasible() {
        LocalDate today = LocalDate.of(2026, 1, 15);
        LocalDate targetDate = LocalDate.of(2026, 6, 15); // 5 months
        BigDecimal target = new BigDecimal("1000.00");
        BigDecimal current = new BigDecimal("0.00"); // remaining = 1000.00 -> required = 200.00/mo
        BigDecimal surplus = new BigDecimal("350.00"); // surplus > 200.00

        Goal goal = new Goal(1L, 10L, "Vacation Fund", target, current, targetDate, null);
        GoalFeasibilityResult res = GoalFeasibilityResult.compute(goal, target, current, targetDate, today, surplus);

        assertEquals(GoalFeasibilityResult.Status.FEASIBLE, res.getStatus());
        assertTrue(res.isFeasible());
        assertEquals(5, res.getMonthsRemaining());
        assertEquals(new BigDecimal("200.00"), res.getRequiredMonthlySavings());
        assertEquals(new BigDecimal("350.00"), res.getAverageMonthlySurplus());

        // Verify clear explanation contains relevant details
        assertNotNull(res.getExplanation());
        assertTrue(res.getExplanation().startsWith("Feasible:"));
        assertTrue(res.getExplanation().contains("$200.00/month"));
        assertTrue(res.getExplanation().contains("5 month(s)"));
        assertTrue(res.getExplanation().contains("$350.00"));
    }

    @Test
    @DisplayName("At Risk: Surplus is positive but less than required monthly savings")
    void testGoalAtRisk() {
        LocalDate today = LocalDate.of(2026, 1, 1);
        LocalDate targetDate = LocalDate.of(2026, 5, 1); // 4 months
        BigDecimal target = new BigDecimal("2000.00");
        BigDecimal current = new BigDecimal("400.00"); // remaining = 1600.00 -> required = 400.00/mo
        BigDecimal surplus = new BigDecimal("250.00"); // surplus < 400.00

        Goal goal = new Goal(2L, 10L, "New Laptop", target, current, targetDate, null);
        GoalFeasibilityResult res = GoalFeasibilityResult.compute(goal, target, current, targetDate, today, surplus);

        assertEquals(GoalFeasibilityResult.Status.AT_RISK, res.getStatus());
        assertFalse(res.isFeasible());
        assertEquals(4, res.getMonthsRemaining());
        assertEquals(new BigDecimal("400.00"), res.getRequiredMonthlySavings());

        // Verify clear explanation identifies risk and monthly shortfall
        assertNotNull(res.getExplanation());
        assertTrue(res.getExplanation().startsWith("At Risk:"));
        assertTrue(res.getExplanation().contains("$400.00/month"));
        assertTrue(res.getExplanation().contains("$250.00"));
        assertTrue(res.getExplanation().contains("$150.00")); // shortfall: 400 - 250 = 150
    }

    @Test
    @DisplayName("Not Feasible: Monthly surplus is zero or negative")
    void testGoalNotFeasibleNegativeSurplus() {
        LocalDate today = LocalDate.of(2026, 1, 1);
        LocalDate targetDate = LocalDate.of(2026, 7, 1); // 6 months
        BigDecimal target = new BigDecimal("3000.00");
        BigDecimal current = new BigDecimal("0.00"); // required = 500.00/mo
        BigDecimal surplus = new BigDecimal("-100.00"); // spending exceeds income

        Goal goal = new Goal(3L, 10L, "Emergency Buffer", target, current, targetDate, null);
        GoalFeasibilityResult res = GoalFeasibilityResult.compute(goal, target, current, targetDate, today, surplus);

        assertEquals(GoalFeasibilityResult.Status.NOT_FEASIBLE, res.getStatus());
        assertFalse(res.isFeasible());
        assertEquals(6, res.getMonthsRemaining());
        assertEquals(new BigDecimal("500.00"), res.getRequiredMonthlySavings());

        // Verify clear explanation states expenses meet or exceed income
        assertNotNull(res.getExplanation());
        assertTrue(res.getExplanation().startsWith("Not Feasible:"));
        assertTrue(res.getExplanation().contains("$-100.00"));
        assertTrue(res.getExplanation().contains("$500.00/month"));
    }

    @Test
    @DisplayName("Achieved: Current saved amount equals or exceeds target amount")
    void testGoalAlreadyAchieved() {
        LocalDate today = LocalDate.of(2026, 1, 1);
        LocalDate targetDate = LocalDate.of(2026, 12, 31);
        BigDecimal target = new BigDecimal("5000.00");
        BigDecimal current = new BigDecimal("5500.00");
        BigDecimal surplus = new BigDecimal("100.00");

        Goal goal = new Goal(4L, 10L, "College Savings", target, current, targetDate, null);
        GoalFeasibilityResult res = GoalFeasibilityResult.compute(goal, target, current, targetDate, today, surplus);

        assertEquals(GoalFeasibilityResult.Status.ACHIEVED, res.getStatus());
        assertTrue(res.isFeasible());
        assertEquals(new BigDecimal("0.00"), res.getRequiredMonthlySavings());
        assertTrue(res.getExplanation().contains("Goal Achieved!"));
    }

    @Test
    @DisplayName("Edge case: Target date is in current month -> minimum 1 month remaining")
    void testTargetDateInCurrentMonth() {
        LocalDate today = LocalDate.of(2026, 3, 10);
        LocalDate targetDate = LocalDate.of(2026, 3, 25);
        BigDecimal target = new BigDecimal("600.00");
        BigDecimal current = new BigDecimal("100.00"); // remaining = 500.00
        BigDecimal surplus = new BigDecimal("500.00");

        Goal goal = new Goal(5L, 10L, "Bill Payment", target, current, targetDate, null);
        GoalFeasibilityResult res = GoalFeasibilityResult.compute(goal, target, current, targetDate, today, surplus);

        assertEquals(1, res.getMonthsRemaining());
        assertEquals(new BigDecimal("500.00"), res.getRequiredMonthlySavings());
        assertEquals(GoalFeasibilityResult.Status.FEASIBLE, res.getStatus());
    }
}
