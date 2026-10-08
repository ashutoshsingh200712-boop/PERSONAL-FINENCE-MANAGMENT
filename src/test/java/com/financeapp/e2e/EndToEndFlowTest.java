package com.financeapp.e2e;

import com.financeapp.dao.*;
import com.financeapp.dao.jdbc.*;
import com.financeapp.model.*;
import com.financeapp.planner.DefaultStrategy;
import com.financeapp.planner.PlanStatus;
import com.financeapp.planner.RecalculationResult;
import com.financeapp.service.PlannerService;
import com.financeapp.util.DBConnection;
import com.financeapp.util.SecurityUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("End-to-End Flow & Security Integration Tests")
class EndToEndFlowTest {

    private static final String H2_URL = "jdbc:h2:mem:e2e_finance_test;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
    private static Connection masterConn;

    private static UserDAO userDAO;
    private static CategoryDAO categoryDAO;
    private static IncomeDAO incomeDAO;
    private static FixedExpenseDAO fixedExpenseDAO;
    private static ExpenseDAO expenseDAO;
    private static SpendingPlanDAO spendingPlanDAO;
    private static PlannerService plannerService;

    @BeforeAll
    static void setUp() throws Exception {
        masterConn = DriverManager.getConnection(H2_URL, "sa", "");
        DBConnection.setCustomConnectionSupplier(() -> {
            try {
                return DriverManager.getConnection(H2_URL, "sa", "");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        try (Statement stmt = masterConn.createStatement()) {
            stmt.execute("CREATE TABLE users (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL, email VARCHAR(150) NOT NULL UNIQUE, password_hash VARCHAR(255) NOT NULL, role VARCHAR(20) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE, failed_logins INT NOT NULL DEFAULT 0, locked_until TIMESTAMP NULL, advisor_id BIGINT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            stmt.execute("CREATE TABLE categories (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50) NOT NULL UNIQUE, description VARCHAR(255) NULL, is_system BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            stmt.execute("CREATE TABLE income (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, source VARCHAR(100) NOT NULL, amount DECIMAL(12,2) NOT NULL, frequency VARCHAR(20) NOT NULL, income_date DATE NOT NULL, notes TEXT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            stmt.execute("CREATE TABLE fixed_expenses (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, category_id BIGINT NOT NULL, title VARCHAR(100) NOT NULL, amount DECIMAL(12,2) NOT NULL, due_day INT NOT NULL DEFAULT 1, is_active BOOLEAN NOT NULL DEFAULT TRUE, notes TEXT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            stmt.execute("CREATE TABLE expenses (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, category_id BIGINT NOT NULL, amount DECIMAL(12,2) NOT NULL, expense_date DATE NOT NULL, description VARCHAR(255) NOT NULL, payment_method VARCHAR(50) NOT NULL DEFAULT 'CASH', created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            stmt.execute("CREATE TABLE spending_plans (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, plan_month CHAR(7) NOT NULL, total_income DECIMAL(12,2) NOT NULL DEFAULT 0.00, total_fixed_expenses DECIMAL(12,2) NOT NULL DEFAULT 0.00, disposable_income DECIMAL(12,2) NOT NULL DEFAULT 0.00, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            stmt.execute("CREATE TABLE plan_items (id BIGINT AUTO_INCREMENT PRIMARY KEY, plan_id BIGINT NOT NULL, category_id BIGINT NOT NULL, allocated_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00, weight_percentage DECIMAL(5,2) NOT NULL DEFAULT 0.00, notes VARCHAR(255) NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            stmt.execute("CREATE TABLE budgets (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, category_id BIGINT NOT NULL, budget_month CHAR(7) NOT NULL, budget_limit DECIMAL(12,2) NOT NULL, alert_threshold DECIMAL(5,2) NOT NULL DEFAULT 80.00, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            stmt.execute("CREATE TABLE notifications (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, title VARCHAR(150) NOT NULL, message TEXT NOT NULL, type VARCHAR(20) NOT NULL DEFAULT 'INFO', is_read BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            stmt.execute("CREATE TABLE system_settings (id BIGINT AUTO_INCREMENT PRIMARY KEY, setting_key VARCHAR(100) NOT NULL UNIQUE, setting_value VARCHAR(255) NOT NULL, description VARCHAR(255) NULL, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            stmt.execute("CREATE TABLE audit_logs (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NULL, action VARCHAR(100) NOT NULL, entity_type VARCHAR(50) NOT NULL, entity_id BIGINT NULL, details TEXT NULL, ip_address VARCHAR(45) NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");

            // Seed default categories matching sql/seed.sql
            stmt.execute("INSERT INTO categories (id, name, description) VALUES (1, 'Food', 'Groceries & Dining')");
            stmt.execute("INSERT INTO categories (id, name, description) VALUES (2, 'Savings', 'Long-term savings')");
            stmt.execute("INSERT INTO categories (id, name, description) VALUES (3, 'Emergency', 'Emergency fund')");
            stmt.execute("INSERT INTO categories (id, name, description) VALUES (4, 'Entertainment', 'Leisure & Subscriptions')");
            stmt.execute("INSERT INTO categories (id, name, description) VALUES (5, 'Other', 'General miscellaneous')");

            // Seed system weights
            stmt.execute("INSERT INTO system_settings (setting_key, setting_value) VALUES ('weight_Food', '40')");
            stmt.execute("INSERT INTO system_settings (setting_key, setting_value) VALUES ('weight_Savings', '20')");
            stmt.execute("INSERT INTO system_settings (setting_key, setting_value) VALUES ('weight_Emergency', '15')");
            stmt.execute("INSERT INTO system_settings (setting_key, setting_value) VALUES ('weight_Entertainment', '10')");
            stmt.execute("INSERT INTO system_settings (setting_key, setting_value) VALUES ('weight_Other', '15')");
        }

        userDAO = new JdbcUserDAO();
        categoryDAO = new JdbcCategoryDAO();
        incomeDAO = new JdbcIncomeDAO();
        fixedExpenseDAO = new JdbcFixedExpenseDAO();
        expenseDAO = new JdbcExpenseDAO();
        spendingPlanDAO = new JdbcSpendingPlanDAO();
        plannerService = new PlannerService();
    }

    @AfterAll
    static void tearDown() throws Exception {
        DBConnection.resetCustomConnectionSupplier();
        if (masterConn != null && !masterConn.isClosed()) {
            masterConn.close();
        }
    }

    @Test
    @DisplayName("Verification: Seed SQL BCrypt password hashes match plain text passwords")
    void testSeedPasswordsMatchBCrypt() {
        // Admin
        String adminPlain = "AdminPassword123!";
        String adminHash = "$2a$10$htdjpO4It8oNPfOGPnzXe.ebyQ2znSERLUqhfzw.4SqvHberNARiu";
        assertTrue(SecurityUtil.checkPassword(adminPlain, adminHash), "Admin password hash must match");

        // Advisor
        String advisorPlain = "AdvisorPassword123!";
        String advisorHash = "$2a$10$g/y/wX0NbW39d5uSXJOkvesj.DDddQsK9AmEfRCM5FETSSJXQYZDy";
        assertTrue(SecurityUtil.checkPassword(advisorPlain, advisorHash), "Advisor password hash must match");

        // Users
        String userPlain = "UserPassword123!";
        String userHash = "$2a$10$jdJCA.ME491j/egT51eX5.Cfncuer6h5rdOXoQARAczZb3F7Q/mBa";
        assertTrue(SecurityUtil.checkPassword(userPlain, userHash), "User password hash must match");
    }

    @Test
    @DisplayName("End-to-End Flow: Register -> Income 5000 & Fixed 2000 -> Food gets 1200 -> Log expenses -> Status OVER & Advice")
    void testCompleteUserJourney() {
        String currentMonth = YearMonth.now().toString();

        // 1. Registration
        String plainPassword = "SecurePassword123!";
        String hashedPassword = SecurityUtil.hashPassword(plainPassword);
        RegularUser user = new RegularUser(null, "E2E User", "e2e@example.com", hashedPassword, true, 0, null, null, LocalDateTime.now());
        long userId = userDAO.create(user);
        assertTrue(userId > 0, "User registration should succeed and generate ID");

        // Verify login authentication with BCrypt
        Optional<User> foundUser = userDAO.findByEmail("e2e@example.com");
        assertTrue(foundUser.isPresent());
        assertTrue(SecurityUtil.checkPassword(plainPassword, foundUser.get().getPasswordHash()));

        // 2. Set income to 5000
        Income income = new Income(null, userId, "Software Salary", new BigDecimal("5000.00"), Income.Frequency.MONTHLY, LocalDate.now(), "Monthly salary", LocalDateTime.now());
        long incomeId = incomeDAO.create(income);
        assertTrue(incomeId > 0);

        // Set fixed expenses to 2000
        FixedExpense rent = new FixedExpense();
        rent.setUserId(userId);
        rent.setCategoryId(5L);
        rent.setTitle("Apartment Rent");
        rent.setAmount(new BigDecimal("2000.00"));
        rent.setDueDay(1);
        rent.setActive(true);
        long fixedId = fixedExpenseDAO.create(rent);
        assertTrue(fixedId > 0);

        // 3. Generate plan: available = 5000 - 2000 = 3000
        long planId = plannerService.createPlan(userId, currentMonth, BigDecimal.ZERO, new DefaultStrategy());
        assertTrue(planId > 0);

        Optional<SpendingPlan> planOpt = spendingPlanDAO.findById(planId);
        assertTrue(planOpt.isPresent());
        SpendingPlan plan = planOpt.get();

        // Verify disposable income is exactly 3000.00
        assertEquals(new BigDecimal("3000.00"), plan.getDisposableIncome());

        // Verify Food receives 1200.00 (40% of 3000 available)
        Optional<PlanItem> foodItemOpt = plan.getItems().stream()
                .filter(item -> "Food".equalsIgnoreCase(item.getCategoryName()))
                .findFirst();
        assertTrue(foodItemOpt.isPresent(), "Food category plan item must be present");
        assertEquals(new BigDecimal("1200.00"), foodItemOpt.get().getAllocatedAmount(), "Food should receive exactly 40% of 3000 = 1200.00");

        // 4. Log expenses in Food ($1,400 spent in Food)
        Expense expense = new Expense();
        expense.setUserId(userId);
        expense.setCategoryId(foodItemOpt.get().getCategoryId());
        expense.setAmount(new BigDecimal("1400.00"));
        expense.setDate(LocalDate.now());
        expense.setDescription("Weekly Grocery Haul");
        expense.setPaymentMethod("DEBIT_CARD");
        long expenseId = expenseDAO.create(expense);
        assertTrue(expenseId > 0);

        // 5. Mid-month recalculation: status becomes OVER and explanatory advice appears
        int dayOfMonth = LocalDate.now().getDayOfMonth();
        int daysInMonth = YearMonth.now().lengthOfMonth();

        RecalculationResult recalc = plannerService.recalculate(
                "Food",
                foodItemOpt.get().getAllocatedAmount(), // $1,200.00
                new BigDecimal("1400.00"),             // $1,400.00 spent
                dayOfMonth,
                daysInMonth
        );

        // Assert status is OVER
        assertEquals(PlanStatus.OVER, recalc.getStatus(), "Plan status for Food must be OVER budget");

        // Assert explanatory advice appears
        assertFalse(recalc.getAdviceList().isEmpty(), "Explanatory advice list must not be empty");
        String advice = recalc.getAdviceList().get(0);
        assertTrue(advice.contains("OverspendingRule") || advice.contains("over budget") || advice.contains("exceeded"),
                "Advice must contain overspending explanation");
    }

    @Test
    @DisplayName("Security: SQL-Injection in inputs is neutralized by PreparedStatement")
    void testSqlInjectionSafety() {
        String injectionAttempt = "' OR '1'='1";
        Optional<User> userOpt = userDAO.findByEmail(injectionAttempt);
        assertTrue(userOpt.isEmpty(), "SQL injection string must not match any user");

        List<Expense> expenses = expenseDAO.findByUserId(999999L, 10, 0);
        assertTrue(expenses.isEmpty(), "Query must return empty list safely");
    }
}
