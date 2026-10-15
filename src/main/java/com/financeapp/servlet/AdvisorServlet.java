package com.financeapp.servlet;

import com.financeapp.dao.*;
import com.financeapp.dao.jdbc.*;
import com.financeapp.model.*;
import com.financeapp.service.GoalService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller handling advisor interactions with assigned clients:
 * - Server-side enforcement: Advisor only accesses assigned users (checks client.advisor_id == advisorId)
 * - View client's active spending plan, category envelopes, and advice history
 * - Add comments to advice items
 * - Write manual advice messages for users
 * - Send direct messages to clients
 */
@WebServlet(name = "AdvisorServlet", urlPatterns = {"/advisor/client", "/advisor/action"})
public class AdvisorServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AdvisorServlet.class.getName());

    private final UserDAO userDAO;
    private final SpendingPlanDAO spendingPlanDAO;
    private final AdviceDAO adviceDAO;
    private final MessageDAO messageDAO;
    private final NotificationDAO notificationDAO;
    private final GoalService goalService;

    public AdvisorServlet() {
        this(new JdbcUserDAO(), new JdbcSpendingPlanDAO(), new JdbcAdviceDAO(),
             new JdbcMessageDAO(), new JdbcNotificationDAO(), new GoalService());
    }

    public AdvisorServlet(UserDAO userDAO, SpendingPlanDAO spendingPlanDAO, AdviceDAO adviceDAO,
                          MessageDAO messageDAO, NotificationDAO notificationDAO, GoalService goalService) {
        this.userDAO = userDAO;
        this.spendingPlanDAO = spendingPlanDAO;
        this.adviceDAO = adviceDAO;
        this.messageDAO = messageDAO;
        this.notificationDAO = notificationDAO;
        this.goalService = goalService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        Long advisorId = (Long) session.getAttribute("userId");
        String clientIdStr = req.getParameter("id");
        if (clientIdStr == null || clientIdStr.trim().isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/advisor/dashboard");
            return;
        }

        long clientId = Long.parseLong(clientIdStr.trim());

        // STRICT SERVER-SIDE CHECK: Advisor can ONLY see assigned clients
        User client = verifyAssignedClient(advisorId, clientId);
        if (client == null) {
            LOGGER.warning(String.format("403 Forbidden: Advisor %d attempted to view unassigned client %d", advisorId, clientId));
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: You are not assigned to supervise this client account.");
            return;
        }

        String month = req.getParameter("month");
        if (month == null || !month.matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
            month = YearMonth.now().toString();
        }

        // 1. Client's Spending Plan
        Optional<SpendingPlan> planOpt = spendingPlanDAO.findByUserAndMonth(clientId, month);
        SpendingPlan plan = planOpt.orElse(null);

        // 2. Client's Advice History
        List<Advice> adviceHistory = adviceDAO.findByUserId(clientId);

        // 3. Client's Goals & Feasibility
        List<GoalFeasibilityResult> goals = goalService.evaluateUserGoals(clientId);

        // 4. Advisor-Client Conversation History
        List<Message> conversation = messageDAO.findConversation(advisorId, clientId);
        // Mark client messages as read by advisor
        messageDAO.markConversationAsRead(advisorId, clientId);

        req.setAttribute("client", client);
        req.setAttribute("plan", plan);
        req.setAttribute("adviceHistory", adviceHistory);
        req.setAttribute("goals", goals);
        req.setAttribute("conversation", conversation);
        req.setAttribute("selectedMonth", month);

        req.getRequestDispatcher("/WEB-INF/views/advisor/client.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        Long advisorId = (Long) session.getAttribute("userId");
        String clientIdStr = req.getParameter("clientId");
        if (clientIdStr == null || clientIdStr.trim().isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/advisor/dashboard");
            return;
        }

        long clientId = Long.parseLong(clientIdStr.trim());

        // STRICT SERVER-SIDE CHECK
        User client = verifyAssignedClient(advisorId, clientId);
        if (client == null) {
            LOGGER.warning(String.format("403 Forbidden: Advisor %d attempted action on unassigned client %d", advisorId, clientId));
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: You are not assigned to this client.");
            return;
        }

        String action = req.getParameter("action");
        if (action == null) {
            action = "";
        }

        try {
            switch (action) {
                case "comment_advice" -> {
                    // Add a comment to an advice item
                    long adviceId = Long.parseLong(req.getParameter("adviceId"));
                    String comment = req.getParameter("comment");
                    if (comment != null && !comment.trim().isEmpty()) {
                        Optional<Advice> adviceOpt = adviceDAO.findById(adviceId);
                        if (adviceOpt.isPresent() && adviceOpt.get().getUserId().equals(clientId)) {
                            adviceDAO.updateAdvisorComment(adviceId, comment.trim());

                            // Notify user
                            Notification notif = new Notification(
                                    null, clientId, "Advisor Comment Received",
                                    "Your financial advisor left a comment on your advice: \"" + adviceOpt.get().getTitle() + "\"",
                                    Notification.Type.ADVICE, false, LocalDateTime.now()
                            );
                            notificationDAO.create(notif);
                            session.setAttribute("flashMessage", "Advisor comment saved and client notified.");
                            session.setAttribute("flashType", "success");
                        }
                    }
                }
                case "manual_advice" -> {
                    // Write a manual advice message for a user
                    String title = req.getParameter("title");
                    String explanation = req.getParameter("explanation");
                    String comment = req.getParameter("advisorComment");
                    String planIdStr = req.getParameter("planId");
                    Long planId = (planIdStr != null && !planIdStr.trim().isEmpty()) ? Long.parseLong(planIdStr.trim()) : null;

                    Advice advice = new Advice(
                            null, clientId, planId, "ADVISOR_MANUAL",
                            (title != null ? title.trim() : "Advisor Recommendation"),
                            (explanation != null ? explanation.trim() : ""),
                            Advice.Status.NEW,
                            (comment != null ? comment.trim() : null),
                            LocalDateTime.now()
                    );
                    adviceDAO.create(advice);

                    // Notify user
                    Notification notif = new Notification(
                            null, clientId, "New Advisor Guidance: " + advice.getTitle(),
                            "Your personal financial advisor has provided custom guidance for your spending plan.",
                            Notification.Type.ADVICE, false, LocalDateTime.now()
                    );
                    notificationDAO.create(notif);

                    session.setAttribute("flashMessage", "Manual advice sent to client successfully.");
                    session.setAttribute("flashType", "success");
                }
                case "send_message" -> {
                    // Send messages to assigned user
                    String body = req.getParameter("body");
                    if (body != null && !body.trim().isEmpty()) {
                        Message msg = new Message(null, advisorId, clientId, body.trim(), false, LocalDateTime.now());
                        messageDAO.create(msg);

                        // Notify user
                        Notification notif = new Notification(
                                null, clientId, "New Message from Financial Advisor",
                                "Your advisor sent you a new direct message. Click Messages to review.",
                                Notification.Type.INFO, false, LocalDateTime.now()
                        );
                        notificationDAO.create(notif);

                        session.setAttribute("flashMessage", "Message sent to client.");
                        session.setAttribute("flashType", "success");
                    }
                }
                default -> {
                    session.setAttribute("flashMessage", "Unknown advisor action.");
                    session.setAttribute("flashType", "warning");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in AdvisorServlet: " + e.getMessage(), e);
            session.setAttribute("flashMessage", "Error processing request: " + e.getMessage());
            session.setAttribute("flashType", "danger");
        }

        resp.sendRedirect(req.getContextPath() + "/advisor/client?id=" + clientId);
    }

    /**
     * Strictly verifies server-side that the client is assigned to this advisor.
     */
    private User verifyAssignedClient(long advisorId, long clientId) {
        Optional<User> clientOpt = userDAO.findById(clientId);
        if (clientOpt.isEmpty()) {
            return null;
        }
        User client = clientOpt.get();
        if (client.getAdvisorId() == null || !client.getAdvisorId().equals(advisorId)) {
            return null;
        }
        return client;
    }
}
