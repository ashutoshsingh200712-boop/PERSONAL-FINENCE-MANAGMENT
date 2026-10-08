package com.financeapp.task;

import com.financeapp.dao.ExpenseDAO;
import com.financeapp.dao.NotificationDAO;
import com.financeapp.dao.SpendingPlanDAO;
import com.financeapp.model.Expense;
import com.financeapp.model.Notification;
import com.financeapp.model.SpendingPlan;
import com.financeapp.planner.PlanStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlanMonitorTaskTest {

    private SpendingPlanDAO spendingPlanDAO;
    private ExpenseDAO expenseDAO;
    private NotificationDAO notificationDAO;
    private Connection conn;
    private PlanMonitorTask task;

    @BeforeEach
    void setUp() {
        spendingPlanDAO = mock(SpendingPlanDAO.class);
        expenseDAO = mock(ExpenseDAO.class);
        notificationDAO = mock(NotificationDAO.class);
        conn = mock(Connection.class);

        task = new PlanMonitorTask(spendingPlanDAO, expenseDAO, notificationDAO);
    }

    @Test
    @DisplayName("PlanMonitorTask: Should increment run counter using AtomicInteger")
    void testRunCounterIncrements() {
        assertEquals(0, task.getRunCount());
        task.getRunCounter().incrementAndGet();
        assertEquals(1, task.getRunCount());
    }

    @Test
    @DisplayName("PlanMonitorTask: Detects transition to OVER and inserts notification")
    void testDetectsOverStatusAndInsertsNotification() {
        String currentMonth = YearMonth.now().toString();
        SpendingPlan plan = new SpendingPlan();
        plan.setId(101L);
        plan.setUserId(5L);
        plan.setPlanMonth(currentMonth);
        plan.setDisposableIncome(new BigDecimal("1000.00"));
        plan.setStatus(SpendingPlan.Status.ACTIVE);

        when(spendingPlanDAO.findAllActive(conn)).thenReturn(List.of(plan));

        // User spent $1,500 which is > 110% of expected budget pace -> OVER
        Expense expense = new Expense();
        expense.setAmount(new BigDecimal("1500.00"));
        when(expenseDAO.findByMonthAndCategory(conn, 5L, currentMonth, null, 10000, 0))
                .thenReturn(List.of(expense));

        task.executeWithConnection(conn);

        // Verify status recorded in ConcurrentHashMap
        assertEquals(PlanStatus.OVER, task.getLastKnownStatus().get(101L));

        // Verify notification inserted
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationDAO, times(1)).create(eq(conn), captor.capture());

        Notification notif = captor.getValue();
        assertEquals(5L, notif.getUserId());
        assertEquals(Notification.Type.ALERT, notif.getType());
        assertTrue(notif.getTitle().contains("Overspending Detected"));
    }

    @Test
    @DisplayName("PlanMonitorTask: No duplicate notification when status remains OVER")
    void testNoDuplicateNotificationWhenStatusRemainsOver() {
        String currentMonth = YearMonth.now().toString();
        SpendingPlan plan = new SpendingPlan();
        plan.setId(102L);
        plan.setUserId(5L);
        plan.setPlanMonth(currentMonth);
        plan.setDisposableIncome(new BigDecimal("1000.00"));
        plan.setStatus(SpendingPlan.Status.ACTIVE);

        when(spendingPlanDAO.findAllActive(conn)).thenReturn(List.of(plan));

        Expense expense = new Expense();
        expense.setAmount(new BigDecimal("1500.00"));
        when(expenseDAO.findByMonthAndCategory(conn, 5L, currentMonth, null, 10000, 0))
                .thenReturn(List.of(expense));

        // Pre-populate ConcurrentHashMap with OVER status
        task.getLastKnownStatus().put(102L, PlanStatus.OVER);

        task.executeWithConnection(conn);

        // No new notification should be inserted because status didn't change
        verify(notificationDAO, never()).create(any(Connection.class), any(Notification.class));
    }

    @Test
    @DisplayName("PlanMonitorTask: Detects transition to UNDER (surplus) and inserts notification")
    void testDetectsSurplusAndInsertsNotification() {
        String currentMonth = YearMonth.now().toString();
        SpendingPlan plan = new SpendingPlan();
        plan.setId(103L);
        plan.setUserId(8L);
        plan.setPlanMonth(currentMonth);
        plan.setDisposableIncome(new BigDecimal("1000.00"));
        plan.setStatus(SpendingPlan.Status.ACTIVE);

        when(spendingPlanDAO.findAllActive(conn)).thenReturn(List.of(plan));

        // Minimal spending ($5) -> UNDER (surplus available)
        Expense expense = new Expense();
        expense.setAmount(new BigDecimal("5.00"));
        when(expenseDAO.findByMonthAndCategory(conn, 8L, currentMonth, null, 10000, 0))
                .thenReturn(List.of(expense));

        // Pre-populate with ON_TRACK
        task.getLastKnownStatus().put(103L, PlanStatus.ON_TRACK);

        task.executeWithConnection(conn);

        assertEquals(PlanStatus.UNDER, task.getLastKnownStatus().get(103L));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationDAO, times(1)).create(eq(conn), captor.capture());

        Notification notif = captor.getValue();
        assertEquals(8L, notif.getUserId());
        assertEquals(Notification.Type.INFO, notif.getType());
        assertTrue(notif.getTitle().contains("Surplus Available"));
    }
}
