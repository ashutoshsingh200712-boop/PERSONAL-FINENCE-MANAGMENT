package com.financeapp.service;

import com.financeapp.dao.AdviceDAO;
import com.financeapp.dao.BudgetDAO;
import com.financeapp.dao.ExpenseDAO;
import com.financeapp.dao.NotificationDAO;
import com.financeapp.dao.SpendingPlanDAO;
import com.financeapp.model.Advice;
import com.financeapp.model.Budget;
import com.financeapp.model.PlanItem;
import com.financeapp.model.SpendingPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("BudgetService Unit Tests")
class BudgetServiceTest {

    private BudgetDAO budgetDAO;
    private SpendingPlanDAO spendingPlanDAO;
    private ExpenseDAO expenseDAO;
    private AdviceDAO adviceDAO;
    private NotificationDAO notificationDAO;
    private Connection conn;

    private BudgetService budgetService;

    @BeforeEach
    void setUp() {
        budgetDAO = mock(BudgetDAO.class);
        spendingPlanDAO = mock(SpendingPlanDAO.class);
        expenseDAO = mock(ExpenseDAO.class);
        adviceDAO = mock(AdviceDAO.class);
        notificationDAO = mock(NotificationDAO.class);
        conn = mock(Connection.class);

        budgetService = new BudgetService(budgetDAO, spendingPlanDAO, expenseDAO, adviceDAO, notificationDAO);
    }

    @Test
    @DisplayName("Accept Plan: Copies plan items into budgets with source = AI_PLAN and updates advice to ACCEPTED")
    void testAcceptPlanCopiesItemsToBudgets() {
        long userId = 5L;
        long planId = 20L;
        String month = "2026-10";

        SpendingPlan plan = new SpendingPlan();
        plan.setId(planId);
        plan.setUserId(userId);
        plan.setPlanMonth(month);

        PlanItem item1 = new PlanItem();
        item1.setCategoryId(1L);
        item1.setAllocatedAmount(new BigDecimal("400.00"));

        PlanItem item2 = new PlanItem();
        item2.setCategoryId(2L);
        item2.setAllocatedAmount(new BigDecimal("200.00"));

        plan.setItems(List.of(item1, item2));

        when(spendingPlanDAO.findById(conn, planId)).thenReturn(Optional.of(plan));

        Advice advice = new Advice(50L, userId, planId, "SurplusRule", "Surplus Notice", "Take advantage", Advice.Status.NEW, null, null);
        when(adviceDAO.findByPlanId(conn, planId)).thenReturn(List.of(advice));

        boolean success = budgetService.acceptPlanIntoBudgets(conn, userId, planId);
        assertTrue(success);

        // Verify each item upserted into budgets with source = AI_PLAN
        ArgumentCaptor<Budget> budgetCaptor = ArgumentCaptor.forClass(Budget.class);
        verify(budgetDAO, times(2)).upsert(eq(conn), budgetCaptor.capture());

        List<Budget> savedBudgets = budgetCaptor.getAllValues();
        assertEquals(2, savedBudgets.size());

        for (Budget b : savedBudgets) {
            assertEquals(userId, b.getUserId());
            assertEquals(month, b.getBudgetMonth());
            assertEquals("AI_PLAN", b.getSource());
        }

        // Verify plan status updated to ACTIVE
        assertEquals(SpendingPlan.Status.ACTIVE, plan.getStatus());
        verify(spendingPlanDAO, times(1)).update(eq(conn), eq(plan));

        // Verify advice status updated to ACCEPTED
        verify(adviceDAO, times(1)).updateStatus(eq(conn), eq(50L), eq(Advice.Status.ACCEPTED));

        // Verify notification inserted
        verify(notificationDAO, times(1)).create(eq(conn), any());
    }

    @Test
    @DisplayName("Accept Plan: Security check rejects accepting another user's plan")
    void testAcceptPlanRejectsUnauthorizedUser() {
        long authorizedUserId = 5L;
        long planOwnerId = 99L;
        long planId = 20L;

        SpendingPlan plan = new SpendingPlan();
        plan.setId(planId);
        plan.setUserId(planOwnerId);

        when(spendingPlanDAO.findById(conn, planId)).thenReturn(Optional.of(plan));

        assertThrows(SecurityException.class, () -> budgetService.acceptPlanIntoBudgets(conn, authorizedUserId, planId));
        verify(budgetDAO, never()).upsert(any(Connection.class), any(Budget.class));
    }
}
