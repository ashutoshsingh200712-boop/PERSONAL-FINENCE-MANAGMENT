package com.financeapp.dao.jdbc;

import com.financeapp.dao.*;
import com.financeapp.exception.DataAccessException;
import com.financeapp.model.*;
import com.financeapp.util.DBConnection;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JDBC Data Layer Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JdbcDataLayerIntegrationTest {

    private static final String H2_URL = "jdbc:h2:mem:finance_test;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
    private static Connection testMasterConn;

    private static UserDAO userDAO;
    private static CategoryDAO categoryDAO;
    private static ExpenseDAO expenseDAO;
    private static FixedExpenseDAO fixedExpenseDAO;
    private static BudgetDAO budgetDAO;
    private static IncomeDAO incomeDAO;
    private static SpendingPlanDAO spendingPlanDAO;

    @BeforeAll
    static void setUpDatabase() throws Exception {
        // Keep a master connection alive so the in-memory database persists across DAO calls
        testMasterConn = DriverManager.getConnection(H2_URL, "sa", "");

        // Initialize schema in H2 MySQL mode
        try (Statement stmt = testMasterConn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS audit_logs");
            stmt.execute("DROP TABLE IF EXISTS notifications");
            stmt.execute("DROP TABLE IF EXISTS budgets");
            stmt.execute("DROP TABLE IF EXISTS plan_items");
            stmt.execute("DROP TABLE IF EXISTS spending_plans");
            stmt.execute("DROP TABLE IF EXISTS expenses");
            stmt.execute("DROP TABLE IF EXISTS fixed_expenses");
            stmt.execute("DROP TABLE IF EXISTS income");
            stmt.execute("DROP TABLE IF EXISTS categories");
            stmt.execute("DROP TABLE IF EXISTS system_settings");
            stmt.execute("DROP TABLE IF EXISTS users");

            stmt.execute("CREATE TABLE users (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(150) NOT NULL UNIQUE, " +
                    "password_hash VARCHAR(255) NOT NULL, " +
                    "role VARCHAR(20) NOT NULL, " +
                    "active BOOLEAN NOT NULL DEFAULT TRUE, " +
                    "failed_logins INT NOT NULL DEFAULT 0, " +
                    "locked_until TIMESTAMP NULL, " +
                    "advisor_id BIGINT NULL, " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "CONSTRAINT fk_users_advisor FOREIGN KEY (advisor_id) REFERENCES users (id) ON DELETE SET NULL" +
                    ")");

            stmt.execute("CREATE TABLE categories (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(50) NOT NULL UNIQUE, " +
                    "description VARCHAR(255) NULL, " +
                    "is_system BOOLEAN NOT NULL DEFAULT TRUE, " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ")");

            stmt.execute("CREATE TABLE income (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id BIGINT NOT NULL, " +
                    "source VARCHAR(100) NOT NULL, " +
                    "amount DECIMAL(12,2) NOT NULL, " +
                    "frequency VARCHAR(20) NOT NULL, " +
                    "income_date DATE NOT NULL, " +
                    "notes TEXT NULL, " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "CONSTRAINT fk_income_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE" +
                    ")");

            stmt.execute("CREATE TABLE fixed_expenses (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id BIGINT NOT NULL, " +
                    "category_id BIGINT NOT NULL, " +
                    "title VARCHAR(100) NOT NULL, " +
                    "amount DECIMAL(12,2) NOT NULL, " +
                    "due_day INT NOT NULL DEFAULT 1, " +
                    "is_active BOOLEAN NOT NULL DEFAULT TRUE, " +
                    "notes TEXT NULL, " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "CONSTRAINT fk_fixed_expenses_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE, " +
                    "CONSTRAINT fk_fixed_expenses_cat FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT" +
                    ")");

            stmt.execute("CREATE TABLE expenses (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id BIGINT NOT NULL, " +
                    "category_id BIGINT NOT NULL, " +
                    "amount DECIMAL(12,2) NOT NULL, " +
                    "expense_date DATE NOT NULL, " +
                    "description VARCHAR(255) NOT NULL, " +
                    "payment_method VARCHAR(50) NOT NULL DEFAULT 'CASH', " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "CONSTRAINT fk_expenses_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE, " +
                    "CONSTRAINT fk_expenses_cat FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT" +
                    ")");

            stmt.execute("CREATE TABLE spending_plans (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id BIGINT NOT NULL, " +
                    "plan_month CHAR(7) NOT NULL, " +
                    "total_income DECIMAL(12,2) NOT NULL DEFAULT 0.00, " +
                    "total_fixed_expenses DECIMAL(12,2) NOT NULL DEFAULT 0.00, " +
                    "disposable_income DECIMAL(12,2) NOT NULL DEFAULT 0.00, " +
                    "status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "CONSTRAINT fk_spending_plans_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE, " +
                    "CONSTRAINT uq_spending_plans_user_month UNIQUE (user_id, plan_month)" +
                    ")");

            stmt.execute("CREATE TABLE plan_items (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "plan_id BIGINT NOT NULL, " +
                    "category_id BIGINT NOT NULL, " +
                    "allocated_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00, " +
                    "weight_percentage DECIMAL(5,2) NOT NULL DEFAULT 0.00, " +
                    "notes VARCHAR(255) NULL, " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "CONSTRAINT fk_plan_items_plan FOREIGN KEY (plan_id) REFERENCES spending_plans (id) ON DELETE CASCADE, " +
                    "CONSTRAINT fk_plan_items_cat FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT" +
                    ")");

            stmt.execute("CREATE TABLE budgets (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id BIGINT NOT NULL, " +
                    "category_id BIGINT NOT NULL, " +
                    "budget_month CHAR(7) NOT NULL, " +
                    "budget_limit DECIMAL(12,2) NOT NULL, " +
                    "alert_threshold DECIMAL(5,2) NOT NULL DEFAULT 80.00, " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE, " +
                    "CONSTRAINT fk_budgets_cat FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT, " +
                    "CONSTRAINT uq_budgets_user_cat_month UNIQUE (user_id, category_id, budget_month)" +
                    ")");

            stmt.execute("CREATE TABLE notifications (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id BIGINT NOT NULL, " +
                    "title VARCHAR(150) NOT NULL, " +
                    "message TEXT NOT NULL, " +
                    "type VARCHAR(20) NOT NULL DEFAULT 'INFO', " +
                    "is_read BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ")");

            stmt.execute("CREATE TABLE system_settings (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "setting_key VARCHAR(100) NOT NULL UNIQUE, " +
                    "setting_value VARCHAR(255) NOT NULL, " +
                    "description VARCHAR(255) NULL, " +
                    "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ")");

            stmt.execute("CREATE TABLE audit_logs (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id BIGINT NULL, " +
                    "action VARCHAR(100) NOT NULL, " +
                    "entity_type VARCHAR(50) NOT NULL, " +
                    "entity_id BIGINT NULL, " +
                    "details TEXT NULL, " +
                    "ip_address VARCHAR(45) NULL, " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ")");
        }

        // Supply H2 connection to DBConnection for tests
        DBConnection.setCustomConnectionSupplier(() -> {
            try {
                return DriverManager.getConnection(H2_URL, "sa", "");
            } catch (SQLException e) {
                throw new RuntimeException("Error obtaining H2 test connection", e);
            }
        });

        userDAO = new JdbcUserDAO();
        categoryDAO = new JdbcCategoryDAO();
        expenseDAO = new JdbcExpenseDAO();
        fixedExpenseDAO = new JdbcFixedExpenseDAO();
        budgetDAO = new JdbcBudgetDAO();
        incomeDAO = new JdbcIncomeDAO();
        spendingPlanDAO = new JdbcSpendingPlanDAO();
    }

    @AfterAll
    static void tearDown() throws Exception {
        DBConnection.resetCustomConnectionSupplier();
        if (testMasterConn != null && !testMasterConn.isClosed()) {
            testMasterConn.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should prove full CRUD and polymorphic OOP mapping for User")
    void testUserCrudAndPolymorphicMapping() {
        // Create RegularUser
        User regularUser = new RegularUser();
        regularUser.setName("Alice Test");
        regularUser.setEmail("alice@integration.test");
        regularUser.setPasswordHash("$2a$10$hashedpasswordstringforuser");
        regularUser.setActive(true);

        long userId = userDAO.create(regularUser);
        assertTrue(userId > 0, "Created user ID must be positive");
        assertEquals("/user/dashboard", regularUser.getDashboardPath());

        // Read by ID
        Optional<User> fetchedOpt = userDAO.findById(userId);
        assertTrue(fetchedOpt.isPresent(), "User should be found by ID");
        User fetched = fetchedOpt.get();
        assertInstanceOf(RegularUser.class, fetched, "Should polymorphically instantiate RegularUser");
        assertEquals("Alice Test", fetched.getName());
        assertEquals("alice@integration.test", fetched.getEmail());
        assertEquals("/user/dashboard", fetched.getDashboardPath());

        // Update
        fetched.setName("Alice Updated");
        fetched.setActive(false);
        boolean updated = userDAO.update(fetched);
        assertTrue(updated, "User update should succeed");

        Optional<User> reloaded = userDAO.findById(userId);
        assertTrue(reloaded.isPresent());
        assertEquals("Alice Updated", reloaded.get().getName());
        assertFalse(reloaded.get().isActive());

        // Delete
        boolean deleted = userDAO.delete(userId);
        assertTrue(deleted, "User deletion should succeed");
        assertTrue(userDAO.findById(userId).isEmpty(), "User should no longer exist");
    }

    @Test
    @Order(2)
    @DisplayName("Should prove full CRUD, month/category filter, pagination, and GROUP BY for Expenses")
    void testExpenseCrudAndAggregations() {
        // Setup User and Categories
        User user = new RegularUser();
        user.setName("Bob ExpenseTester");
        user.setEmail("bob.expenses@integration.test");
        user.setPasswordHash("$2a$10$hashedpw");
        long userId = userDAO.create(user);

        Category catFood = new Category(null, "Food", "Groceries", true, LocalDateTime.now());
        long foodId = categoryDAO.create(catFood);

        Category catEnt = new Category(null, "Entertainment", "Leisure", true, LocalDateTime.now());
        long entId = categoryDAO.create(catEnt);

        // Create Expenses
        Expense e1 = new Expense(null, userId, foodId, new BigDecimal("45.50"),
                LocalDate.of(2026, 10, 5), "Supermarket Grocery", "CARD", LocalDateTime.now());
        long e1Id = expenseDAO.create(e1);
        assertTrue(e1Id > 0);
        assertEquals(new BigDecimal("-45.50"), e1.signedAmount(), "Expense signed amount must be negative");

        Expense e2 = new Expense(null, userId, foodId, new BigDecimal("25.00"),
                LocalDate.of(2026, 10, 12), "Lunch Bistro", "CASH", LocalDateTime.now());
        expenseDAO.create(e2);

        Expense e3 = new Expense(null, userId, entId, new BigDecimal("60.00"),
                LocalDate.of(2026, 10, 20), "Cinema & Concert", "CARD", LocalDateTime.now());
        expenseDAO.create(e3);

        Expense e4DifferentMonth = new Expense(null, userId, foodId, new BigDecimal("90.00"),
                LocalDate.of(2026, 9, 15), "September Groceries", "CARD", LocalDateTime.now());
        expenseDAO.create(e4DifferentMonth);

        // Filter by month (2026-10) and Category (Food)
        List<Expense> foodOctober = expenseDAO.findByMonthAndCategory(userId, "2026-10", foodId, 10, 0);
        assertEquals(2, foodOctober.size(), "Should find exactly 2 food expenses in October");

        int foodOctoberCount = expenseDAO.countByMonthAndCategory(userId, "2026-10", foodId);
        assertEquals(2, foodOctoberCount);

        // Test Pagination
        List<Expense> paginatedPage1 = expenseDAO.findByMonthAndCategory(userId, "2026-10", null, 2, 0);
        assertEquals(2, paginatedPage1.size(), "Page 1 limit 2 must return 2 items");

        List<Expense> paginatedPage2 = expenseDAO.findByMonthAndCategory(userId, "2026-10", null, 2, 2);
        assertEquals(1, paginatedPage2.size(), "Page 2 offset 2 must return 1 remaining item");

        // GROUP BY query: Category spending in 2026-10
        List<CategorySpendingSummary> summaries = expenseDAO.getCategorySpendingInMonth(userId, "2026-10");
        assertNotNull(summaries);
        assertEquals(2, summaries.size(), "Should have aggregated 2 categories in October");

        // Food total = 45.50 + 25.00 = 70.50
        CategorySpendingSummary foodSummary = summaries.stream()
                .filter(s -> "Food".equals(s.getCategoryName()))
                .findFirst()
                .orElseThrow();
        assertEquals(new BigDecimal("70.50"), foodSummary.getTotalAmount());
        assertEquals(2, foodSummary.getTransactionCount());

        // Entertainment total = 60.00
        CategorySpendingSummary entSummary = summaries.stream()
                .filter(s -> "Entertainment".equals(s.getCategoryName()))
                .findFirst()
                .orElseThrow();
        assertEquals(new BigDecimal("60.00"), entSummary.getTotalAmount());
        assertEquals(1, entSummary.getTransactionCount());

        // Update Expense
        e1.setAmount(new BigDecimal("50.00"));
        e1.setDescription("Updated Supermarket Grocery");
        boolean updated = expenseDAO.update(e1);
        assertTrue(updated);

        Optional<Expense> reloadedE1 = expenseDAO.findById(e1Id);
        assertTrue(reloadedE1.isPresent());
        assertEquals(new BigDecimal("50.00"), reloadedE1.get().getAmount());
        assertEquals("Updated Supermarket Grocery", reloadedE1.get().getDescription());

        // Delete Expense
        boolean deleted = expenseDAO.delete(e1Id);
        assertTrue(deleted);
        assertTrue(expenseDAO.findById(e1Id).isEmpty());
    }

    @Test
    @Order(3)
    @DisplayName("Should prove CRUD for FixedExpenses and Budgets")
    void testFixedExpenseAndBudgetCrud() {
        // Setup User and Category
        User user = new RegularUser();
        user.setName("Carol FixedTester");
        user.setEmail("carol.fixed@integration.test");
        user.setPasswordHash("$2a$10$hashedpw");
        long userId = userDAO.create(user);

        Category cat = new Category(null, "Housing", "Rent and home", true, LocalDateTime.now());
        long catId = categoryDAO.create(cat);

        // Fixed Expense CRUD
        FixedExpense fe = new FixedExpense(null, userId, catId, "Apartment Rent",
                new BigDecimal("1200.00"), 1, true, "Monthly lease", LocalDateTime.now());
        long feId = fixedExpenseDAO.create(fe);
        assertTrue(feId > 0);

        List<FixedExpense> activeList = fixedExpenseDAO.findActiveByUserId(userId);
        assertEquals(1, activeList.size());
        assertEquals("Apartment Rent", activeList.get(0).getTitle());

        fe.setAmount(new BigDecimal("1250.00"));
        assertTrue(fixedExpenseDAO.update(fe));
        assertEquals(new BigDecimal("1250.00"), fixedExpenseDAO.findById(feId).orElseThrow().getAmount());

        assertTrue(fixedExpenseDAO.delete(feId));
        assertTrue(fixedExpenseDAO.findById(feId).isEmpty());

        // Budget CRUD
        Budget budget = new Budget(null, userId, catId, "2026-10",
                new BigDecimal("1500.00"), new BigDecimal("80.00"), LocalDateTime.now());
        long budgetId = budgetDAO.create(budget);
        assertTrue(budgetId > 0);

        Optional<Budget> budgetOpt = budgetDAO.findByUserCategoryAndMonth(userId, catId, "2026-10");
        assertTrue(budgetOpt.isPresent());
        assertEquals(new BigDecimal("1500.00"), budgetOpt.get().getBudgetLimit());

        budget.setBudgetLimit(new BigDecimal("1600.00"));
        assertTrue(budgetDAO.update(budget));
        assertEquals(new BigDecimal("1600.00"), budgetDAO.findById(budgetId).orElseThrow().getBudgetLimit());

        assertTrue(budgetDAO.delete(budgetId));
        assertTrue(budgetDAO.findById(budgetId).isEmpty());
    }

    @Test
    @Order(4)
    @DisplayName("Should prove SpendingPlan and PlanItems creation and retrieval")
    void testSpendingPlanWithItems() {
        User user = new RegularUser();
        user.setName("Dave PlannerTester");
        user.setEmail("dave.plan@integration.test");
        user.setPasswordHash("$2a$10$hashedpw");
        long userId = userDAO.create(user);

        Category catSavings = new Category(null, "Savings", "Emergency reserve", true, LocalDateTime.now());
        long catSavingsId = categoryDAO.create(catSavings);

        SpendingPlan plan = new SpendingPlan();
        plan.setUserId(userId);
        plan.setPlanMonth("2026-10");
        plan.setTotalIncome(new BigDecimal("4000.00"));
        plan.setTotalFixedExpenses(new BigDecimal("1500.00"));
        plan.setDisposableIncome(new BigDecimal("2500.00"));
        plan.setStatus(SpendingPlan.Status.ACTIVE);

        PlanItem item1 = new PlanItem();
        item1.setCategoryId(catSavingsId);
        item1.setAllocatedAmount(new BigDecimal("500.00"));
        item1.setWeightPercentage(new BigDecimal("20.00"));
        item1.setNotes("20% wealth allocation");
        plan.addItem(item1);

        long planId = spendingPlanDAO.create(plan);
        assertTrue(planId > 0);

        Optional<SpendingPlan> fetchedOpt = spendingPlanDAO.findById(planId);
        assertTrue(fetchedOpt.isPresent());
        SpendingPlan fetched = fetchedOpt.get();
        assertEquals(1, fetched.getItems().size());
        assertEquals(new BigDecimal("500.00"), fetched.getItems().get(0).getAllocatedAmount());

        assertTrue(spendingPlanDAO.delete(planId));
        assertTrue(spendingPlanDAO.findById(planId).isEmpty());
    }

    @Test
    @Order(5)
    @DisplayName("Should prove transaction rolls back completely when the second step fails")
    void testTransactionRollsBackOnSecondStepFailure() throws Exception {
        // Setup User and Category
        User user = new RegularUser();
        user.setName("Txn User");
        user.setEmail("txn.rollback@integration.test");
        user.setPasswordHash("$2a$10$hashedpw");
        long userId = userDAO.create(user);

        Category cat = new Category(null, "TxnCategory", "Category for tx testing", true, LocalDateTime.now());
        long catId = categoryDAO.create(cat);

        Long firstExpenseId = null;

        // 1. Transaction where Step 2 fails
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Step 1: Valid Expense
                Expense step1Expense = new Expense(null, userId, catId, new BigDecimal("100.00"),
                        LocalDate.of(2026, 10, 1), "Txn Step 1", "CARD", LocalDateTime.now());
                firstExpenseId = expenseDAO.create(conn, step1Expense);
                assertTrue(firstExpenseId > 0, "Step 1 should produce an ID");

                // Step 2: Invalid operation on connection (violates foreign key with invalid category ID)
                Expense step2InvalidExpense = new Expense(null, userId, 999999L, new BigDecimal("50.00"),
                        LocalDate.of(2026, 10, 1), "Txn Step 2 Invalid", "CARD", LocalDateTime.now());
                expenseDAO.create(conn, step2InvalidExpense);

                conn.commit();
                fail("Transaction should have failed at Step 2 and jumped to catch block");
            } catch (Exception e) {
                // Rollback transaction upon error
                conn.rollback();
            }
        }

        // Verify Step 1 was ROLLED BACK and does NOT exist in the database!
        assertNotNull(firstExpenseId);
        Optional<Expense> rolledBackExpense = expenseDAO.findById(firstExpenseId);
        assertTrue(rolledBackExpense.isEmpty(), "Step 1 expense must NOT exist after rollback!");

        // 2. Transaction where both steps succeed and commit
        Long valid1Id;
        Long valid2Id;
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Expense valid1 = new Expense(null, userId, catId, new BigDecimal("150.00"),
                        LocalDate.of(2026, 10, 2), "Valid Step 1", "CARD", LocalDateTime.now());
                valid1Id = expenseDAO.create(conn, valid1);

                Expense valid2 = new Expense(null, userId, catId, new BigDecimal("250.00"),
                        LocalDate.of(2026, 10, 2), "Valid Step 2", "CARD", LocalDateTime.now());
                valid2Id = expenseDAO.create(conn, valid2);

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }

        // Verify both steps committed successfully
        assertTrue(expenseDAO.findById(valid1Id).isPresent(), "Valid Step 1 expense must exist");
        assertTrue(expenseDAO.findById(valid2Id).isPresent(), "Valid Step 2 expense must exist");
    }
}
