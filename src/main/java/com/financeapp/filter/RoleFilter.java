package com.financeapp.filter;

import com.financeapp.dao.AuditDAO;
import com.financeapp.dao.jdbc.JdbcAuditDAO;
import com.financeapp.model.AuditLog;
import com.financeapp.model.Role;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.logging.Logger;

/**
 * Filter enforcing Role-Based Access Control (RBAC).
 * Enforces ADMIN for /admin/* and ADVISOR for /advisor/*.
 * On unauthorized attempts, sends 403 Forbidden and records an audit log entry.
 */
@WebFilter(filterName = "RoleFilter", urlPatterns = {"/admin/*", "/advisor/*"})
public class RoleFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(RoleFilter.class.getName());
    private final AuditDAO auditDAO = new JdbcAuditDAO();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String contextPath = httpRequest.getContextPath();
        String path = httpRequest.getRequestURI().substring(contextPath.length());
        HttpSession session = httpRequest.getSession(false);

        Long userId = (session != null) ? (Long) session.getAttribute("userId") : null;
        Object roleObj = (session != null) ? session.getAttribute("role") : null;
        Role userRole = null;
        if (roleObj instanceof Role r) {
            userRole = r;
        } else if (roleObj instanceof String str) {
            userRole = Role.fromString(str);
        }

        boolean authorized = true;
        String requiredRoleName = "";

        if (path.startsWith("/admin")) {
            requiredRoleName = "ADMIN";
            if (userRole != Role.ADMIN) {
                authorized = false;
            }
        } else if (path.startsWith("/advisor")) {
            requiredRoleName = "ADVISOR";
            if (userRole != Role.ADVISOR) {
                authorized = false;
            }
        }

        if (!authorized) {
            String clientIp = httpRequest.getRemoteAddr();
            LOGGER.warning(String.format("403 Forbidden: User %s with role %s attempted accessing %s (requires %s)",
                    userId, userRole, path, requiredRoleName));

            // Record audit log entry
            try {
                auditDAO.log(new AuditLog(
                        userId,
                        "ACCESS_DENIED",
                        "SECURITY",
                        null,
                        "Unauthorized attempt to access " + path + "; user role: " + userRole + ", required: " + requiredRoleName,
                        clientIp,
                        LocalDateTime.now()
                ));
            } catch (Exception e) {
                LOGGER.warning("Could not write audit log for access denied: " + e.getMessage());
            }

            httpRequest.setAttribute("errorMessage", "Access Denied: You do not have permission to view this resource.");
            httpRequest.setAttribute("statusCode", HttpServletResponse.SC_FORBIDDEN);
            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpRequest.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(httpRequest, httpResponse);
            return;
        }

        chain.doFilter(request, response);
    }
}
