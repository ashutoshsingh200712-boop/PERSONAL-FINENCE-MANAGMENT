package com.financeapp.servlet;

import com.financeapp.dao.AuditDAO;
import com.financeapp.dao.SettingsDAO;
import com.financeapp.dao.UserDAO;
import com.financeapp.dao.jdbc.JdbcAuditDAO;
import com.financeapp.dao.jdbc.JdbcSettingsDAO;
import com.financeapp.dao.jdbc.JdbcUserDAO;
import com.financeapp.model.AuditLog;
import com.financeapp.model.Role;
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
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Controller servlet for administrator management actions (user maintenance, system settings).
 */
@WebServlet(name = "AdminServlet", urlPatterns = {"/admin/action"})
public class AdminServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AdminServlet.class.getName());

    private final UserDAO userDAO = new JdbcUserDAO();
    private final SettingsDAO settingsDAO = new JdbcSettingsDAO();
    private final AuditDAO auditDAO = new JdbcAuditDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Long adminId = (Long) session.getAttribute("userId");
        String action = req.getParameter("action");
        if (action == null) {
            action = "";
        }

        try {
            switch (action) {
                case "createUser" -> handleCreateUser(req, adminId);
                case "updateUser" -> handleUpdateUser(req, adminId);
                case "toggleActive" -> handleToggleActive(req, adminId);
                case "deleteUser" -> handleDeleteUser(req, adminId);
                case "updateSettings" -> handleUpdateSettings(req, adminId);
                default -> LOGGER.warning("Unknown admin action: " + action);
            }
        } catch (Exception e) {
            LOGGER.severe("Admin action [" + action + "] failed: " + e.getMessage());
            session.setAttribute("flashMessage", "Action failed: " + e.getMessage());
            session.setAttribute("flashType", "danger");
            resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
            return;
        }

        resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
    }

    private void handleCreateUser(HttpServletRequest req, Long adminId) {
        String name = req.getParameter("name");
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String roleStr = req.getParameter("role");

        Role role = Role.fromString(roleStr);
        String hash = SecurityUtil.hashPassword(password != null ? password : "Password123!");

        User newUser = User.createWithRole(role);
        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setPasswordHash(hash);
        newUser.setActive(true);

        long id = userDAO.create(newUser);
        auditDAO.log(new AuditLog(adminId, "ADMIN_CREATE_USER", "USER", id, "Admin created user " + email + " with role " + role, req.getRemoteAddr(), LocalDateTime.now()));

        req.getSession().setAttribute("flashMessage", "User account created successfully (ID: " + id + ").");
        req.getSession().setAttribute("flashType", "success");
    }

    private void handleUpdateUser(HttpServletRequest req, Long adminId) {
        long targetId = Long.parseLong(req.getParameter("targetUserId"));
        Optional<User> userOpt = userDAO.findById(targetId);
        if (userOpt.isPresent()) {
            User u = userOpt.get();
            u.setName(req.getParameter("name"));
            u.setEmail(req.getParameter("email"));
            u.setRole(Role.fromString(req.getParameter("role")));
            u.setActive("true".equalsIgnoreCase(req.getParameter("active")));

            String newPass = req.getParameter("newPassword");
            if (newPass != null && !newPass.trim().isEmpty()) {
                u.setPasswordHash(SecurityUtil.hashPassword(newPass.trim()));
            }

            userDAO.update(u);
            auditDAO.log(new AuditLog(adminId, "ADMIN_UPDATE_USER", "USER", targetId, "Admin updated user details for " + u.getEmail(), req.getRemoteAddr(), LocalDateTime.now()));
            req.getSession().setAttribute("flashMessage", "User updated successfully.");
            req.getSession().setAttribute("flashType", "success");
        }
    }

    private void handleToggleActive(HttpServletRequest req, Long adminId) {
        long targetId = Long.parseLong(req.getParameter("targetUserId"));
        Optional<User> userOpt = userDAO.findById(targetId);
        if (userOpt.isPresent()) {
            User u = userOpt.get();
            boolean newStatus = !u.isActive();
            u.setActive(newStatus);
            userDAO.update(u);
            auditDAO.log(new AuditLog(adminId, newStatus ? "USER_ACTIVATED" : "USER_DEACTIVATED", "USER", targetId, "User active status set to " + newStatus, req.getRemoteAddr(), LocalDateTime.now()));
            req.getSession().setAttribute("flashMessage", "User status changed to " + (newStatus ? "Active" : "Deactivated") + ".");
            req.getSession().setAttribute("flashType", "info");
        }
    }

    private void handleDeleteUser(HttpServletRequest req, Long adminId) {
        long targetId = Long.parseLong(req.getParameter("targetUserId"));
        userDAO.delete(targetId);
        auditDAO.log(new AuditLog(adminId, "ADMIN_DELETE_USER", "USER", targetId, "Admin deleted user account", req.getRemoteAddr(), LocalDateTime.now()));
        req.getSession().setAttribute("flashMessage", "User account deleted successfully.");
        req.getSession().setAttribute("flashType", "warning");
    }

    private void handleUpdateSettings(HttpServletRequest req, Long adminId) {
        // AI Category Weights
        updateSettingIfPresent(req, "weight_Food");
        updateSettingIfPresent(req, "weight_Savings");
        updateSettingIfPresent(req, "weight_Emergency");
        updateSettingIfPresent(req, "weight_Entertainment");
        updateSettingIfPresent(req, "weight_Other");

        // Thresholds and Lockouts
        updateSettingIfPresent(req, "max_failed_logins");
        updateSettingIfPresent(req, "lockout_duration_minutes");
        updateSettingIfPresent(req, "default_alert_threshold");
        updateSettingIfPresent(req, "session_timeout_minutes");

        auditDAO.log(new AuditLog(adminId, "SETTINGS_UPDATED", "SYSTEM_SETTINGS", null, "Admin updated system configurations", req.getRemoteAddr(), LocalDateTime.now()));
        req.getSession().setAttribute("flashMessage", "System settings updated successfully.");
        req.getSession().setAttribute("flashType", "success");
    }

    private void updateSettingIfPresent(HttpServletRequest req, String key) {
        String val = req.getParameter(key);
        if (val != null && !val.trim().isEmpty()) {
            settingsDAO.set(key, val.trim());
        }
    }
}
