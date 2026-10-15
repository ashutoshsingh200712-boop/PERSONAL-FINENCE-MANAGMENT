package com.financeapp.service;

import com.financeapp.dao.ExpenseDAO;
import com.financeapp.dao.GoalDAO;
import com.financeapp.dao.IncomeDAO;
import com.financeapp.dao.SpendingPlanDAO;
import com.financeapp.dao.jdbc.JdbcExpenseDAO;
import com.financeapp.dao.jdbc.JdbcGoalDAO;
import com.financeapp.dao.jdbc.JdbcIncomeDAO;
import com.financeapp.dao.jdbc.JdbcSpendingPlanDAO;
import com.financeapp.model.Expense;
import com.financeapp.model.Goal;
import com.financeapp.model.GoalFeasibilityResult;
import com.financeapp.model.Income;
import com.financeapp.model.SpendingPlan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service managing user financial goals and evaluating goal feasibility.
 */
public class GoalService {

    private final GoalDAO goalDAO;
    private final IncomeDAO incomeDAO;
    private final ExpenseDAO expenseDAO;
    private final SpendingPlanDAO spendingPlanDAO;

    public GoalService() {
        this(new JdbcGoalDAO(), new JdbcIncomeDAO(), new JdbcExpenseDAO(), new JdbcSpendingPlanDAO());
    }

    public GoalService(GoalDAO goalDAO, IncomeDAO incomeDAO, ExpenseDAO expenseDAO, SpendingPlanDAO spendingPlanDAO) {
        this.goalDAO = goalDAO;
        this.incomeDAO = incomeDAO;
        this.expenseDAO = expenseDAO;
        this.spendingPlanDAO = spendingPlanDAO;
    }

    public long createGoal(Goal goal) {
        return goalDAO.create(goal);
    }

    public Optional<Goal> findById(long id) {
        return goalDAO.findById(id);
    }

    public List<Goal> findByUserId(long userId) {
        return goalDAO.findByUserId(userId);
    }

    public boolean updateGoal(Goal goal) {
        return goalDAO.update(goal);
    }

    public boolean deleteGoal(long id) {
        return goalDAO.delete(id);
    }

    /**
     * Calculates the average monthly surplus for a user based on active spending plans,
     * income records, and expense patterns.
     */
    public BigDecimal calculateAverageMonthlySurplus(long userId) {
        YearMonth currentYm = YearMonth.now();
        String currentMonth = currentYm.toString();

        // 1. Try calculating from active spending plan disposable income minus recent spending
        Optional<SpendingPlan> planOpt = spendingPlanDAO.findByUserAndMonth(userId, currentMonth);
        if (planOpt.isPresent()) {
            SpendingPlan plan = planOpt.get();
            BigDecimal disposable = plan.getDisposableIncome();
            List<Expense> expenses = expenseDAO.findByMonthAndCategory(userId, currentMonth, null, 1000, 0);
            BigDecimal totalSpent = expenses.stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal surplus = disposable.subtract(totalSpent).setScale(2, RoundingMode.HALF_UP);
            return surplus;
        }

        // 2. Fallback: Monthly income minus monthly expenses
        List<Income> incomes = incomeDAO.findByUserAndMonth(userId, currentMonth);
        if (incomes.isEmpty()) {
            incomes = incomeDAO.findByUserId(userId);
        }
        BigDecimal totalIncome = incomes.stream()
                .map(Income::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Expense> expenses = expenseDAO.findByMonthAndCategory(userId, currentMonth, null, 1000, 0);
        BigDecimal totalExpenses = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalIncome.subtract(totalExpenses).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Evaluates feasibility for all goals of a user.
     */
    public List<GoalFeasibilityResult> evaluateUserGoals(long userId) {
        List<Goal> goals = goalDAO.findByUserId(userId);
        BigDecimal surplus = calculateAverageMonthlySurplus(userId);
        LocalDate today = LocalDate.now();

        List<GoalFeasibilityResult> results = new ArrayList<>();
        for (Goal g : goals) {
            results.add(GoalFeasibilityResult.compute(g, today, surplus));
        }
        return results;
    }

    /**
     * Evaluates feasibility for a specific goal.
     */
    public GoalFeasibilityResult evaluateGoal(Goal goal, LocalDate currentDate) {
        BigDecimal surplus = calculateAverageMonthlySurplus(goal.getUserId());
        return GoalFeasibilityResult.compute(goal, currentDate, surplus);
    }
}
