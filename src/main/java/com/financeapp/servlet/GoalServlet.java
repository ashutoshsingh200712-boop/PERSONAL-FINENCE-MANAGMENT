package com.financeapp.servlet;

import com.financeapp.model.Goal;
import com.financeapp.model.GoalFeasibilityResult;
import com.financeapp.service.GoalService;
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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet handling user savings goals, progress tracking, and feasibility evaluation.
 */
@WebServlet(name = "GoalServlet", urlPatterns = {"/goals"})
public class GoalServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(GoalServlet.class.getName());

    private final GoalService goalService;

    public GoalServlet() {
        this(new GoalService());
    }

    public GoalServlet(GoalService goalService) {
        this.goalService = goalService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        Long userId = (Long) session.getAttribute("userId");
        List<GoalFeasibilityResult> goalResults = goalService.evaluateUserGoals(userId);
        BigDecimal averageMonthlySurplus = goalService.calculateAverageMonthlySurplus(userId);

        req.setAttribute("goalResults", goalResults);
        req.setAttribute("averageMonthlySurplus", averageMonthlySurplus);

        req.getRequestDispatcher("/WEB-INF/views/user/goals.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        Long userId = (Long) session.getAttribute("userId");
        String action = req.getParameter("action");
        if (action == null) {
            action = "create";
        }

        try {
            switch (action) {
                case "delete" -> {
                    long goalId = Long.parseLong(req.getParameter("goalId"));
                    goalService.findById(goalId).ifPresent(g -> {
                        if (g.getUserId().equals(userId)) {
                            goalService.deleteGoal(goalId);
                        }
                    });
                    session.setAttribute("flashMessage", "Goal deleted successfully.");
                    session.setAttribute("flashType", "success");
                }
                case "update_progress" -> {
                    long goalId = Long.parseLong(req.getParameter("goalId"));
                    BigDecimal addAmount = new BigDecimal(req.getParameter("amount"));
                    goalService.findById(goalId).ifPresent(g -> {
                        if (g.getUserId().equals(userId)) {
                            BigDecimal current = (g.getCurrentAmount() != null) ? g.getCurrentAmount() : BigDecimal.ZERO;
                            g.setCurrentAmount(current.add(addAmount));
                            goalService.updateGoal(g);
                        }
                    });
                    session.setAttribute("flashMessage", "Goal progress updated successfully!");
                    session.setAttribute("flashType", "success");
                }
                default -> { // create
                    String name = req.getParameter("name");
                    BigDecimal target = new BigDecimal(req.getParameter("targetAmount"));
                    String currentStr = req.getParameter("currentAmount");
                    BigDecimal current = (currentStr != null && !currentStr.trim().isEmpty())
                            ? new BigDecimal(currentStr.trim()) : BigDecimal.ZERO;
                    LocalDate targetDate = LocalDate.parse(req.getParameter("targetDate"));

                    Goal goal = new Goal(null, userId, name, target, current, targetDate, LocalDateTime.now());
                    goalService.createGoal(goal);

                    session.setAttribute("flashMessage", "New savings goal '" + name + "' created successfully!");
                    session.setAttribute("flashType", "success");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in GoalServlet: " + e.getMessage(), e);
            session.setAttribute("flashMessage", "Failed to process goal: " + e.getMessage());
            session.setAttribute("flashType", "danger");
        }

        resp.sendRedirect(req.getContextPath() + "/goals");
    }
}
