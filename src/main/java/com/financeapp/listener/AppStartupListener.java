package com.financeapp.listener;

import com.financeapp.dao.SettingsDAO;
import com.financeapp.dao.jdbc.JdbcSettingsDAO;
import com.financeapp.task.PlanMonitorTask;
import com.financeapp.task.RecurringEntryTask;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application lifecycle listener that manages the background multithreading engine.
 * Initializes a 2-thread ScheduledExecutorService for PlanMonitorTask and RecurringEntryTask,
 * a fixed-pool ExecutorService for asynchronous CSV export, and handles graceful shutdown
 * when the context is destroyed.
 */
@WebListener
public class AppStartupListener implements ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger(AppStartupListener.class.getName());

    public static final String SCHEDULER_ATTR = "appScheduler";
    public static final String PLAN_MONITOR_TASK_ATTR = "planMonitorTask";
    public static final String RECURRING_ENTRY_TASK_ATTR = "recurringEntryTask";
    public static final String CSV_EXPORT_EXECUTOR_ATTR = "csvExportExecutor";

    private ScheduledExecutorService scheduler;
    private ExecutorService csvExportExecutor;
    private PlanMonitorTask planMonitorTask;
    private RecurringEntryTask recurringEntryTask;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.info("Starting up Personal Finance Management Platform background thread engine...");

        // 1. Create 2-thread ScheduledExecutorService with custom named daemon threads
        this.scheduler = Executors.newScheduledThreadPool(2, new ThreadFactory() {
            private final AtomicInteger threadIndex = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "FinanceApp-Worker-" + threadIndex.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        });

        // 2. Create fixed pool ExecutorService for asynchronous CSV exports without blocking request threads
        this.csvExportExecutor = Executors.newFixedThreadPool(2, new ThreadFactory() {
            private final AtomicInteger threadIndex = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "FinanceApp-CsvExport-" + threadIndex.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        });

        this.planMonitorTask = new PlanMonitorTask();
        this.recurringEntryTask = new RecurringEntryTask();

        ServletContext ctx = sce.getServletContext();
        ctx.setAttribute(SCHEDULER_ATTR, scheduler);
        ctx.setAttribute(PLAN_MONITOR_TASK_ATTR, planMonitorTask);
        ctx.setAttribute(RECURRING_ENTRY_TASK_ATTR, recurringEntryTask);
        ctx.setAttribute(CSV_EXPORT_EXECUTOR_ATTR, csvExportExecutor);

        // Resolve configurable interval (1 minute in demo mode, 6 hours otherwise)
        long intervalSeconds = resolveIntervalSeconds(ctx);
        LOGGER.info("Configured background tasks with interval: " + intervalSeconds + " seconds (demo mode: 60s, non-demo: 21600s)");

        // Initial delay staggered to avoid initial burst contention
        long initialDelay = 5;
        scheduler.scheduleAtFixedRate(planMonitorTask, initialDelay, intervalSeconds, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(recurringEntryTask, initialDelay + 5, intervalSeconds, TimeUnit.SECONDS);

        LOGGER.info("Background tasks successfully scheduled with 2 worker threads.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.info("Shutting down background thread pools...");

        // Shut down scheduler
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOGGER.warning("ScheduledExecutorService did not terminate gracefully within 5s; forcing shutdown.");
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "Scheduler shutdown interrupted; forcing shutdown.", e);
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Shut down CSV export fixed pool
        if (csvExportExecutor != null && !csvExportExecutor.isShutdown()) {
            csvExportExecutor.shutdown();
            try {
                if (!csvExportExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOGGER.warning("CsvExportExecutor did not terminate gracefully within 5s; forcing shutdown.");
                    csvExportExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "CSV export executor shutdown interrupted; forcing shutdown.", e);
                csvExportExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        LOGGER.info("Background thread pool shutdown complete.");
    }

    /**
     * Resolves task interval in seconds: checks system property, context param,
     * system_settings database table, defaulting to 60 seconds (1 minute in demo mode, 6 hours otherwise).
     */
    public long resolveIntervalSeconds(ServletContext ctx) {
        // 1. System property check (useful for integration tests)
        String prop = System.getProperty("financeapp.task.interval.seconds");
        if (prop != null && !prop.trim().isEmpty()) {
            try {
                return Long.parseLong(prop.trim());
            } catch (NumberFormatException ignored) {}
        }

        // 2. ServletContext init param
        if (ctx != null) {
            String ctxParam = ctx.getInitParameter("taskIntervalSeconds");
            if (ctxParam != null && !ctxParam.trim().isEmpty()) {
                try {
                    return Long.parseLong(ctxParam.trim());
                } catch (NumberFormatException ignored) {}
            }
        }

        // 3. Database system_settings table
        try {
            SettingsDAO settingsDAO = new JdbcSettingsDAO();
            Optional<String> setting = settingsDAO.get("task_interval_minutes");
            if (setting.isPresent()) {
                long minutes = Long.parseLong(setting.get());
                if (minutes > 0) {
                    return minutes * 60L;
                }
            }
        } catch (Exception e) {
            LOGGER.fine("Could not read task interval from database settings: " + e.getMessage());
        }

        // 4. Check demo mode: 1 minute (60 seconds) in demo mode, 6 hours (21600 seconds) otherwise
        boolean isDemo = true;
        String demoProp = System.getProperty("financeapp.demo.mode");
        if (demoProp != null) {
            isDemo = Boolean.parseBoolean(demoProp.trim());
        } else if (ctx != null) {
            String demoParam = ctx.getInitParameter("demoMode");
            if (demoParam != null) {
                isDemo = Boolean.parseBoolean(demoParam.trim());
            }
        }

        return isDemo ? 60L : (6 * 3600L);
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    public ExecutorService getCsvExportExecutor() {
        return csvExportExecutor;
    }

    public PlanMonitorTask getPlanMonitorTask() {
        return planMonitorTask;
    }

    public RecurringEntryTask getRecurringEntryTask() {
        return recurringEntryTask;
    }
}
