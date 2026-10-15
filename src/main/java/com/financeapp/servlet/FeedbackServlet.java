package com.financeapp.servlet;

import com.financeapp.dao.FeedbackDAO;
import com.financeapp.dao.jdbc.JdbcFeedbackDAO;
import com.financeapp.model.Feedback;
import com.financeapp.model.Role;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller handling user feedback submission and admin ticket triage (listing, replying, closing).
 */
@WebServlet(name = "FeedbackServlet", urlPatterns = {"/feedback", "/admin/feedback"})
public class FeedbackServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(FeedbackServlet.class.getName());

    private final FeedbackDAO feedbackDAO;

    public FeedbackServlet() {
        this(new JdbcFeedbackDAO());
    }

    public FeedbackServlet(FeedbackDAO feedbackDAO) {
        this.feedbackDAO = feedbackDAO;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String path = req.getServletPath();
        Long userId = (Long) session.getAttribute("userId");
        Object roleObj = session.getAttribute("role");
        Role role = (roleObj instanceof Role r) ? r : Role.fromString(String.valueOf(roleObj));

        if ("/admin/feedback".equals(path)) {
            // ADMIN feedback management
            if (role != Role.ADMIN) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: Admin privileges required.");
                return;
            }

            List<Feedback> tickets = feedbackDAO.findAll();
            req.setAttribute("tickets", tickets);
            req.getRequestDispatcher("/WEB-INF/views/admin/feedback.jsp").forward(req, resp);
        } else {
            // USER feedback history & submission form
            List<Feedback> userTickets = feedbackDAO.findByUserId(userId);
            req.setAttribute("userTickets", userTickets);
            req.getRequestDispatcher("/WEB-INF/views/user/feedback.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String path = req.getServletPath();
        Long userId = (Long) session.getAttribute("userId");
        Object roleObj = session.getAttribute("role");
        Role role = (roleObj instanceof Role r) ? r : Role.fromString(String.valueOf(roleObj));

        if ("/admin/feedback".equals(path)) {
            // ADMIN ACTION: Reply and/or close ticket with confirmation messages
            if (role != Role.ADMIN) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: Admin privileges required.");
                return;
            }

            String action = req.getParameter("action");
            String ticketIdStr = req.getParameter("ticketId");
            if (ticketIdStr == null || ticketIdStr.trim().isEmpty()) {
                resp.sendRedirect(req.getContextPath() + "/admin/feedback");
                return;
            }

            long ticketId = Long.parseLong(ticketIdStr.trim());
            String adminReply = req.getParameter("adminReply");

            try {
                if ("reply_and_close".equalsIgnoreCase(action)) {
                    feedbackDAO.replyAndClose(ticketId, adminReply != null ? adminReply.trim() : "", Feedback.Status.CLOSED);
                    session.setAttribute("flashMessage", "Confirmation: Feedback ticket #" + ticketId + " replied to and closed successfully.");
                    session.setAttribute("flashType", "success");
                } else if ("close".equalsIgnoreCase(action)) {
                    feedbackDAO.updateStatus(ticketId, Feedback.Status.CLOSED);
                    session.setAttribute("flashMessage", "Confirmation: Feedback ticket #" + ticketId + " closed successfully.");
                    session.setAttribute("flashType", "success");
                } else { // reply
                    feedbackDAO.reply(ticketId, adminReply != null ? adminReply.trim() : "");
                    session.setAttribute("flashMessage", "Confirmation: Feedback ticket #" + ticketId + " replied to successfully.");
                    session.setAttribute("flashType", "success");
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error updating feedback ticket: " + e.getMessage(), e);
                session.setAttribute("flashMessage", "Error updating feedback ticket: " + e.getMessage());
                session.setAttribute("flashType", "danger");
            }

            resp.sendRedirect(req.getContextPath() + "/admin/feedback");
        } else {
            // USER ACTION: Submit feedback ticket
            String subject = req.getParameter("subject");
            String body = req.getParameter("body");

            if (subject == null || subject.trim().isEmpty() || body == null || body.trim().isEmpty()) {
                session.setAttribute("flashMessage", "Subject and message body are required.");
                session.setAttribute("flashType", "danger");
                resp.sendRedirect(req.getContextPath() + "/feedback");
                return;
            }

            try {
                Feedback feedback = new Feedback(
                        null, userId, subject.trim(), body.trim(),
                        Feedback.Status.OPEN, null, LocalDateTime.now()
                );
                long ticketId = feedbackDAO.create(feedback);
                session.setAttribute("flashMessage", "Thank you! Your feedback ticket (#" + ticketId + ") has been submitted successfully.");
                session.setAttribute("flashType", "success");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error creating feedback ticket: " + e.getMessage(), e);
                session.setAttribute("flashMessage", "Failed to submit feedback: " + e.getMessage());
                session.setAttribute("flashType", "danger");
            }

            resp.sendRedirect(req.getContextPath() + "/feedback");
        }
    }
}
