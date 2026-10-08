package com.financeapp.servlet;

import com.financeapp.dao.FixedExpenseDAO;
import com.financeapp.dao.IncomeDAO;
import com.financeapp.dao.jdbc.JdbcFixedExpenseDAO;
import com.financeapp.dao.jdbc.JdbcIncomeDAO;
import com.financeapp.exception.InsufficientFundsException;
import com.financeapp.model.FixedExpense;
import com.financeapp.model.Income;
import com.financeapp.planner.AllocationStrategy;
import com.financeapp.planner.DefaultStrategy;
import com.financeapp.planner.SavingsFocusedStrategy;
import com.financeapp.service.PlannerService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.logging.Logger;

/**
 * Servlet handling initial configuration and regeneration of monthly AI Spending Plans.
 */
@WebServlet(name = "PlanSetupServlet", urlPatterns = {"/plan/setup"})
public class PlanSetupServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(PlanSetupServlet.class.getName());

    private final PlannerService plannerService = new PlannerService();
    private final IncomeDAO incomeDAO = new JdbcIncomeDAO();
    private final FixedExpenseDAO fixedExpenseDAO = new JdbcFixedExpenseDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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

        try {
            String incomeStr = req.getParameter("income");
            String fixedStr = req.getParameter("fixedExpenses");
            String savingsGoalStr = req.getParameter("savingsGoal");
            String strategyType = req.getParameter("strategy");

            BigDecimal income = (incomeStr != null && !incomeStr.trim().isEmpty())
                    ? new BigDecimal(incomeStr.trim()) : BigDecimal.ZERO;
            BigDecimal fixed = (fixedStr != null && !fixedStr.trim().isEmpty())
                    ? new BigDecimal(fixedStr.trim()) : BigDecimal.ZERO;
            BigDecimal savingsGoal = (savingsGoalStr != null && !savingsGoalStr.trim().isEmpty())
                    ? new BigDecimal(savingsGoalStr.trim()) : null;

            // Upsert or insert sample recurring income/fixed if provided and list is currently empty
            if (income.compareTo(BigDecimal.ZERO) > 0 && incomeDAO.findByUserAndMonth(userId, month).isEmpty()) {
                Income inc = new Income(null, userId, "Monthly Paycheck", income,
                        Income.Frequency.MONTHLY, LocalDate.now(), "Configured via Plan Setup", LocalDateTime.now());
                incomeDAO.create(inc);
            }

            if (fixed.compareTo(BigDecimal.ZERO) > 0 && fixedExpenseDAO.findByUserId(userId).isEmpty()) {
                FixedExpense fe = new FixedExpense(null, userId, 5L, "Standard Living Expenses",
                        fixed, 1, true, "Configured via Plan Setup", LocalDateTime.now());
                fixedExpenseDAO.create(fe);
            }

            AllocationStrategy strategy = "savings".equalsIgnoreCase(strategyType)
                    ? new SavingsFocusedStrategy()
                    : new DefaultStrategy();

            long planId = plannerService.createPlan(userId, month, income, fixed, savingsGoal, strategy);
            LOGGER.info("Successfully generated spending plan " + planId + " for user " + userId + " for " + month);

            session.setAttribute("flashMessage", "AI Smart Spending Plan generated successfully!");
            session.setAttribute("flashType", "success");
        } catch (InsufficientFundsException e) {
            session.setAttribute("flashMessage", "Plan creation failed: " + e.getMessage());
            session.setAttribute("flashType", "danger");
        } catch (Exception e) {
            LOGGER.severe("Error creating plan: " + e.getMessage());
            session.setAttribute("flashMessage", "Error generating plan: " + e.getMessage());
            session.setAttribute("flashType", "danger");
        }

        resp.sendRedirect(req.getContextPath() + "/dashboard?month=" + month);
    }
}
