package com.financeapp.servlet;

import com.financeapp.dao.AuditDAO;
import com.financeapp.dao.SettingsDAO;
import com.financeapp.dao.UserDAO;
import com.financeapp.exception.DuplicateEmailException;
import com.financeapp.model.RegularUser;
import com.financeapp.model.Role;
import com.financeapp.model.User;
import com.financeapp.util.SecurityUtil;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Web Layer Servlet Controller Tests")
class ServletFlowTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @Mock
    private RequestDispatcher dispatcher;

    @Mock
    private UserDAO userDAO;

    @Mock
    private SettingsDAO settingsDAO;

    @Mock
    private AuditDAO auditDAO;

    @BeforeEach
    void setUp() {
    }

    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    @DisplayName("LoginServlet: Successful login calls changeSessionId and sets session userId and role")
    void testLoginServletSuccess() throws Exception {
        LoginServlet servlet = new LoginServlet();
        injectField(servlet, "userDAO", userDAO);
        injectField(servlet, "settingsDAO", settingsDAO);
        injectField(servlet, "auditDAO", auditDAO);

        String email = "john@example.com";
        String password = "UserPassword123!";
        String hash = SecurityUtil.hashPassword(password);

        RegularUser user = new RegularUser(3L, "John Doe", email, hash, true, 0, null, null, LocalDateTime.now());

        when(request.getParameter("email")).thenReturn(email);
        when(request.getParameter("password")).thenReturn(password);
        when(userDAO.findByEmail(email)).thenReturn(Optional.of(user));
        when(settingsDAO.get("max_failed_logins")).thenReturn(Optional.of("5"));
        when(settingsDAO.get("lockout_duration_minutes")).thenReturn(Optional.of("15"));
        when(request.getSession(true)).thenReturn(session);
        when(request.getContextPath()).thenReturn("/financeapp");

        servlet.doPost(request, response);

        verify(userDAO).resetFailedLogins(3L);
        verify(request).changeSessionId();
        verify(session).setAttribute("userId", 3L);
        verify(session).setAttribute("role", Role.USER);
        verify(response).sendRedirect("/financeapp/dashboard");
    }

    @Test
    @DisplayName("LoginServlet: Exceeding max failed logins triggers temporary lockout")
    void testLoginServletLockout() throws Exception {
        LoginServlet servlet = new LoginServlet();
        injectField(servlet, "userDAO", userDAO);
        injectField(servlet, "settingsDAO", settingsDAO);
        injectField(servlet, "auditDAO", auditDAO);

        String email = "john@example.com";
        String wrongPassword = "WrongPassword999!";
        String hash = SecurityUtil.hashPassword("CorrectPassword123!");

        // User already has 4 failed attempts; max is 5
        RegularUser user = new RegularUser(3L, "John Doe", email, hash, true, 4, null, null, LocalDateTime.now());

        when(request.getParameter("email")).thenReturn(email);
        when(request.getParameter("password")).thenReturn(wrongPassword);
        when(userDAO.findByEmail(email)).thenReturn(Optional.of(user));
        when(settingsDAO.get("max_failed_logins")).thenReturn(Optional.of("5"));
        when(settingsDAO.get("lockout_duration_minutes")).thenReturn(Optional.of("15"));
        when(request.getRequestDispatcher("/WEB-INF/views/auth/login.jsp")).thenReturn(dispatcher);

        servlet.doPost(request, response);

        // Should lock user account
        verify(userDAO).updateFailedLogins(eq(3L), eq(5), notNull());
        verify(request).setAttribute(eq("errorMessage"), contains("locked for 15 minutes"));
        verify(dispatcher).forward(request, response);
    }

    @Test
    @DisplayName("RegisterServlet: Handles DuplicateEmailException and forwards to registration view")
    void testRegisterServletDuplicateEmail() throws Exception {
        RegisterServlet servlet = new RegisterServlet();
        injectField(servlet, "userDAO", userDAO);

        when(request.getParameter("name")).thenReturn("Jane Doe");
        when(request.getParameter("email")).thenReturn("duplicate@example.com");
        when(request.getParameter("password")).thenReturn("Password123!");
        when(request.getParameter("confirmPassword")).thenReturn("Password123!");
        when(userDAO.create(any(User.class))).thenThrow(new DuplicateEmailException("Email exists"));
        when(request.getRequestDispatcher("/WEB-INF/views/auth/register.jsp")).thenReturn(dispatcher);

        servlet.doPost(request, response);

        verify(request).setAttribute(eq("errorMessage"), contains("already exists"));
        verify(dispatcher).forward(request, response);
    }

    @Test
    @DisplayName("DashboardServlet: /dashboard redirects by role (ADMIN to /admin/dashboard, USER to /user/dashboard)")
    void testDashboardServletRoleRedirect() throws Exception {
        DashboardServlet servlet = new DashboardServlet();

        // 1. Test ADMIN redirect
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(1L);
        when(session.getAttribute("role")).thenReturn(Role.ADMIN);
        when(request.getServletPath()).thenReturn("/dashboard");
        when(request.getContextPath()).thenReturn("/financeapp");
        when(request.getQueryString()).thenReturn(null);

        servlet.doGet(request, response);

        verify(response).sendRedirect("/financeapp/admin/dashboard");

        // 2. Test USER redirect
        reset(response);
        when(session.getAttribute("role")).thenReturn(Role.USER);

        servlet.doGet(request, response);

        verify(response).sendRedirect("/financeapp/user/dashboard");

        // 3. Test ADVISOR redirect
        reset(response);
        when(session.getAttribute("role")).thenReturn(Role.ADVISOR);

        servlet.doGet(request, response);

        verify(response).sendRedirect("/financeapp/advisor/dashboard");
    }

    @Test
    @DisplayName("LogoutServlet: Invalidates session, sets flash message, and redirects to /login")
    void testLogoutServletFlow() throws Exception {
        LogoutServlet servlet = new LogoutServlet();

        HttpSession oldSession = mock(HttpSession.class);
        HttpSession newSession = mock(HttpSession.class);

        when(request.getSession(false)).thenReturn(oldSession);
        when(request.getSession(true)).thenReturn(newSession);
        when(request.getContextPath()).thenReturn("/financeapp");

        servlet.doPost(request, response);

        verify(oldSession).invalidate();
        verify(newSession).setAttribute(eq("flashMessage"), contains("logged out successfully"));
        verify(response).sendRedirect("/financeapp/login");
    }
}
