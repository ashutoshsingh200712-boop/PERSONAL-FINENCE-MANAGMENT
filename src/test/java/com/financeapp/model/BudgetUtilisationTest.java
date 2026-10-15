package com.financeapp.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Budget Utilisation Unit Tests")
class BudgetUtilisationTest {

    @Test
    @DisplayName("Normal level: utilisation < 70%")
    void testNormalLevelUnder70Percent() {
        BigDecimal budget = new BigDecimal("1000.00");
        BigDecimal spent = new BigDecimal("699.90"); // 69.99%

        BudgetUtilisation util = new BudgetUtilisation(spent, budget);

        assertEquals(BudgetUtilisation.Level.NORMAL, util.getLevel());
        assertEquals(new BigDecimal("69.99"), util.getPercentage());
        assertFalse(util.isInfo());
        assertFalse(util.isWarning());
        assertFalse(util.isCritical());
    }

    @Test
    @DisplayName("Info level: utilisation exactly at 70%")
    void testInfoLevelExact70Percent() {
        BigDecimal budget = new BigDecimal("1000.00");
        BigDecimal spent = new BigDecimal("700.00"); // 70.00%

        BudgetUtilisation util = new BudgetUtilisation(spent, budget);

        assertEquals(BudgetUtilisation.Level.INFO, util.getLevel());
        assertEquals(new BigDecimal("70.00"), util.getPercentage());
        assertTrue(util.isInfo());
        assertFalse(util.isWarning());
        assertFalse(util.isCritical());
    }

    @Test
    @DisplayName("Info level: utilisation between 70% and 89.99%")
    void testInfoLevelMidRange() {
        BigDecimal budget = new BigDecimal("1000.00");
        BigDecimal spent = new BigDecimal("850.00"); // 85.00%

        BudgetUtilisation util = new BudgetUtilisation(spent, budget);

        assertEquals(BudgetUtilisation.Level.INFO, util.getLevel());
        assertEquals(new BigDecimal("85.00"), util.getPercentage());
        assertTrue(util.isInfo());
    }

    @Test
    @DisplayName("Warning level: utilisation exactly at 90%")
    void testWarningLevelExact90Percent() {
        BigDecimal budget = new BigDecimal("1000.00");
        BigDecimal spent = new BigDecimal("900.00"); // 90.00%

        BudgetUtilisation util = new BudgetUtilisation(spent, budget);

        assertEquals(BudgetUtilisation.Level.WARNING, util.getLevel());
        assertEquals(new BigDecimal("90.00"), util.getPercentage());
        assertTrue(util.isWarning());
        assertFalse(util.isInfo());
        assertFalse(util.isCritical());
    }

    @Test
    @DisplayName("Warning level: utilisation between 90% and 99.99%")
    void testWarningLevelHighRange() {
        BigDecimal budget = new BigDecimal("1000.00");
        BigDecimal spent = new BigDecimal("990.00"); // 99.00%

        BudgetUtilisation util = new BudgetUtilisation(spent, budget);

        assertEquals(BudgetUtilisation.Level.WARNING, util.getLevel());
        assertEquals(new BigDecimal("99.00"), util.getPercentage());
        assertTrue(util.isWarning());
    }

    @Test
    @DisplayName("Critical level: utilisation exactly at 100%")
    void testCriticalLevelExact100Percent() {
        BigDecimal budget = new BigDecimal("1000.00");
        BigDecimal spent = new BigDecimal("1000.00"); // 100.00%

        BudgetUtilisation util = new BudgetUtilisation(spent, budget);

        assertEquals(BudgetUtilisation.Level.CRITICAL, util.getLevel());
        assertEquals(new BigDecimal("100.00"), util.getPercentage());
        assertTrue(util.isCritical());
    }

    @Test
    @DisplayName("Critical level: utilisation exceeding 100% (e.g. 150%)")
    void testCriticalLevelOver100Percent() {
        BigDecimal budget = new BigDecimal("500.00");
        BigDecimal spent = new BigDecimal("750.00"); // 150.00%

        BudgetUtilisation util = new BudgetUtilisation(spent, budget);

        assertEquals(BudgetUtilisation.Level.CRITICAL, util.getLevel());
        assertEquals(new BigDecimal("150.00"), util.getPercentage());
        assertTrue(util.isCritical());
    }

    @ParameterizedTest
    @CsvSource({
            "0.00, 1000.00, NORMAL",
            "500.00, 1000.00, NORMAL",
            "699.90, 1000.00, NORMAL",
            "700.00, 1000.00, INFO",
            "899.90, 1000.00, INFO",
            "900.00, 1000.00, WARNING",
            "999.90, 1000.00, WARNING",
            "1000.00, 1000.00, CRITICAL",
            "1500.00, 1000.00, CRITICAL"
    })
    @DisplayName("Parameterized test across boundary thresholds")
    void testUtilisationBoundaries(String spentStr, String budgetStr, BudgetUtilisation.Level expectedLevel) {
        BigDecimal spent = new BigDecimal(spentStr);
        BigDecimal budget = new BigDecimal(budgetStr);

        BudgetUtilisation.Level actual = BudgetUtilisation.calculateLevel(spent, budget);
        assertEquals(expectedLevel, actual);
    }

    @Test
    @DisplayName("Edge case: zero budget with positive spending -> Critical")
    void testZeroBudgetWithPositiveSpent() {
        BigDecimal budget = BigDecimal.ZERO;
        BigDecimal spent = new BigDecimal("100.00");

        BudgetUtilisation util = new BudgetUtilisation(spent, budget);
        assertEquals(BudgetUtilisation.Level.CRITICAL, util.getLevel());
    }

    @Test
    @DisplayName("Edge case: zero spent with positive budget -> Normal (0%)")
    void testZeroSpent() {
        BigDecimal budget = new BigDecimal("1000.00");
        BigDecimal spent = BigDecimal.ZERO;

        BudgetUtilisation util = new BudgetUtilisation(spent, budget);
        assertEquals(BudgetUtilisation.Level.NORMAL, util.getLevel());
        assertEquals(new BigDecimal("0.00"), util.getPercentage());
    }
}
