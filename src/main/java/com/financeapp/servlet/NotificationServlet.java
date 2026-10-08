package com.financeapp.servlet;

import com.financeapp.dao.NotificationDAO;
import com.financeapp.dao.jdbc.JdbcNotificationDAO;
import com.financeapp.model.Notification;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Handles actions on user notifications (e.g. marking notifications as read).
 */
@WebServlet(name = "NotificationServlet", urlPatterns = {"/notifications"})
public class NotificationServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(NotificationServlet.class.getName());
    private final NotificationDAO notificationDAO;

    public NotificationServlet() {
        this(new JdbcNotificationDAO());
    }

    public NotificationServlet(NotificationDAO notificationDAO) {
        this.notificationDAO = notificationDAO;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        Long userId = (Long) session.getAttribute("userId");
        String action = req.getParameter("action");

        if ("markRead".equalsIgnoreCase(action)) {
            String idStr = req.getParameter("id");
            if (idStr != null && !idStr.trim().isEmpty()) {
                try {
                    long notifId = Long.parseLong(idStr.trim());
                    Optional<Notification> opt = notificationDAO.findById(notifId);
                    if (opt.isPresent() && opt.get().getUserId().equals(userId)) {
                        notificationDAO.markAsRead(notifId);
                    }
                } catch (NumberFormatException e) {
                    LOGGER.warning("Invalid notification ID: " + idStr);
                }
            }
        } else if ("markAllRead".equalsIgnoreCase(action)) {
            List<Notification> unread = notificationDAO.findUnreadByUserId(userId);
            for (Notification n : unread) {
                notificationDAO.markAsRead(n.getId());
            }
        }

        // Redirect back to dashboard or referring page
        String referer = req.getHeader("Referer");
        if (referer != null && referer.contains(req.getContextPath())) {
            resp.sendRedirect(referer);
        } else {
            resp.sendRedirect(req.getContextPath() + "/dashboard");
        }
    }
}
