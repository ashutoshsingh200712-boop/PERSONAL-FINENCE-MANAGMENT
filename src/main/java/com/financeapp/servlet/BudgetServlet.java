package com.financeapp.servlet;

import com.financeapp.dao.CategoryDAO;
import com.financeapp.dao.jdbc.JdbcCategoryDAO;
import com.financeapp.model.Category;
import com.financeapp.service.BudgetService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet handling manual budget management and accepting AI Spending Plans into budgets.
 */
@WebServlet(name = "BudgetServlet", urlPatterns = {"/budgets", "/budgets/accept-plan"})
public class BudgetServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(BudgetServlet.class.getName());

    private final BudgetService budgetService;
    private final CategoryDAO categoryDAO;

    public BudgetServlet() {
        this(new BudgetService(), new JdbcCategoryDAO());
    }

    public BudgetServlet(BudgetService budgetService, CategoryDAO categoryDAO) {
        this.budgetService = budgetService;
        this.categoryDAO = categoryDAO;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        Long userId = (Long) session.getAttribute("userId");
        String month = req.getParameter("month");
        if (month == null || !month.matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
            month = YearMonth.now().toString();
        }

        List<BudgetService.CategoryBudgetDetail> budgetDetails = budgetService.getBudgetUtilisationDetails(userId, month);
        List<Category> categories = categoryDAO.findAll();

        req.setAttribute("selectedMonth", month);
        req.setAttribute("budgetDetails", budgetDetails);
        req.setAttribute("categories", categories);

        req.getRequestDispatcher("/WEB-INF/views/user/budgets.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        Long userId = (Long) session.getAttribute("userId");
        String path = req.getServletPath();
        String action = req.getParameter("action");
        String month = req.getParameter("month");
        if (month == null || !month.matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
            month = YearMonth.now().toString();
        }

        try {
            if ("/budgets/accept-plan".equals(path) || "accept_plan".equals(action)) {
                // Accepting a plan copies it into budgets (source = AI_PLAN) in ONE transaction
                String planIdStr = req.getParameter("planId");
                if (planIdStr == null || planIdStr.trim().isEmpty()) {
                    throw new IllegalArgumentException("Plan ID is required to accept a spending plan.");
                }
                long planId = Long.parseLong(planIdStr.trim());
                budgetService.acceptPlanIntoBudgets(userId, planId);

                session.setAttribute("flashMessage", "AI Spending Plan successfully accepted and converted into active category budgets (Source: AI_PLAN)!");
                session.setAttribute("flashType", "success");
            } else {
                // Manual budget creation
                String categoryIdStr = req.getParameter("categoryId");
                String limitStr = req.getParameter("budgetLimit");
                String alertThresholdStr = req.getParameter("alertThreshold");

                if (categoryIdStr == null || limitStr == null) {
                    throw new IllegalArgumentException("Category and budget limit are required.");
                }

                long categoryId = Long.parseLong(categoryIdStr.trim());
                BigDecimal limit = new BigDecimal(limitStr.trim());
                BigDecimal alertThreshold = (alertThresholdStr != null && !alertThresholdStr.trim().isEmpty())
                        ? new BigDecimal(alertThresholdStr.trim())
                        : new BigDecimal("80.00");

                budgetService.createOrUpdateManualBudget(userId, categoryId, month, limit, alertThreshold);

                session.setAttribute("flashMessage", "Manual category budget successfully saved (Source: MANUAL)!");
                session.setAttribute("flashType", "success");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error saving budget: " + e.getMessage(), e);
            session.setAttribute("flashMessage", "Failed to save budget: " + e.getMessage());
            session.setAttribute("flashType", "danger");
        }

        String redirect = req.getParameter("redirect");
        if ("dashboard".equalsIgnoreCase(redirect)) {
            resp.sendRedirect(req.getContextPath() + "/dashboard?month=" + month);
        } else {
            resp.sendRedirect(req.getContextPath() + "/budgets?month=" + month);
        }
    }
}
