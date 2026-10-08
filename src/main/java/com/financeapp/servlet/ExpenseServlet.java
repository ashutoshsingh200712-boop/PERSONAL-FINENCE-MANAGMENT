package com.financeapp.servlet;

import com.financeapp.dao.ExpenseDAO;
import com.financeapp.dao.NotificationDAO;
import com.financeapp.dao.jdbc.JdbcExpenseDAO;
import com.financeapp.dao.jdbc.JdbcNotificationDAO;
import com.financeapp.model.Expense;
import com.financeapp.model.Notification;
import com.financeapp.planner.PlanStatus;
import com.financeapp.planner.RecalculationResult;
import com.financeapp.service.PlannerService;
import com.financeapp.util.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet handling expense creation, modification, and deletion.
 * Insertion executes expense creation, plan recalculation, and notification persistence
 * atomically inside ONE JDBC transaction.
 */
@WebServlet(name = "ExpenseServlet", urlPatterns = {"/expenses"})
public class ExpenseServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ExpenseServlet.class.getName());

    private final ExpenseDAO expenseDAO = new JdbcExpenseDAO();
    private final NotificationDAO notificationDAO = new JdbcNotificationDAO();
    private final PlannerService plannerService = new PlannerService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        // Ownership: strictly from session, never from request parameter
        Long sessionUserId = (Long) session.getAttribute("userId");
        String action = req.getParameter("action");
        if (action == null) {
            action = "add";
        }

        String targetMonth = req.getParameter("month");
        if (targetMonth == null || !targetMonth.matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
            targetMonth = YearMonth.now().toString();
        }

        try {
            switch (action) {
                case "delete" -> handleDeleteExpense(req, resp, sessionUserId, targetMonth);
                case "edit" -> handleEditExpense(req, resp, sessionUserId, targetMonth);
                default -> handleAddExpense(req, resp, sessionUserId, targetMonth);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing expense action [" + action + "]: " + e.getMessage(), e);
            session.setAttribute("flashMessage", "Failed to process expense: " + e.getMessage());
            session.setAttribute("flashType", "danger");
            resp.sendRedirect(req.getContextPath() + "/dashboard?month=" + targetMonth);
        }
    }

    /**
     * Executes expense insertion, plan recalculation, and notification creation
     * inside ONE single JDBC transaction.
     */
    private void handleAddExpense(HttpServletRequest req, HttpServletResponse resp, Long userId, String month)
            throws IOException {
        String categoryIdStr = req.getParameter("categoryId");
        String amountStr = req.getParameter("amount");
        String dateStr = req.getParameter("expenseDate");
        String description = req.getParameter("description");
        String paymentMethod = req.getParameter("paymentMethod");

        if (categoryIdStr == null || amountStr == null || dateStr == null) {
            throw new IllegalArgumentException("Category, amount, and date are required.");
        }

        long categoryId = Long.parseLong(categoryIdStr);
        BigDecimal amount = new BigDecimal(amountStr);
        LocalDate expenseDate = LocalDate.parse(dateStr);

        YearMonth ym = YearMonth.from(expenseDate);
        month = ym.toString();
        int daysInMonth = ym.lengthOfMonth();
        int dayOfMonth = expenseDate.getDayOfMonth();

        Expense expense = new Expense();
        expense.setUserId(userId);
        expense.setCategoryId(categoryId);
        expense.setAmount(amount);
        expense.setDate(expenseDate);
        expense.setDescription(description);
        expense.setPaymentMethod(paymentMethod != null ? paymentMethod : "CASH");
        expense.setCreatedAt(LocalDateTime.now());

        // ONE JDBC Transaction: setAutoCommit(false), commit, rollback in catch, restore in finally
        try (Connection conn = DBConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);

                // 1. Insert the expense
                long expenseId = expenseDAO.create(conn, expense);
                LOGGER.fine("Inserted expense ID: " + expenseId + " for user: " + userId);

                // 2. Recalculate the plan
                RecalculationResult recalc = plannerService.recalculate(
                        conn, userId, month, categoryId, dayOfMonth, daysInMonth
                );

                // 3. Insert notification
                String notifTitle = "Expense Logged: " + recalc.getCategoryName();
                String notifMsg = String.format(
                        "Logged $%s in %s. Pace Status: %s. New adjusted daily limit is $%s for remaining %d days.",
                        amount, recalc.getCategoryName(), recalc.getStatus(), recalc.getAdjustedDailyLimit(), recalc.getDaysLeft()
                );
                Notification.Type type = (recalc.getStatus() == PlanStatus.OVER)
                        ? Notification.Type.WARNING
                        : Notification.Type.INFO;

                Notification notification = new Notification(
                        null, userId, notifTitle, notifMsg, type, false, LocalDateTime.now()
                );
                notificationDAO.create(conn, notification);

                // Commit transaction
                conn.commit();
                LOGGER.info(String.format("Committed transaction for user %d: expense %d, recalc %s",
                        userId, expenseId, recalc.getStatus()));

            } catch (Exception e) {
                DBConnection.rollbackQuietly(conn);
                LOGGER.log(Level.SEVERE, "Transaction rolled back during addExpense: " + e.getMessage(), e);
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error executing expense transaction: " + e.getMessage(), e);
        }

        // Post-Redirect-Get pattern with flash message
        HttpSession session = req.getSession();
        session.setAttribute("flashMessage", "Expense added successfully!");
        session.setAttribute("flashType", "success");
        resp.sendRedirect(req.getContextPath() + "/dashboard?month=" + month);
    }

    private void handleEditExpense(HttpServletRequest req, HttpServletResponse resp, Long userId, String month)
            throws IOException {
        long expenseId = Long.parseLong(req.getParameter("expenseId"));

        // Verify ownership
        Optional<Expense> existingOpt = expenseDAO.findById(expenseId);
        if (existingOpt.isEmpty() || !existingOpt.get().getUserId().equals(userId)) {
            throw new SecurityException("Unauthorized attempt to edit expense not owned by current user.");
        }

        Expense expense = existingOpt.get();
        expense.setCategoryId(Long.parseLong(req.getParameter("categoryId")));
        expense.setAmount(new BigDecimal(req.getParameter("amount")));
        expense.setDate(LocalDate.parse(req.getParameter("expenseDate")));
        expense.setDescription(req.getParameter("description"));
        expense.setPaymentMethod(req.getParameter("paymentMethod"));

        expenseDAO.update(expense);

        HttpSession session = req.getSession();
        session.setAttribute("flashMessage", "Expense updated successfully!");
        session.setAttribute("flashType", "success");
        resp.sendRedirect(req.getContextPath() + "/dashboard?month=" + month);
    }

    private void handleDeleteExpense(HttpServletRequest req, HttpServletResponse resp, Long userId, String month)
            throws IOException {
        long expenseId = Long.parseLong(req.getParameter("expenseId"));

        // Verify ownership
        Optional<Expense> existingOpt = expenseDAO.findById(expenseId);
        if (existingOpt.isEmpty() || !existingOpt.get().getUserId().equals(userId)) {
            throw new SecurityException("Unauthorized attempt to delete expense not owned by current user.");
        }

        expenseDAO.delete(expenseId);

        HttpSession session = req.getSession();
        session.setAttribute("flashMessage", "Expense deleted successfully.");
        session.setAttribute("flashType", "info");
        resp.sendRedirect(req.getContextPath() + "/dashboard?month=" + month);
    }
}
