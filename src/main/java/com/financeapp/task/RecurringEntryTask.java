package com.financeapp.task;

import com.financeapp.dao.ExpenseDAO;
import com.financeapp.dao.FixedExpenseDAO;
import com.financeapp.dao.NotificationDAO;
import com.financeapp.dao.jdbc.JdbcExpenseDAO;
import com.financeapp.dao.jdbc.JdbcFixedExpenseDAO;
import com.financeapp.dao.jdbc.JdbcNotificationDAO;
import com.financeapp.model.Expense;
import com.financeapp.model.FixedExpense;
import com.financeapp.model.Notification;
import com.financeapp.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background task that automatically logs fixed recurring commitments (e.g. rent, bills)
 * on their scheduled monthly due day.
 */
public class RecurringEntryTask implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(RecurringEntryTask.class.getName());

    private final AtomicInteger runCounter = new AtomicInteger(0);

    private final FixedExpenseDAO fixedExpenseDAO;
    private final ExpenseDAO expenseDAO;
    private final NotificationDAO notificationDAO;

    public RecurringEntryTask() {
        this(new JdbcFixedExpenseDAO(), new JdbcExpenseDAO(), new JdbcNotificationDAO());
    }

    public RecurringEntryTask(FixedExpenseDAO fixedExpenseDAO, ExpenseDAO expenseDAO, NotificationDAO notificationDAO) {
        this.fixedExpenseDAO = fixedExpenseDAO;
        this.expenseDAO = expenseDAO;
        this.notificationDAO = notificationDAO;
    }

    @Override
    public void run() {
        int runNum = runCounter.incrementAndGet();
        LOGGER.fine("Starting RecurringEntryTask execution #" + runNum);

        try (Connection conn = DBConnection.getConnection()) {
            executeForDate(conn, LocalDate.now());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error executing RecurringEntryTask on run #" + runNum + ": " + e.getMessage(), e);
        }
    }

    /**
     * Identifies active fixed obligations due on the specified date and posts them as expenses.
     */
    public void executeForDate(Connection conn, LocalDate targetDate) {
        int dueDay = targetDate.getDayOfMonth();
        List<FixedExpense> dueList = fixedExpenseDAO.findActiveByDueDay(conn, dueDay);

        if (dueList.isEmpty()) {
            return;
        }

        for (FixedExpense fe : dueList) {
            try {
                String description = "Recurring: " + fe.getTitle();
                if (!isAlreadyPosted(conn, fe.getUserId(), targetDate, description)) {
                    Expense expense = new Expense();
                    expense.setUserId(fe.getUserId());
                    expense.setCategoryId(fe.getCategoryId());
                    expense.setAmount(fe.getAmount());
                    expense.setDate(targetDate);
                    expense.setDescription(description);
                    expense.setPaymentMethod("AUTO_DEBIT");
                    expense.setCreatedAt(LocalDateTime.now());

                    expenseDAO.create(conn, expense);

                    Notification notif = new Notification(
                            null,
                            fe.getUserId(),
                            "Recurring Expense Posted",
                            String.format("Fixed commitment '%s' of $%s was automatically logged on due day (%s).",
                                    fe.getTitle(), fe.getAmount(), targetDate),
                            Notification.Type.INFO,
                            false,
                            LocalDateTime.now()
                    );
                    notificationDAO.create(conn, notif);
                    LOGGER.info("Posted recurring expense '" + fe.getTitle() + "' for user " + fe.getUserId());
                } else {
                    LOGGER.fine("Recurring expense '" + fe.getTitle() + "' already posted for " + targetDate);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to post recurring fixed expense ID " + fe.getId() + ": " + e.getMessage(), e);
            }
        }
    }

    private boolean isAlreadyPosted(Connection conn, long userId, LocalDate date, String description) throws SQLException {
        String sql = "SELECT COUNT(*) FROM expenses WHERE user_id = ? AND expense_date = ? AND description = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setDate(2, Date.valueOf(date));
            stmt.setString(3, description);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public int getRunCount() {
        return runCounter.get();
    }

    public AtomicInteger getRunCounter() {
        return runCounter;
    }
}
