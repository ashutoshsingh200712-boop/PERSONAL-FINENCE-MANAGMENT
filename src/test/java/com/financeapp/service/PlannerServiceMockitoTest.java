package com.financeapp.service;

import com.financeapp.dao.*;
import com.financeapp.exception.InsufficientFundsException;
import com.financeapp.model.*;
import com.financeapp.planner.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlannerService Mockito Unit Tests with Mocked DAOs")
class PlannerServiceMockitoTest {

    @Mock
    private IncomeDAO incomeDAO;

    @Mock
    private FixedExpenseDAO fixedExpenseDAO;

    @Mock
    private ExpenseDAO expenseDAO;

    @Mock
    private SpendingPlanDAO spendingPlanDAO;

    @Mock
    private CategoryDAO categoryDAO;

    @Mock
    private BudgetDAO budgetDAO;

    @Mock
    private SettingsDAO settingsDAO;

    private Connection mockConnection;
    private PlannerService plannerService;

    @BeforeEach
    void setUp() throws Exception {
        mockConnection = java.sql.DriverManager.getConnection("jdbc:h2:mem:mock_test;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        plannerService = new PlannerService(
                incomeDAO,
                fixedExpenseDAO,
                expenseDAO,
                spendingPlanDAO,
                categoryDAO,
                budgetDAO,
                settingsDAO
        );
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() throws Exception {
        if (mockConnection != null && !mockConnection.isClosed()) {
            mockConnection.close();
        }
    }

    @Test
    @DisplayName("Should create plan and plan_items in single transaction using mocked DAOs")
    void testCreatePlanWithMockedDaos() {
        long userId = 1L;
        String month = "2026-10";

        // Mock Income (total = 5000)
        Income income = new Income(1L, userId, "Salary", new BigDecimal("5000.00"),
                Income.Frequency.MONTHLY, LocalDate.of(2026, 10, 1), null, null);
        when(incomeDAO.findByUserAndMonth(userId, month)).thenReturn(List.of(income));

        // Mock Fixed Expense (total = 2000)
        FixedExpense fixed = new FixedExpense(1L, userId, 5L, "Rent", new BigDecimal("2000.00"),
                1, true, null, null);
        when(fixedExpenseDAO.findActiveByUserId(userId)).thenReturn(List.of(fixed));

        // Mock Category lookups
        when(categoryDAO.findByName(anyString())).thenAnswer(invocation -> {
            String catName = invocation.getArgument(0);
            return Optional.of(new Category(10L, catName, catName, true, null));
        });

        // Mock SpendingPlanDAO creation
        when(spendingPlanDAO.create(eq(mockConnection), any(SpendingPlan.class))).thenReturn(999L);

        // Execute createPlan
        long planId = plannerService.createPlan(mockConnection, userId, month, null, new DefaultStrategy());

        assertEquals(999L, planId);

        // Verify spendingPlanDAO called with correct disposable income and items
        ArgumentCaptor<SpendingPlan> planCaptor = ArgumentCaptor.forClass(SpendingPlan.class);
        verify(spendingPlanDAO, times(1)).create(eq(mockConnection), planCaptor.capture());

        SpendingPlan savedPlan = planCaptor.getValue();
        assertEquals(new BigDecimal("3000.00"), savedPlan.getDisposableIncome());
        assertEquals(new BigDecimal("5000.00"), savedPlan.getTotalIncome());
        assertEquals(new BigDecimal("2000.00"), savedPlan.getTotalFixedExpenses());
        assertEquals(5, savedPlan.getItems().size(), "Should contain 5 category plan items");

        // Verify food allocation is 1200
        PlanItem foodItem = savedPlan.getItems().stream()
                .filter(i -> "Food".equals(i.getCategoryName()))
                .findFirst()
                .orElseThrow();
        assertEquals(new BigDecimal("1200.00"), foodItem.getAllocatedAmount());
    }

    @Test
    @DisplayName("Should throw InsufficientFundsException and never invoke DAO when obligations exceed income")
    void testCreatePlanDeficitThrowsException() {
        long userId = 2L;
        String month = "2026-10";

        when(incomeDAO.findByUserAndMonth(userId, month)).thenReturn(List.of(
                new Income(1L, userId, "Part time", new BigDecimal("2000.00"), Income.Frequency.MONTHLY, LocalDate.of(2026, 10, 1), null, null)
        ));
        when(fixedExpenseDAO.findActiveByUserId(userId)).thenReturn(List.of(
                new FixedExpense(1L, userId, 5L, "Rent", new BigDecimal("2500.00"), 1, true, null, null)
        ));

        assertThrows(InsufficientFundsException.class, () ->
                plannerService.createPlan(mockConnection, userId, month, null, new DefaultStrategy())
        );

        verify(spendingPlanDAO, never()).create(any(), any());
    }

    @Test
    @DisplayName("Should recalculate category spending using mocked BudgetDAO and ExpenseDAO")
    void testRecalculateWithMocks() {
        long userId = 3L;
        long categoryId = 1L;
        String month = "2026-10";
        int dayOfMonth = 10;
        int daysInMonth = 30;

        when(categoryDAO.findById(categoryId)).thenReturn(Optional.of(
                new Category(categoryId, "Food", "Food expenses", true, null)
        ));

        when(budgetDAO.findByUserCategoryAndMonth(userId, categoryId, month)).thenReturn(Optional.of(
                new Budget(1L, userId, categoryId, month, new BigDecimal("1200.00"), new BigDecimal("80.00"), null)
        ));

        when(expenseDAO.findByMonthAndCategory(eq(mockConnection), eq(userId), eq(month), eq(categoryId), eq(10000), eq(0))).thenReturn(List.of(
                new Expense(1L, userId, categoryId, new BigDecimal("350.00"), LocalDate.of(2026, 10, 2), "Groceries", "CARD", null),
                new Expense(2L, userId, categoryId, new BigDecimal("250.00"), LocalDate.of(2026, 10, 8), "Dining out", "CARD", null)
        ));

        RecalculationResult result = plannerService.recalculate(mockConnection, userId, month, categoryId, dayOfMonth, daysInMonth);

        assertEquals("Food", result.getCategoryName());
        assertEquals(new BigDecimal("1200.00"), result.getBudget());
        assertEquals(new BigDecimal("600.00"), result.getSpent());
        assertEquals(PlanStatus.OVER, result.getStatus());
        assertEquals(new BigDecimal("30.00"), result.getAdjustedDailyLimit());
        assertFalse(result.getAdviceList().isEmpty());
    }

    @Test
    @DisplayName("Should aggregate category expenses using Java Streams Collectors.groupingBy")
    void testStreamGroupingBy() {
        List<Expense> expenses = List.of(
                new Expense(1L, 1L, 1L, new BigDecimal("50.00"), LocalDate.of(2026, 10, 1), "Grocery", "CARD", null) {{ setCategoryName("Food"); }},
                new Expense(2L, 1L, 1L, new BigDecimal("30.00"), LocalDate.of(2026, 10, 3), "Lunch", "CARD", null) {{ setCategoryName("Food"); }},
                new Expense(3L, 1L, 2L, new BigDecimal("15.00"), LocalDate.of(2026, 10, 4), "Cinema", "CARD", null) {{ setCategoryName("Entertainment"); }}
        );

        Map<String, BigDecimal> perCategory = plannerService.calculateSpentPerCategory(expenses);

        assertEquals(2, perCategory.size());
        assertEquals(new BigDecimal("80.00"), perCategory.get("Food"));
        assertEquals(new BigDecimal("15.00"), perCategory.get("Entertainment"));
    }

    @Test
    @DisplayName("Should aggregate multi-month totals into a sorted TreeMap using Java Streams")
    void testStreamMonthlyTotalsTreeMap() {
        List<Expense> expenses = List.of(
                new Expense(1L, 1L, 1L, new BigDecimal("100.00"), LocalDate.of(2026, 8, 15), "August Exp", "CARD", null),
                new Expense(2L, 1L, 1L, new BigDecimal("250.00"), LocalDate.of(2026, 10, 5), "October Exp", "CARD", null),
                new Expense(3L, 1L, 1L, new BigDecimal("150.00"), LocalDate.of(2026, 9, 20), "September Exp", "CARD", null),
                new Expense(4L, 1L, 1L, new BigDecimal("50.00"), LocalDate.of(2026, 10, 10), "October Exp 2", "CARD", null)
        );

        TreeMap<YearMonth, BigDecimal> monthlyTotals = plannerService.calculateMonthlyTotals(expenses);

        assertEquals(3, monthlyTotals.size());
        // Verify chronological order: August -> September -> October
        Iterator<YearMonth> it = monthlyTotals.keySet().iterator();
        assertEquals(YearMonth.of(2026, 8), it.next());
        assertEquals(YearMonth.of(2026, 9), it.next());
        assertEquals(YearMonth.of(2026, 10), it.next());

        assertEquals(new BigDecimal("100.00"), monthlyTotals.get(YearMonth.of(2026, 8)));
        assertEquals(new BigDecimal("150.00"), monthlyTotals.get(YearMonth.of(2026, 9)));
        assertEquals(new BigDecimal("300.00"), monthlyTotals.get(YearMonth.of(2026, 10)));
    }

    @Test
    @DisplayName("Should read individual weight rows from system_settings, divide whole numbers by 100, and ignore JSON row")
    void testLoadWeightsFromSettings() {
        when(settingsDAO.get("weight_Food")).thenReturn(Optional.of("40"));
        when(settingsDAO.get("weight_Savings")).thenReturn(Optional.of("20"));
        when(settingsDAO.get("weight_Emergency")).thenReturn(Optional.of("15"));
        when(settingsDAO.get("weight_Entertainment")).thenReturn(Optional.of("10"));
        when(settingsDAO.get("weight_Other")).thenReturn(Optional.of("15"));

        Map<String, BigDecimal> weights = plannerService.loadWeightsFromSettings();

        assertEquals(new BigDecimal("0.40"), weights.get("Food"));
        assertEquals(new BigDecimal("0.20"), weights.get("Savings"));
        assertEquals(new BigDecimal("0.15"), weights.get("Emergency"));
        assertEquals(new BigDecimal("0.10"), weights.get("Entertainment"));
        assertEquals(new BigDecimal("0.15"), weights.get("Other"));

        verify(settingsDAO, times(1)).get("weight_Food");
        verify(settingsDAO, times(1)).get("weight_Savings");
        verify(settingsDAO, times(1)).get("weight_Emergency");
        verify(settingsDAO, times(1)).get("weight_Entertainment");
        verify(settingsDAO, times(1)).get("weight_Other");
        verify(settingsDAO, never()).get("planner_default_weights");
    }

    @Test
    @DisplayName("Should use weights from system_settings when strategy parameter is null")
    void testCreatePlanUsesSettingsWeightsWhenStrategyNull() {
        long userId = 5L;
        String month = "2026-10";

        when(settingsDAO.get("weight_Food")).thenReturn(Optional.of("40"));
        when(settingsDAO.get("weight_Savings")).thenReturn(Optional.of("20"));
        when(settingsDAO.get("weight_Emergency")).thenReturn(Optional.of("15"));
        when(settingsDAO.get("weight_Entertainment")).thenReturn(Optional.of("10"));
        when(settingsDAO.get("weight_Other")).thenReturn(Optional.of("15"));

        when(categoryDAO.findByName(anyString())).thenAnswer(invocation -> {
            String catName = invocation.getArgument(0);
            return Optional.of(new Category(10L, catName, catName, true, null));
        });
        when(spendingPlanDAO.create(eq(mockConnection), any(SpendingPlan.class))).thenReturn(101L);

        long planId = plannerService.createPlan(mockConnection, userId, month, new BigDecimal("5000.00"), new BigDecimal("2000.00"), null, null);
        assertEquals(101L, planId);

        ArgumentCaptor<SpendingPlan> planCaptor = ArgumentCaptor.forClass(SpendingPlan.class);
        verify(spendingPlanDAO).create(eq(mockConnection), planCaptor.capture());
        SpendingPlan plan = planCaptor.getValue();
        assertEquals(new BigDecimal("3000.00"), plan.getDisposableIncome());
        PlanItem foodItem = plan.getItems().stream().filter(i -> "Food".equals(i.getCategoryName())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("1200.00"), foodItem.getAllocatedAmount());
    }

    @Test
    @DisplayName("recalculate(Connection, userId) excludes Savings and Emergency transfers and returns adjusted daily limit")
    void testRecalculatePlanExcludesTransfers() {
        long userId = 4L;
        String month = "2026-10";
        int dayOfMonth = 10;
        int daysInMonth = 30;

        // Plan with 5 items: Food (1200), Savings (600 - transfer), Emergency (450 - transfer), Entertainment (300), Other (450)
        // Discretionary daily spending budget = 1200 + 300 + 450 = 1950.00
        SpendingPlan plan = new SpendingPlan();
        plan.setId(10L);
        plan.setUserId(userId);
        plan.setPlanMonth(month);
        plan.setDisposableIncome(new BigDecimal("3000.00"));
        plan.addItem(new PlanItem(1L, 10L, 1L, "Food", new BigDecimal("1200.00"), new BigDecimal("40.00"), null, null));
        plan.addItem(new PlanItem(2L, 10L, 2L, "Savings", new BigDecimal("600.00"), new BigDecimal("20.00"), null, null));
        plan.addItem(new PlanItem(3L, 10L, 3L, "Emergency", new BigDecimal("450.00"), new BigDecimal("15.00"), null, null));
        plan.addItem(new PlanItem(4L, 10L, 4L, "Entertainment", new BigDecimal("300.00"), new BigDecimal("10.00"), null, null));
        plan.addItem(new PlanItem(5L, 10L, 5L, "Other", new BigDecimal("450.00"), new BigDecimal("15.00"), null, null));

        when(spendingPlanDAO.findByUserAndMonth(mockConnection, userId, month)).thenReturn(Optional.of(plan));

        // Expenses for user:
        // Food: 600, Savings: 600 (transfer - excluded), Entertainment: 200, Other: 150
        // Total daily spending spent = 600 + 200 + 150 = 950.00
        List<Expense> expenses = List.of(
                new Expense(1L, userId, 1L, new BigDecimal("600.00"), LocalDate.of(2026, 10, 5), "Groceries", "CARD", null) {{ setCategoryName("Food"); }},
                new Expense(2L, userId, 2L, new BigDecimal("600.00"), LocalDate.of(2026, 10, 5), "Savings transfer", "TRANSFER", null) {{ setCategoryName("Savings"); }},
                new Expense(3L, userId, 4L, new BigDecimal("200.00"), LocalDate.of(2026, 10, 6), "Concert", "CARD", null) {{ setCategoryName("Entertainment"); }},
                new Expense(4L, userId, 5L, new BigDecimal("150.00"), LocalDate.of(2026, 10, 8), "Misc", "CARD", null) {{ setCategoryName("Other"); }}
        );
        when(expenseDAO.findByMonthAndCategory(mockConnection, userId, month, null, 10000, 0)).thenReturn(expenses);

        RecalculationResult result = plannerService.recalculate(mockConnection, userId, month, dayOfMonth, daysInMonth);

        // Daily spending budget = 1950.00 (Savings and Emergency excluded!)
        assertEquals(new BigDecimal("1950.00"), result.getBudget());
        // Daily spending spent = 950.00 (Savings transfer excluded!)
        assertEquals(new BigDecimal("950.00"), result.getSpent());
        // Expected pace through day 10 = (1950 / 30) * 10 = 65.00 * 10 = 650.00
        assertEquals(new BigDecimal("650.00"), result.getExpectedSpent());
        // Ratio = 950 / 650 = 1.4615 > 1.10 -> OVER
        assertEquals(PlanStatus.OVER, result.getStatus());
        // Days left = 30 - 10 = 20
        assertEquals(20, result.getDaysLeft());
        // Adjusted daily limit = max(1950 - 950, 0) / 20 = 1000 / 20 = 50.00
        assertEquals(new BigDecimal("50.00"), result.getAdjustedDailyLimit());
        assertFalse(result.getAdviceList().isEmpty());
    }
}
