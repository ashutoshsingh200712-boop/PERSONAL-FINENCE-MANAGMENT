package com.financeapp.task;

import com.financeapp.dao.ExpenseDAO;
import com.financeapp.dao.NotificationDAO;
import com.financeapp.dao.SpendingPlanDAO;
import com.financeapp.dao.jdbc.JdbcExpenseDAO;
import com.financeapp.dao.jdbc.JdbcNotificationDAO;
import com.financeapp.dao.jdbc.JdbcSpendingPlanDAO;
import com.financeapp.model.Expense;
import com.financeapp.model.Notification;
import com.financeapp.model.SpendingPlan;
import com.financeapp.planner.PlanStatus;
import com.financeapp.planner.RecalculationResult;
import com.financeapp.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background monitoring task that re-evaluates every active spending plan.
 * Detects plan status transitions and inserts notifications when status changes
 * to OVER or when a spending surplus appears.
 */
public class PlanMonitorTask implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(PlanMonitorTask.class.getName());

    private final AtomicInteger runCounter = new AtomicInteger(0);
    private final ConcurrentHashMap<Long, PlanStatus> lastKnownStatus = new ConcurrentHashMap<>();

    private final SpendingPlanDAO spendingPlanDAO;
    private final ExpenseDAO expenseDAO;
    private final NotificationDAO notificationDAO;

    public PlanMonitorTask() {
        this(new JdbcSpendingPlanDAO(), new JdbcExpenseDAO(), new JdbcNotificationDAO());
    }

    public PlanMonitorTask(SpendingPlanDAO spendingPlanDAO, ExpenseDAO expenseDAO, NotificationDAO notificationDAO) {
        this.spendingPlanDAO = spendingPlanDAO;
        this.expenseDAO = expenseDAO;
        this.notificationDAO = notificationDAO;
    }

    @Override
    public void run() {
        int runNum = runCounter.incrementAndGet();
        LOGGER.fine("Starting PlanMonitorTask execution #" + runNum);

        try (Connection conn = DBConnection.getConnection()) {
            executeWithConnection(conn);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error executing PlanMonitorTask on run #" + runNum + ": " + e.getMessage(), e);
        }
    }

    /**
     * Executes the monitoring cycle over all active spending plans using the provided connection.
     */
    public void executeWithConnection(Connection conn) {
        List<SpendingPlan> activePlans = spendingPlanDAO.findAllActive(conn);
        if (activePlans.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();

        for (SpendingPlan plan : activePlans) {
            try {
                evaluatePlan(conn, plan, today);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to evaluate spending plan ID " + plan.getId() + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * Evaluates a single spending plan and records status changes.
     */
    public void evaluatePlan(Connection conn, SpendingPlan plan, LocalDate today) {
        Long planId = plan.getId();
        Long userId = plan.getUserId();
        String month = plan.getPlanMonth();

        YearMonth ym = YearMonth.parse(month);
        int daysInMonth = ym.lengthOfMonth();
        int dayOfMonth;
        if (ym.equals(YearMonth.from(today))) {
            dayOfMonth = today.getDayOfMonth();
        } else if (ym.isBefore(YearMonth.from(today))) {
            dayOfMonth = daysInMonth;
        } else {
            dayOfMonth = 1;
        }

        // Fetch user expenses for that month
        List<Expense> expenses = expenseDAO.findByMonthAndCategory(conn, userId, month, null, 10000, 0);
        BigDecimal totalSpent = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Recalculate status and pace metrics
        RecalculationResult recalc = RecalculationResult.compute(
                "Total Plan", plan.getDisposableIncome(), totalSpent, dayOfMonth, daysInMonth
        );
        PlanStatus currentStatus = recalc.getStatus();

        // Check against last-known status in concurrent map
        PlanStatus oldStatus = lastKnownStatus.get(planId);
        lastKnownStatus.put(planId, currentStatus);

        // 1. Status changes to OVER (or becomes OVER)
        if (currentStatus == PlanStatus.OVER && (oldStatus == null || oldStatus != PlanStatus.OVER)) {
            String title = "Plan Alert: Overspending Detected";
            String msg = String.format(
                    "Your spending plan for %s has exceeded target pace (Status: OVER). Current pace ratio: %s. Adjusted daily limit is $%s for remaining %d days.",
                    month, recalc.getRatio(), recalc.getAdjustedDailyLimit(), recalc.getDaysLeft()
            );
            Notification notif = new Notification(
                    null, userId, title, msg, Notification.Type.ALERT, false, LocalDateTime.now()
            );
            notificationDAO.create(conn, notif);
            LOGGER.info("Inserted OVER status notification for user " + userId + " (plan ID: " + planId + ")");
        }
        // 2. Surplus appears (status changes to UNDER or surplus detected when not previously UNDER)
        else if (currentStatus == PlanStatus.UNDER && (oldStatus == null || oldStatus != PlanStatus.UNDER)) {
            String title = "Plan Update: Spending Surplus Available";
            String msg = String.format(
                    "Great news! A spending surplus of $%s has appeared in your %s plan. Adjusted daily allowance is $%s for remaining %d days.",
                    recalc.getSurplus(), month, recalc.getAdjustedDailyLimit(), recalc.getDaysLeft()
            );
            Notification notif = new Notification(
                    null, userId, title, msg, Notification.Type.INFO, false, LocalDateTime.now()
            );
            notificationDAO.create(conn, notif);
            LOGGER.info("Inserted SURPLUS status notification for user " + userId + " (plan ID: " + planId + ")");
        }
    }

    public int getRunCount() {
        return runCounter.get();
    }

    public AtomicInteger getRunCounter() {
        return runCounter;
    }

    public ConcurrentHashMap<Long, PlanStatus> getLastKnownStatus() {
        return lastKnownStatus;
    }
}
