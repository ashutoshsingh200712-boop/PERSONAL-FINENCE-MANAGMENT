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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecurringEntryTaskTest {

    private static final String H2_URL = "jdbc:h2:mem:recurring_test;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
    private static Connection masterConn;

    private static FixedExpenseDAO fixedExpenseDAO;
    private static ExpenseDAO expenseDAO;
    private static NotificationDAO notificationDAO;
    private static RecurringEntryTask task;

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
            stmt.execute("CREATE TABLE users (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100), email VARCHAR(100), password_hash VARCHAR(100), role VARCHAR(20))");
            stmt.execute("CREATE TABLE categories (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50), description VARCHAR(255), is_system BOOLEAN DEFAULT TRUE)");
            stmt.execute("CREATE TABLE fixed_expenses (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT, category_id BIGINT, title VARCHAR(100), amount DECIMAL(12,2), due_day INT DEFAULT 1, is_active BOOLEAN DEFAULT TRUE, notes TEXT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            stmt.execute("CREATE TABLE expenses (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT, category_id BIGINT, amount DECIMAL(12,2), expense_date DATE, description VARCHAR(255), payment_method VARCHAR(50) DEFAULT 'CASH', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            stmt.execute("CREATE TABLE notifications (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT, title VARCHAR(150), message TEXT, type VARCHAR(20) DEFAULT 'INFO', is_read BOOLEAN DEFAULT FALSE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("INSERT INTO users (id, name, email, password_hash, role) VALUES (1, 'Test User', 'recurring@test.com', 'hash', 'USER')");
            stmt.execute("INSERT INTO categories (id, name) VALUES (1, 'Rent & Housing')");
            stmt.execute("INSERT INTO categories (id, name) VALUES (2, 'Internet & Utilities')");
        }

        fixedExpenseDAO = new JdbcFixedExpenseDAO();
        expenseDAO = new JdbcExpenseDAO();
        notificationDAO = new JdbcNotificationDAO();
        task = new RecurringEntryTask(fixedExpenseDAO, expenseDAO, notificationDAO);
    }

    @AfterAll
    static void tearDown() throws Exception {
        DBConnection.resetCustomConnectionSupplier();
        if (masterConn != null && !masterConn.isClosed()) {
            masterConn.close();
        }
    }

    @Test
    @DisplayName("RecurringEntryTask: Should increment run counter using AtomicInteger")
    void testRunCounter() {
        assertEquals(0, task.getRunCount());
        task.getRunCounter().incrementAndGet();
        assertEquals(1, task.getRunCount());
    }

    @Test
    @DisplayName("RecurringEntryTask: Automatically adds due fixed expenses and avoids duplicate runs")
    void testExecuteForDatePostsExpensesAndPreventsDuplicates() throws Exception {
        LocalDate date = LocalDate.of(2026, 10, 15);
        int dueDay = date.getDayOfMonth(); // 15

        // Seed an active fixed expense with due_day = 15
        FixedExpense fe = new FixedExpense();
        fe.setUserId(1L);
        fe.setCategoryId(1L);
        fe.setTitle("Apartment Rent");
        fe.setAmount(new BigDecimal("1200.00"));
        fe.setDueDay(dueDay);
        fe.setActive(true);
        fixedExpenseDAO.create(masterConn, fe);

        // Run task for target date
        task.executeForDate(masterConn, date);

        // Verify expense was created
        List<Expense> expenses = expenseDAO.findByUserId(1L, 10, 0);
        assertEquals(1, expenses.size());
        Expense posted = expenses.get(0);
        assertEquals("Recurring: Apartment Rent", posted.getDescription());
        assertEquals(new BigDecimal("1200.00"), posted.getAmount());
        assertEquals(date, posted.getDate());
        assertEquals("AUTO_DEBIT", posted.getPaymentMethod());

        // Verify notification was created
        List<Notification> notifs = notificationDAO.findByUserId(1L);
        assertEquals(1, notifs.size());
        assertTrue(notifs.get(0).getTitle().contains("Recurring Expense Posted"));

        // Running task AGAIN on the same date should NOT duplicate the expense
        task.executeForDate(masterConn, date);

        List<Expense> expensesAfterSecondRun = expenseDAO.findByUserId(1L, 10, 0);
        assertEquals(1, expensesAfterSecondRun.size(), "Should not create duplicate expense on repeat execution");
    }
}
