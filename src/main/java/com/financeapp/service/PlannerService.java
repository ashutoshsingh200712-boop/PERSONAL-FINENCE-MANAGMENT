package com.financeapp.service;

import com.financeapp.dao.*;
import com.financeapp.dao.jdbc.*;
import com.financeapp.exception.DataAccessException;
import com.financeapp.exception.InsufficientFundsException;
import com.financeapp.model.*;
import com.financeapp.planner.*;
import com.financeapp.util.DBConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI Smart Spending Planner business service orchestrating rule-based allocations,
 * limits calculations, mid-month recalculations, and explainable advice rules.
 */
public class PlannerService {

    private final IncomeDAO incomeDAO;
    private final FixedExpenseDAO fixedExpenseDAO;
    private final ExpenseDAO expenseDAO;
    private final SpendingPlanDAO spendingPlanDAO;
    private final CategoryDAO categoryDAO;
    private final BudgetDAO budgetDAO;
    private final SettingsDAO settingsDAO;
    private final List<AdviceRule> adviceRules;

    public PlannerService() {
        this(
                new JdbcIncomeDAO(),
                new JdbcFixedExpenseDAO(),
                new JdbcExpenseDAO(),
                new JdbcSpendingPlanDAO(),
                new JdbcCategoryDAO(),
                new JdbcBudgetDAO(),
                new JdbcSettingsDAO()
        );
    }

    public PlannerService(IncomeDAO incomeDAO, FixedExpenseDAO fixedExpenseDAO,
                          ExpenseDAO expenseDAO, SpendingPlanDAO spendingPlanDAO,
                          CategoryDAO categoryDAO, BudgetDAO budgetDAO,
                          SettingsDAO settingsDAO) {
        this.incomeDAO = incomeDAO;
        this.fixedExpenseDAO = fixedExpenseDAO;
        this.expenseDAO = expenseDAO;
        this.spendingPlanDAO = spendingPlanDAO;
        this.categoryDAO = categoryDAO;
        this.budgetDAO = budgetDAO;
        this.settingsDAO = settingsDAO;
        this.adviceRules = List.of(
                new OverspendingRule(),
                new SurplusRule(),
                new LowBalanceRule()
        );
    }

    /**
     * Calculates initial daily spending pace limit.
     */
    public BigDecimal calculateDailyLimit(BigDecimal monthlyAmount, int daysInMonth) {
        if (monthlyAmount == null || daysInMonth <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return monthlyAmount.divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates weekly spending pace limit based on daily limit.
     */
    public BigDecimal calculateWeeklyLimit(BigDecimal dailyLimit) {
        if (dailyLimit == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return dailyLimit.multiply(BigDecimal.valueOf(7)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Overload calculating weekly limit directly from monthly amount and days in month.
     */
    public BigDecimal calculateWeeklyLimit(BigDecimal monthlyAmount, int daysInMonth) {
        BigDecimal daily = calculateDailyLimit(monthlyAmount, daysInMonth);
        return calculateWeeklyLimit(daily);
    }

    /**
     * Creates and saves an AI Smart Spending Plan for a user using explicit income and fixed figures.
     * Throws InsufficientFundsException if available (income - fixed) is <= 0.
     * Persists plan and plan_items in a single database transaction.
     */
    public long createPlan(Connection conn, long userId, String month, BigDecimal income,
                           BigDecimal fixedExpenses, BigDecimal savingsGoal, AllocationStrategy strategy) {
        if (income == null) income = BigDecimal.ZERO;
        if (fixedExpenses == null) fixedExpenses = BigDecimal.ZERO;

        BigDecimal available = income.subtract(fixedExpenses).setScale(2, RoundingMode.HALF_UP);
        if (available.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InsufficientFundsException(String.format(
                    "Insufficient funds to generate spending plan: available income is $%s (income $%s - fixed obligations $%s)",
                    available, income, fixedExpenses
            ));
        }

        AllocationStrategy activeStrategy = (strategy != null) ? strategy : new DefaultStrategy();
        Map<String, BigDecimal> allocations = activeStrategy.allocate(available, savingsGoal);

        SpendingPlan plan = new SpendingPlan();
        plan.setUserId(userId);
        plan.setPlanMonth(month);
        plan.setTotalIncome(income);
        plan.setTotalFixedExpenses(fixedExpenses);
        plan.setDisposableIncome(available);
        plan.setStatus(SpendingPlan.Status.ACTIVE);

        for (Map.Entry<String, BigDecimal> entry : allocations.entrySet()) {
            String catName = entry.getKey();
            BigDecimal amount = entry.getValue();

            // Resolve category id
            long catId = resolveCategoryId(conn, catName);
            BigDecimal weight = (available.compareTo(BigDecimal.ZERO) > 0)
                    ? amount.multiply(BigDecimal.valueOf(100)).divide(available, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            PlanItem item = new PlanItem();
            item.setCategoryId(catId);
            item.setCategoryName(catName);
            item.setAllocatedAmount(amount);
            item.setWeightPercentage(weight);
            item.setNotes("AI Allocation: " + activeStrategy.getClass().getSimpleName());
            plan.addItem(item);
        }

        return spendingPlanDAO.create(conn, plan);
    }

    /**
     * Overload creating and persisting plan without caller connection (manages transaction).
     */
    public long createPlan(long userId, String month, BigDecimal income, BigDecimal fixedExpenses,
                           BigDecimal savingsGoal, AllocationStrategy strategy) {
        try (Connection conn = DBConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                long planId = createPlan(conn, userId, month, income, fixedExpenses, savingsGoal, strategy);
                conn.commit();
                return planId;
            } catch (Exception e) {
                DBConnection.rollbackQuietly(conn);
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to execute createPlan transaction: " + e.getMessage(), e);
        }
    }

    /**
     * Creates and saves plan by reading user income and fixed expenses from DAOs.
     */
    public long createPlan(Connection conn, long userId, String month, BigDecimal savingsGoal,
                           AllocationStrategy strategy) {
        List<Income> incomes = incomeDAO.findByUserAndMonth(userId, month);
        if (incomes.isEmpty()) {
            incomes = incomeDAO.findByUserId(userId);
        }
        BigDecimal totalIncome = incomes.stream()
                .map(Income::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<FixedExpense> fixedList = fixedExpenseDAO.findActiveByUserId(userId);
        BigDecimal totalFixed = fixedList.stream()
                .map(FixedExpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return createPlan(conn, userId, month, totalIncome, totalFixed, savingsGoal, strategy);
    }

    public long createPlan(long userId, String month, BigDecimal savingsGoal, AllocationStrategy strategy) {
        try (Connection conn = DBConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                long planId = createPlan(conn, userId, month, savingsGoal, strategy);
                conn.commit();
                return planId;
            } catch (Exception e) {
                DBConnection.rollbackQuietly(conn);
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to execute createPlan transaction: " + e.getMessage(), e);
        }
    }

    /**
     * Pure recalculation calculation given category metrics and days.
     */
    public RecalculationResult recalculate(String categoryName, BigDecimal budget, BigDecimal spent,
                                          int dayOfMonth, int daysInMonth) {
        RecalculationResult result = RecalculationResult.compute(categoryName, budget, spent, dayOfMonth, daysInMonth);
        for (AdviceRule rule : adviceRules) {
            rule.evaluate(result).ifPresent(result::addAdvice);
        }
        return result;
    }

    /**
     * Database-driven recalculation for a user category in a given month.
     */
    public RecalculationResult recalculate(Connection conn, long userId, String month, long categoryId,
                                          int dayOfMonth, int daysInMonth) {
        String categoryName = "Category";
        Optional<Category> catOpt = categoryDAO.findById(categoryId);
        if (catOpt.isPresent()) {
            categoryName = catOpt.get().getName();
        }

        // 1. Determine category budget limit
        BigDecimal budgetLimit = BigDecimal.ZERO;
        Optional<Budget> budgetOpt = budgetDAO.findByUserCategoryAndMonth(userId, categoryId, month);
        if (budgetOpt.isPresent()) {
            budgetLimit = budgetOpt.get().getBudgetLimit();
        } else {
            Optional<SpendingPlan> planOpt = spendingPlanDAO.findByUserAndMonth(userId, month);
            if (planOpt.isPresent()) {
                budgetLimit = planOpt.get().getItems().stream()
                        .filter(item -> Objects.equals(item.getCategoryId(), categoryId))
                        .map(PlanItem::getAllocatedAmount)
                        .findFirst()
                        .orElse(BigDecimal.ZERO);
            }
        }

        // 2. Query total spent so far in this month and category
        List<Expense> expenses = expenseDAO.findByMonthAndCategory(userId, month, categoryId, 10000, 0);
        BigDecimal totalSpent = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return recalculate(categoryName, budgetLimit, totalSpent, dayOfMonth, daysInMonth);
    }

    public RecalculationResult recalculate(long userId, String month, long categoryId,
                                          int dayOfMonth, int daysInMonth) {
        try (Connection conn = DBConnection.getConnection()) {
            return recalculate(conn, userId, month, categoryId, dayOfMonth, daysInMonth);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to recalculate plan metrics: " + e.getMessage(), e);
        }
    }

    /**
     * Aggregates total expenditure per category using Java Streams and Collectors.groupingBy.
     */
    public Map<String, BigDecimal> calculateSpentPerCategory(List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            return Collections.emptyMap();
        }
        return expenses.stream()
                .collect(Collectors.groupingBy(
                        e -> (e.getCategoryName() != null && !e.getCategoryName().isEmpty())
                                ? e.getCategoryName() : "Other",
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));
    }

    /**
     * Aggregates total expenditure by YearMonth using Java Streams into a sorted TreeMap.
     */
    public TreeMap<YearMonth, BigDecimal> calculateMonthlyTotals(List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            return new TreeMap<>();
        }
        return expenses.stream()
                .filter(e -> e.getDate() != null && e.getAmount() != null)
                .collect(Collectors.groupingBy(
                        e -> YearMonth.from(e.getDate()),
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));
    }

    private long resolveCategoryId(Connection conn, String categoryName) {
        Optional<Category> opt = categoryDAO.findByName(categoryName);
        if (opt.isPresent()) {
            return opt.get().getId();
        }
        Category newCat = new Category(null, categoryName, categoryName + " category", true, null);
        return categoryDAO.create(conn, newCat);
    }
}
