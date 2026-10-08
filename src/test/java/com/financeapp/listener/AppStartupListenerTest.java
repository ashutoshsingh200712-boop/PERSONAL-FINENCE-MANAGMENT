package com.financeapp.listener;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppStartupListenerTest {

    @Test
    @DisplayName("AppStartupListener: Initializes 2-thread scheduler and shuts down on contextDestroyed")
    void testStartupAndShutdownLifecycle() {
        AppStartupListener listener = new AppStartupListener();

        ServletContext ctx = mock(ServletContext.class);
        ServletContextEvent sce = new ServletContextEvent(ctx);

        when(ctx.getInitParameter("taskIntervalSeconds")).thenReturn("60");

        listener.contextInitialized(sce);

        // Verify scheduler created with 2 core threads
        ScheduledExecutorService scheduler = listener.getScheduler();
        assertNotNull(scheduler);
        assertFalse(scheduler.isShutdown());

        if (scheduler instanceof ScheduledThreadPoolExecutor stpe) {
            assertEquals(2, stpe.getCorePoolSize());
        }

        // Verify tasks and scheduler set in ServletContext
        verify(ctx, times(1)).setAttribute(eq(AppStartupListener.SCHEDULER_ATTR), eq(scheduler));
        verify(ctx, times(1)).setAttribute(eq(AppStartupListener.PLAN_MONITOR_TASK_ATTR), any());
        verify(ctx, times(1)).setAttribute(eq(AppStartupListener.RECURRING_ENTRY_TASK_ATTR), any());

        // Verify shutdown
        listener.contextDestroyed(sce);
        assertTrue(scheduler.isShutdown());
    }

    @Test
    @DisplayName("AppStartupListener: Configurable interval resolution")
    void testConfigurableIntervalResolution() {
        AppStartupListener listener = new AppStartupListener();
        ServletContext ctx = mock(ServletContext.class);

        // 1. Context param
        when(ctx.getInitParameter("taskIntervalSeconds")).thenReturn("120");
        assertEquals(120L, listener.resolveIntervalSeconds(ctx));

        // 2. Default when null (demo mode: 60 seconds = 1 minute)
        when(ctx.getInitParameter("taskIntervalSeconds")).thenReturn(null);
        assertEquals(60L, listener.resolveIntervalSeconds(ctx));

        // 3. System property override
        System.setProperty("financeapp.task.interval.seconds", "30");
        try {
            assertEquals(30L, listener.resolveIntervalSeconds(ctx));
        } finally {
            System.clearProperty("financeapp.task.interval.seconds");
        }
    }
}
