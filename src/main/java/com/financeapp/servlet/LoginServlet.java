package com.financeapp.servlet;

import com.financeapp.dao.AuditDAO;
import com.financeapp.dao.SettingsDAO;
import com.financeapp.dao.UserDAO;
import com.financeapp.dao.jdbc.JdbcAuditDAO;
import com.financeapp.dao.jdbc.JdbcSettingsDAO;
import com.financeapp.dao.jdbc.JdbcUserDAO;
import com.financeapp.filter.CsrfFilter;
import com.financeapp.model.AuditLog;
import com.financeapp.model.User;
import com.financeapp.util.SecurityUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Servlet handling user authentication with brute-force lockout and session fixation defense.
 */
@WebServlet(name = "LoginServlet", urlPatterns = {"/login"})
public class LoginServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
    private static final DateTimeFormatter LOCKOUT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UserDAO userDAO = new JdbcUserDAO();
    private final SettingsDAO settingsDAO = new JdbcSettingsDAO();
    private final AuditDAO auditDAO = new JdbcAuditDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("userId") != null) {
            resp.sendRedirect(req.getContextPath() + "/dashboard");
            return;
        }
        req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email = req.getParameter("email");
        String password = req.getParameter("password");

        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            req.setAttribute("errorMessage", "Email and password are required.");
            req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
            return;
        }

        email = email.trim().toLowerCase();
        Optional<User> userOpt = userDAO.findByEmail(email);

        if (userOpt.isEmpty()) {
            req.setAttribute("errorMessage", "Invalid email or password.");
            req.setAttribute("email", email);
            req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
            return;
        }

        User user = userOpt.get();

        // 1. Check if user is active
        if (!user.isActive()) {
            req.setAttribute("errorMessage", "This account has been deactivated. Please contact an administrator.");
            req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
            return;
        }

        // 2. Check temporary lockout
        if (user.isLocked()) {
            String lockedUntilStr = user.getLockedUntil().format(LOCKOUT_FMT);
            req.setAttribute("errorMessage", "Account is temporarily locked until " + lockedUntilStr + " due to multiple failed login attempts.");
            req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
            return;
        }

        // Retrieve security lockout settings
        int maxFailedLogins = Integer.parseInt(settingsDAO.get("max_failed_logins").orElse("5"));
        int lockoutMinutes = Integer.parseInt(settingsDAO.get("lockout_duration_minutes").orElse("15"));

        // 3. Verify BCrypt password
        boolean validPassword = SecurityUtil.checkPassword(password, user.getPasswordHash());

        if (!validPassword) {
            int newFailedCount = user.getFailedLogins() + 1;
            LocalDateTime lockedUntil = null;

            if (newFailedCount >= maxFailedLogins) {
                lockedUntil = LocalDateTime.now().plusMinutes(lockoutMinutes);
                userDAO.updateFailedLogins(user.getId(), newFailedCount, lockedUntil);
                auditDAO.log(new AuditLog(
                        user.getId(),
                        "ACCOUNT_LOCKED",
                        "USER",
                        user.getId(),
                        "Account locked for " + lockoutMinutes + " minutes after " + newFailedCount + " failed attempts",
                        req.getRemoteAddr(),
                        LocalDateTime.now()
                ));
                req.setAttribute("errorMessage", "Too many failed attempts. Account has been locked for " + lockoutMinutes + " minutes.");
            } else {
                userDAO.updateFailedLogins(user.getId(), newFailedCount, null);
                int attemptsRemaining = maxFailedLogins - newFailedCount;
                req.setAttribute("errorMessage", "Invalid email or password. " + attemptsRemaining + " attempts remaining before account lockout.");
            }

            req.setAttribute("email", email);
            req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
            return;
        }

        // 4. Successful authentication: reset failed logins
        userDAO.resetFailedLogins(user.getId());

        // 5. Defend against Session Fixation: request.changeSessionId()
        req.changeSessionId();
        HttpSession session = req.getSession(true);

        session.setAttribute("userId", user.getId());
        session.setAttribute("userName", user.getName());
        session.setAttribute("userEmail", user.getEmail());
        session.setAttribute("role", user.getRole());
        session.setAttribute(CsrfFilter.CSRF_SESSION_KEY, SecurityUtil.generateCsrfToken());

        auditDAO.log(new AuditLog(
                user.getId(),
                "LOGIN_SUCCESS",
                "AUTH",
                user.getId(),
                "User logged in successfully with role " + user.getRole(),
                req.getRemoteAddr(),
                LocalDateTime.now()
        ));

        LOGGER.info("User logged in successfully: " + user.getEmail() + " (" + user.getRole() + ")");
        resp.sendRedirect(req.getContextPath() + "/dashboard");
    }
}
