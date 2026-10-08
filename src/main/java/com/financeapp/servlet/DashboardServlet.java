package com.financeapp.servlet;

import com.financeapp.dao.*;
import com.financeapp.dao.jdbc.*;
import com.financeapp.model.*;
import com.financeapp.planner.PlanStatus;
import com.financeapp.planner.RecalculationResult;
import com.financeapp.service.PlannerService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller servlet dispatching authenticated users to their role-specific dashboards.
 */
@WebServlet(name = "DashboardServlet", urlPatterns = {"/dashboard", "/user/dashboard", "/admin/dashboard", "/advisor/dashboard"})
public class DashboardServlet extends HttpServlet {

    private final UserDAO userDAO = new JdbcUserDAO();
    private final SpendingPlanDAO spendingPlanDAO = new JdbcSpendingPlanDAO();
    private final ExpenseDAO expenseDAO = new JdbcExpenseDAO();
    private final FixedExpenseDAO fixedExpenseDAO = new JdbcFixedExpenseDAO();
    private final IncomeDAO incomeDAO = new JdbcIncomeDAO();
    private final CategoryDAO categoryDAO = new JdbcCategoryDAO();
    private final SettingsDAO settingsDAO = new JdbcSettingsDAO();
    private final AuditDAO auditDAO = new JdbcAuditDAO();
    private final NotificationDAO notificationDAO = new JdbcNotificationDAO();
    private final PlannerService plannerService = new PlannerService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        Long userId = (Long) session.getAttribute("userId");
        Object roleObj = session.getAttribute("role");
        Role role = (roleObj instanceof Role r) ? r : Role.fromString(String.valueOf(roleObj));

        if (role == Role.ADMIN) {
            handleAdminDashboard(req, resp);
        } else if (role == Role.ADVISOR) {
            handleAdvisorDashboard(req, resp, userId);
        } else {
            handleUserDashboard(req, resp, userId);
        }
    }

    private void handleUserDashboard(HttpServletRequest req, HttpServletResponse resp, Long userId)
            throws ServletException, IOException {
        String month = req.getParameter("month");
        if (month == null || !month.matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
            month = YearMonth.now().toString();
        }
        req.setAttribute("selectedMonth", month);

        YearMonth ym = YearMonth.parse(month);
        int daysInMonth = ym.lengthOfMonth();
        int dayOfMonth;
        if (ym.equals(YearMonth.now())) {
            dayOfMonth = LocalDate.now().getDayOfMonth();
        } else if (ym.isBefore(YearMonth.now())) {
            dayOfMonth = daysInMonth;
        } else {
            dayOfMonth = 1;
        }
        int daysLeft = Math.max(daysInMonth - dayOfMonth, 0);

        req.setAttribute("daysInMonth", daysInMonth);
        req.setAttribute("dayOfMonth", dayOfMonth);
        req.setAttribute("daysLeft", daysLeft);

        // 1. Load User Spending Plan
        Optional<SpendingPlan> planOpt = spendingPlanDAO.findByUserAndMonth(userId, month);
        SpendingPlan plan = planOpt.orElse(null);
        req.setAttribute("plan", plan);

        // 2. Load Categories
        List<Category> categories = categoryDAO.findAll();
        req.setAttribute("categories", categories);

        // 3. Load Expenses for selected month
        List<Expense> expenses = expenseDAO.findByMonthAndCategory(userId, month, null, 100, 0);
        req.setAttribute("expenses", expenses);

        // 4. Load Active Fixed Expenses & Income streams for setup form pre-population
        List<FixedExpense> fixedExpenses = fixedExpenseDAO.findByUserId(userId);
        req.setAttribute("fixedExpenses", fixedExpenses);
        BigDecimal totalFixed = fixedExpenses.stream()
                .filter(FixedExpense::isActive)
                .map(FixedExpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        req.setAttribute("totalFixed", totalFixed);

        List<Income> incomes = incomeDAO.findByUserAndMonth(userId, month);
        if (incomes.isEmpty()) {
            incomes = incomeDAO.findByUserId(userId);
        }
        req.setAttribute("incomes", incomes);
        BigDecimal totalIncome = incomes.stream()
                .map(Income::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        req.setAttribute("totalIncome", totalIncome);

        // 5. Unread Notifications
        List<Notification> unreadNotifs = notificationDAO.findUnreadByUserId(userId);
        req.setAttribute("notifications", unreadNotifs);

        // 6. Category Spending Aggregations & Recalculation
        Map<String, BigDecimal> spentPerCategory = plannerService.calculateSpentPerCategory(expenses);
        req.setAttribute("spentPerCategory", spentPerCategory);

        BigDecimal totalSpent = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        req.setAttribute("totalSpent", totalSpent);

        // Overall & per-category recalculations
        List<RecalculationResult> recalculations = new ArrayList<>();
        PlanStatus overallStatus = PlanStatus.ON_TRACK;
        BigDecimal dailyLimit = BigDecimal.ZERO;
        BigDecimal weeklyLimit = BigDecimal.ZERO;

        if (plan != null) {
            dailyLimit = plannerService.calculateDailyLimit(plan.getDisposableIncome(), daysInMonth);
            weeklyLimit = plannerService.calculateWeeklyLimit(dailyLimit);

            // Compute overall spending trajectory
            RecalculationResult overallRecalc = plannerService.recalculate(
                    "Total Disposable", plan.getDisposableIncome(), totalSpent, dayOfMonth, daysInMonth
            );
            overallStatus = overallRecalc.getStatus();

            // Category item recalculations
            for (PlanItem item : plan.getItems()) {
                BigDecimal catSpent = spentPerCategory.getOrDefault(item.getCategoryName(), BigDecimal.ZERO);
                RecalculationResult catResult = plannerService.recalculate(
                        item.getCategoryName(), item.getAllocatedAmount(), catSpent, dayOfMonth, daysInMonth
                );
                recalculations.add(catResult);
            }
        }

        req.setAttribute("dailyLimit", dailyLimit);
        req.setAttribute("weeklyLimit", weeklyLimit);
        req.setAttribute("overallStatus", overallStatus);
        req.setAttribute("recalculations", recalculations);

        // 7. Chart.js Data Preparation
        prepareChartData(req, categories, spentPerCategory);

        req.getRequestDispatcher("/WEB-INF/views/user/dashboard.jsp").forward(req, resp);
    }

    private void handleAdminDashboard(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        List<User> users = userDAO.findAll();
        req.setAttribute("users", users);

        Map<String, String> settings = settingsDAO.getAll();
        req.setAttribute("settings", settings);

        List<AuditLog> auditLogs = auditDAO.findRecent(30);
        req.setAttribute("auditLogs", auditLogs);

        req.getRequestDispatcher("/WEB-INF/views/admin/dashboard.jsp").forward(req, resp);
    }

    private void handleAdvisorDashboard(HttpServletRequest req, HttpServletResponse resp, Long advisorId)
            throws ServletException, IOException {
        List<User> clients = userDAO.findByAdvisorId(advisorId);
        String currentMonth = YearMonth.now().toString();

        Map<Long, SpendingPlan> clientPlans = new HashMap<>();
        Map<Long, PlanStatus> clientStatuses = new HashMap<>();

        for (User client : clients) {
            Optional<SpendingPlan> planOpt = spendingPlanDAO.findByUserAndMonth(client.getId(), currentMonth);
            if (planOpt.isPresent()) {
                SpendingPlan p = planOpt.get();
                clientPlans.put(client.getId(), p);

                List<Expense> expenses = expenseDAO.findByMonthAndCategory(client.getId(), currentMonth, null, 1000, 0);
                BigDecimal spent = expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                RecalculationResult r = plannerService.recalculate(
                        "Overview", p.getDisposableIncome(), spent, LocalDate.now().getDayOfMonth(), YearMonth.now().lengthOfMonth()
                );
                clientStatuses.put(client.getId(), r.getStatus());
            } else {
                clientStatuses.put(client.getId(), PlanStatus.ON_TRACK);
            }
        }

        req.setAttribute("clients", clients);
        req.setAttribute("clientPlans", clientPlans);
        req.setAttribute("clientStatuses", clientStatuses);
        req.setAttribute("currentMonth", currentMonth);

        req.getRequestDispatcher("/WEB-INF/views/advisor/dashboard.jsp").forward(req, resp);
    }

    private void prepareChartData(HttpServletRequest req, List<Category> categories, Map<String, BigDecimal> spentMap) {
        List<String> chartLabels = new ArrayList<>();
        List<BigDecimal> chartValues = new ArrayList<>();

        for (Category c : categories) {
            BigDecimal amt = spentMap.getOrDefault(c.getName(), BigDecimal.ZERO);
            if (amt.compareTo(BigDecimal.ZERO) > 0) {
                chartLabels.add(c.getName());
                chartValues.add(amt);
            }
        }

        // Fallback if no expenses recorded yet
        if (chartLabels.isEmpty()) {
            chartLabels.add("No Expenses Recorded");
            chartValues.add(BigDecimal.ONE);
        }

        String labelsJson = "[" + chartLabels.stream().map(l -> "\"" + l + "\"").collect(Collectors.joining(",")) + "]";
        String dataJson = "[" + chartValues.stream().map(BigDecimal::toString).collect(Collectors.joining(",")) + "]";

        req.setAttribute("chartLabelsJson", labelsJson);
        req.setAttribute("chartDataJson", dataJson);
    }
}
