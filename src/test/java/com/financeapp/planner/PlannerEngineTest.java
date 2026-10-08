package com.financeapp.planner;

import com.financeapp.exception.InsufficientFundsException;
import com.financeapp.service.PlannerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AI Smart Spending Planner Engine & Strategy Tests")
class PlannerEngineTest {

    private PlannerService plannerService;
    private DefaultStrategy defaultStrategy;
    private SavingsFocusedStrategy savingsStrategy;

    @BeforeEach
    void setUp() {
        plannerService = new PlannerService();
        defaultStrategy = new DefaultStrategy();
        savingsStrategy = new SavingsFocusedStrategy();
    }

    @Test
    @DisplayName("income 5000 / fixed 2000 -> available 3000, Food 1200, Savings 600, Emergency 450, Entertainment 300, Other 450")
    void testDefaultStrategyAllocation() {
        BigDecimal income = new BigDecimal("5000.00");
        BigDecimal fixed = new BigDecimal("2000.00");
        BigDecimal available = income.subtract(fixed); // 3000.00

        assertEquals(new BigDecimal("3000.00"), available);

        Map<String, BigDecimal> allocations = defaultStrategy.allocate(available, null);

        assertEquals(new BigDecimal("1200.00"), allocations.get("Food"));
        assertEquals(new BigDecimal("600.00"), allocations.get("Savings"));
        assertEquals(new BigDecimal("450.00"), allocations.get("Emergency"));
        assertEquals(new BigDecimal("300.00"), allocations.get("Entertainment"));
        assertEquals(new BigDecimal("450.00"), allocations.get("Other"));

        // Verify total equals available
        BigDecimal total = allocations.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(available, total);
    }

    @Test
    @DisplayName("Food 1200 over 30 days -> daily 40.00, weekly 280.00")
    void testDailyAndWeeklyLimits() {
        BigDecimal monthlyAmount = new BigDecimal("1200.00");
        int daysInMonth = 30;

        BigDecimal dailyLimit = plannerService.calculateDailyLimit(monthlyAmount, daysInMonth);
        BigDecimal weeklyLimit = plannerService.calculateWeeklyLimit(dailyLimit);

        assertEquals(new BigDecimal("40.00"), dailyLimit);
        assertEquals(new BigDecimal("280.00"), weeklyLimit);
    }

    @Test
    @DisplayName("goal 900 -> Savings 900, Food 1050, Emergency 393.75, Entertainment 262.50, Other 393.75")
    void testSavingsFocusedStrategyAllocation() {
        BigDecimal available = new BigDecimal("3000.00");
        BigDecimal goal = new BigDecimal("900.00"); // 30% of 3000 -> within [20%, 40%] clamp

        Map<String, BigDecimal> allocations = savingsStrategy.allocate(available, goal);

        assertEquals(new BigDecimal("900.00"), allocations.get("Savings"));
        assertEquals(new BigDecimal("1050.00"), allocations.get("Food"));
        assertEquals(new BigDecimal("393.75"), allocations.get("Emergency"));
        assertEquals(new BigDecimal("262.50"), allocations.get("Entertainment"));
        assertEquals(new BigDecimal("393.75"), allocations.get("Other"));

        assertFalse(savingsStrategy.isGoalClamped(), "Goal of 30% should not be clamped");
        assertTrue(savingsStrategy.getClampWarning().isEmpty());

        // Verify total equals available
        BigDecimal total = allocations.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(available, total);
    }

    @Test
    @DisplayName("day 10 food spent 600 -> OVER, new daily limit 30.00")
    void testRecalculateOverspending() {
        BigDecimal budget = new BigDecimal("1200.00");
        BigDecimal spent = new BigDecimal("600.00");
        int dayOfMonth = 10;
        int daysInMonth = 30;

        RecalculationResult result = plannerService.recalculate("Food", budget, spent, dayOfMonth, daysInMonth);

        assertEquals(PlanStatus.OVER, result.getStatus());
        assertEquals(new BigDecimal("30.00"), result.getAdjustedDailyLimit());
        assertEquals(20, result.getDaysLeft());
        assertEquals(new BigDecimal("400.00"), result.getExpectedSpent());
        assertEquals(new BigDecimal("1.5000"), result.getRatio());

        // Verify OverspendingRule fired and produced advice with numbers
        assertFalse(result.getAdviceList().isEmpty());
        assertTrue(result.getAdviceList().stream().anyMatch(a -> a.contains("[OverspendingRule]") && a.contains("30.00")));
    }

    @Test
    @DisplayName("day 10 food spent 250 -> UNDER, surplus 150, spread limit 47.50")
    void testRecalculateSurplus() {
        BigDecimal budget = new BigDecimal("1200.00");
        BigDecimal spent = new BigDecimal("250.00");
        int dayOfMonth = 10;
        int daysInMonth = 30;

        RecalculationResult result = plannerService.recalculate("Food", budget, spent, dayOfMonth, daysInMonth);

        assertEquals(PlanStatus.UNDER, result.getStatus());
        assertEquals(new BigDecimal("150.00"), result.getSurplus());
        assertEquals(new BigDecimal("47.50"), result.getSpreadLimit());
        assertEquals(20, result.getDaysLeft());
        assertEquals(new BigDecimal("400.00"), result.getExpectedSpent());

        // Verify SurplusRule fired and produced advice with numbers
        assertFalse(result.getAdviceList().isEmpty());
        assertTrue(result.getAdviceList().stream().anyMatch(a -> a.contains("[SurplusRule]") && a.contains("150.00") && a.contains("47.50")));
    }

    @Test
    @DisplayName("income 2000 / fixed 2500 -> InsufficientFundsException")
    void testInsufficientFundsException() {
        BigDecimal income = new BigDecimal("2000.00");
        BigDecimal fixed = new BigDecimal("2500.00");

        assertThrows(InsufficientFundsException.class, () ->
                plannerService.createPlan(null, 1L, "2026-10", income, fixed, null, defaultStrategy)
        );
    }

    @Test
    @DisplayName("Allocations must always sum to available exactly across arbitrary amounts")
    void testAllocationsAlwaysSumToAvailable() {
        BigDecimal[] testAmounts = {
                new BigDecimal("1000.00"),
                new BigDecimal("3333.33"),
                new BigDecimal("4999.99"),
                new BigDecimal("123.45"),
                new BigDecimal("8888.88"),
                new BigDecimal("0.01"),
                new BigDecimal("10.00")
        };

        for (BigDecimal available : testAmounts) {
            // Test DefaultStrategy
            Map<String, BigDecimal> defaultAlloc = defaultStrategy.allocate(available, null);
            BigDecimal defaultSum = defaultAlloc.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(available, defaultSum, "DefaultStrategy allocations must sum to available: " + available);

            // Test SavingsFocusedStrategy
            Map<String, BigDecimal> savingsAlloc = savingsStrategy.allocate(available, available.multiply(new BigDecimal("0.35")));
            BigDecimal savingsSum = savingsAlloc.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(available, savingsSum, "SavingsFocusedStrategy allocations must sum to available: " + available);
        }
    }

    @Test
    @DisplayName("Should clamp savings goal when below 20% or above 40% and expose warning")
    void testSavingsClampingAndWarnings() {
        BigDecimal available = new BigDecimal("1000.00");

        // Case 1: Below 20% (Goal $100 = 10%)
        Map<String, BigDecimal> allocLow = savingsStrategy.allocate(available, new BigDecimal("100.00"));
        assertTrue(savingsStrategy.isGoalClamped(), "Should be clamped when requested goal is below 20%");
        assertTrue(savingsStrategy.getClampWarning().isPresent());
        assertTrue(savingsStrategy.getClampWarning().get().contains("clamped to 20.00%"));
        assertEquals(new BigDecimal("200.00"), allocLow.get("Savings"));

        // Case 2: Above 40% (Goal $500 = 50%)
        Map<String, BigDecimal> allocHigh = savingsStrategy.allocate(available, new BigDecimal("500.00"));
        assertTrue(savingsStrategy.isGoalClamped(), "Should be clamped when requested goal exceeds 40%");
        assertTrue(savingsStrategy.getClampWarning().isPresent());
        assertTrue(savingsStrategy.getClampWarning().get().contains("clamped to 40.00%"));
        assertEquals(new BigDecimal("400.00"), allocHigh.get("Savings"));
    }
}
