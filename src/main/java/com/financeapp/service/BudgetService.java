package com.financeapp.service;

import com.financeapp.dao.AdviceDAO;
import com.financeapp.dao.BudgetDAO;
import com.financeapp.dao.ExpenseDAO;
import com.financeapp.dao.NotificationDAO;
import com.financeapp.dao.SpendingPlanDAO;
import com.financeapp.dao.jdbc.JdbcAdviceDAO;
import com.financeapp.dao.jdbc.JdbcBudgetDAO;
import com.financeapp.dao.jdbc.JdbcExpenseDAO;
import com.financeapp.dao.jdbc.JdbcNotificationDAO;
import com.financeapp.dao.jdbc.JdbcSpendingPlanDAO;
import com.financeapp.exception.DataAccessException;
import com.financeapp.model.*;
import com.financeapp.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Service managing budget operations, including transaction-based plan acceptance into budgets
 * and category-level budget utilisation tracking.
 */
public class BudgetService {

    private static final Logger LOGGER = Logger.getLogger(BudgetService.class.getName());

    private final BudgetDAO budgetDAO;
    private final SpendingPlanDAO spendingPlanDAO;
    private final ExpenseDAO expenseDAO;
    private final AdviceDAO adviceDAO;
    private final NotificationDAO notificationDAO;

    public BudgetService() {
        this(new JdbcBudgetDAO(), new JdbcSpendingPlanDAO(), new JdbcExpenseDAO(), new JdbcAdviceDAO(), new JdbcNotificationDAO());
    }

    public BudgetService(BudgetDAO budgetDAO, SpendingPlanDAO spendingPlanDAO, ExpenseDAO expenseDAO,
                         AdviceDAO adviceDAO, NotificationDAO notificationDAO) {
        this.budgetDAO = budgetDAO;
        this.spendingPlanDAO = spendingPlanDAO;
        this.expenseDAO = expenseDAO;
        this.adviceDAO = adviceDAO;
        this.notificationDAO = notificationDAO;
    }

    /**
     * Accepts an AI Smart Spending Plan and copies all its plan items into category budgets
     * with source = 'AI_PLAN' inside ONE single JDBC transaction.
     * Also updates plan status and marks any associated advice as ACCEPTED.
     */
    public boolean acceptPlanIntoBudgets(long userId, long planId) {
        try (Connection conn = DBConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);

                boolean success = acceptPlanIntoBudgets(conn, userId, planId);
                conn.commit();
                return success;
            } catch (Exception e) {
                DBConnection.rollbackQuietly(conn);
                throw new DataAccessException("Failed to accept plan into budgets: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database transaction error: " + e.getMessage(), e);
        }
    }

    /**
     * Accepts plan within an existing transactional connection.
     */
    public boolean acceptPlanIntoBudgets(Connection conn, long userId, long planId) {
        Optional<SpendingPlan> planOpt = spendingPlanDAO.findById(conn, planId);
        if (planOpt.isEmpty()) {
            throw new IllegalArgumentException("Spending plan not found: " + planId);
        }

        SpendingPlan plan = planOpt.get();
        if (plan.getUserId() != userId) {
            throw new SecurityException("Unauthorized attempt to accept another user's plan");
        }

        String month = plan.getPlanMonth();
        List<PlanItem> items = plan.getItems();

        // Copy each plan item into budgets table with source = AI_PLAN
        for (PlanItem item : items) {
            if (item.getCategoryId() == null || item.getAllocatedAmount() == null) {
                continue;
            }

            Budget budget = new Budget();
            budget.setUserId(userId);
            budget.setCategoryId(item.getCategoryId());
            budget.setBudgetMonth(month);
            budget.setBudgetLimit(item.getAllocatedAmount());
            budget.setAlertThreshold(new BigDecimal("80.00"));
            budget.setSource("AI_PLAN");
            budget.setCreatedAt(LocalDateTime.now());

            budgetDAO.upsert(conn, budget);
        }

        // Update plan status to ACTIVE
        plan.setStatus(SpendingPlan.Status.ACTIVE);
        spendingPlanDAO.update(conn, plan);

        // Update any associated advice for this plan to ACCEPTED
        List<Advice> adviceList = adviceDAO.findByPlanId(conn, planId);
        for (Advice a : adviceList) {
            adviceDAO.updateStatus(conn, a.getId(), Advice.Status.ACCEPTED);
        }

        // Add confirmation notification
        Notification notif = new Notification(
                null,
                userId,
                "Plan Accepted as Budget",
                String.format("AI Spending Plan for %s successfully copied into category budgets (source: AI_PLAN).", month),
                Notification.Type.INFO,
                false,
                LocalDateTime.now()
        );
        notificationDAO.create(conn, notif);

        LOGGER.info(String.format("Successfully accepted plan %d into budgets for user %d for month %s", planId, userId, month));
        return true;
    }

    /**
     * Creates or updates a manual category budget (source = 'MANUAL').
     */
    public long createOrUpdateManualBudget(long userId, long categoryId, String month,
                                           BigDecimal limit, BigDecimal alertThreshold) {
        Budget budget = new Budget(
                null,
                userId,
                categoryId,
                month,
                limit,
                (alertThreshold != null) ? alertThreshold : new BigDecimal("80.00"),
                "MANUAL",
                LocalDateTime.now()
        );
        return budgetDAO.upsert(budget);
    }

    /**
     * Calculates budget utilisation details for a user in a given month.
     */
    public List<CategoryBudgetDetail> getBudgetUtilisationDetails(long userId, String month) {
        List<Budget> budgets = budgetDAO.findByUserAndMonth(userId, month);
        List<CategoryBudgetDetail> details = new ArrayList<>();

        for (Budget b : budgets) {
            List<Expense> expenses = expenseDAO.findByMonthAndCategory(userId, month, b.getCategoryId(), 1000, 0);
            BigDecimal spent = expenses.stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BudgetUtilisation utilisation = new BudgetUtilisation(spent, b.getBudgetLimit());
            details.add(new CategoryBudgetDetail(b, spent, utilisation));
        }

        return details;
    }

    public static class CategoryBudgetDetail {
        private final Budget budget;
        private final BigDecimal spent;
        private final BudgetUtilisation utilisation;

        public CategoryBudgetDetail(Budget budget, BigDecimal spent, BudgetUtilisation utilisation) {
            this.budget = budget;
            this.spent = spent;
            this.utilisation = utilisation;
        }

        public Budget getBudget() {
            return budget;
        }

        public BigDecimal getSpent() {
            return spent;
        }

        public BudgetUtilisation getUtilisation() {
            return utilisation;
        }
    }
}
