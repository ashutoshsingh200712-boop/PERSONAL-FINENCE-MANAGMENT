package com.financeapp.servlet;

import com.financeapp.dao.ExpenseDAO;
import com.financeapp.dao.jdbc.JdbcExpenseDAO;
import com.financeapp.listener.AppStartupListener;
import com.financeapp.model.Expense;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Asynchronous servlet that streams expense CSV export using a fixed-pool ExecutorService
 * without blocking the HTTP request thread.
 */
@WebServlet(name = "CsvExportServlet", urlPatterns = {"/expenses/export"}, asyncSupported = true)
public class CsvExportServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(CsvExportServlet.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ExpenseDAO expenseDAO;
    private final ExecutorService fallbackExecutor;

    public CsvExportServlet() {
        this(new JdbcExpenseDAO(), Executors.newFixedThreadPool(2));
    }

    public CsvExportServlet(ExpenseDAO expenseDAO, ExecutorService fallbackExecutor) {
        this.expenseDAO = expenseDAO;
        this.fallbackExecutor = fallbackExecutor;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        Long userId = (Long) session.getAttribute("userId");
        String month = req.getParameter("month"); // optional month filter

        // Retrieve background fixed-pool executor from ServletContext or use fallback
        ExecutorService executor = (ExecutorService) req.getServletContext().getAttribute(AppStartupListener.CSV_EXPORT_EXECUTOR_ATTR);
        if (executor == null || executor.isShutdown()) {
            executor = fallbackExecutor;
        }

        // Start non-blocking asynchronous servlet processing
        final AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(30000); // 30 second timeout

        executor.submit(() -> {
            try {
                HttpServletResponse asyncResp = (HttpServletResponse) asyncContext.getResponse();

                // Fetch expenses in worker thread
                List<Expense> expenses;
                if (month != null && month.matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
                    expenses = expenseDAO.findByMonthAndCategory(userId, month, null, 10000, 0);
                } else {
                    expenses = expenseDAO.findByUserId(userId, 10000, 0);
                }

                // Format filename and headers
                String filename = (month != null && !month.isEmpty())
                        ? "expenses_" + month + ".csv"
                        : "expenses_export.csv";

                asyncResp.setContentType("text/csv; charset=UTF-8");
                asyncResp.setCharacterEncoding("UTF-8");
                asyncResp.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
                asyncResp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                asyncResp.setHeader("Pragma", "no-cache");
                asyncResp.setDateHeader("Expires", 0);

                // Build CSV string
                StringBuilder sb = new StringBuilder();
                sb.append("ID,Date,Category,Amount,Payment Method,Description\n");

                for (Expense e : expenses) {
                    sb.append(e.getId() != null ? e.getId() : "").append(",");
                    sb.append(e.getDate() != null ? e.getDate().format(DATE_FORMATTER) : "").append(",");
                    sb.append(escapeCsv(e.getCategoryName())).append(",");
                    sb.append(e.getAmount() != null ? e.getAmount().toPlainString() : "0.00").append(",");
                    sb.append(escapeCsv(e.getPaymentMethod())).append(",");
                    sb.append(escapeCsv(e.getDescription())).append("\n");
                }

                byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
                asyncResp.setContentLength(bytes.length);

                ServletOutputStream out = asyncResp.getOutputStream();
                out.write(bytes);
                out.flush();

                LOGGER.info("Successfully generated and streamed CSV export (" + expenses.size() + " records) for user: " + userId);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error generating CSV export in background worker thread: " + e.getMessage(), e);
                try {
                    HttpServletResponse errResp = (HttpServletResponse) asyncContext.getResponse();
                    if (!errResp.isCommitted()) {
                        errResp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to generate CSV export");
                    }
                } catch (Exception ignored) {}
            } finally {
                asyncContext.complete();
            }
        });
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "\"\"";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    @Override
    public void destroy() {
        if (fallbackExecutor != null && !fallbackExecutor.isShutdown()) {
            fallbackExecutor.shutdown();
        }
        super.destroy();
    }
}
