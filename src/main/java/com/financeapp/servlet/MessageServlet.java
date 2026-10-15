package com.financeapp.servlet;

import com.financeapp.dao.MessageDAO;
import com.financeapp.dao.UserDAO;
import com.financeapp.dao.jdbc.JdbcMessageDAO;
import com.financeapp.dao.jdbc.JdbcUserDAO;
import com.financeapp.model.Message;
import com.financeapp.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Servlet allowing users to view and exchange direct messages with their assigned financial advisor.
 */
@WebServlet(name = "MessageServlet", urlPatterns = {"/messages"})
public class MessageServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(MessageServlet.class.getName());

    private final MessageDAO messageDAO;
    private final UserDAO userDAO;

    public MessageServlet() {
        this(new JdbcMessageDAO(), new JdbcUserDAO());
    }

    public MessageServlet(MessageDAO messageDAO, UserDAO userDAO) {
        this.messageDAO = messageDAO;
        this.userDAO = userDAO;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        Long userId = (Long) session.getAttribute("userId");
        Optional<User> userOpt = userDAO.findById(userId);
        if (userOpt.isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/logout");
            return;
        }

        User user = userOpt.get();
        Long advisorId = user.getAdvisorId();

        List<Message> conversation = List.of();
        User advisor = null;

        if (advisorId != null) {
            advisor = userDAO.findById(advisorId).orElse(null);
            conversation = messageDAO.findConversation(userId, advisorId);
            // Mark unread messages from advisor as read
            messageDAO.markConversationAsRead(userId, advisorId);
        }

        req.setAttribute("user", user);
        req.setAttribute("advisor", advisor);
        req.setAttribute("conversation", conversation);

        req.getRequestDispatcher("/WEB-INF/views/user/messages.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        Long userId = (Long) session.getAttribute("userId");
        Optional<User> userOpt = userDAO.findById(userId);
        if (userOpt.isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/logout");
            return;
        }

        Long advisorId = userOpt.get().getAdvisorId();
        if (advisorId == null) {
            session.setAttribute("flashMessage", "You do not have an assigned advisor to message.");
            session.setAttribute("flashType", "warning");
            resp.sendRedirect(req.getContextPath() + "/messages");
            return;
        }

        String body = req.getParameter("body");
        if (body != null && !body.trim().isEmpty()) {
            Message msg = new Message(null, userId, advisorId, body.trim(), false, LocalDateTime.now());
            messageDAO.create(msg);
            session.setAttribute("flashMessage", "Message sent to your advisor.");
            session.setAttribute("flashType", "success");
        }

        resp.sendRedirect(req.getContextPath() + "/messages");
    }
}
