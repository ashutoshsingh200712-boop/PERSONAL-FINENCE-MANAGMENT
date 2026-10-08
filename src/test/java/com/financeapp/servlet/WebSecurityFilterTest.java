package com.financeapp.servlet;

import com.financeapp.filter.AuthFilter;
import com.financeapp.filter.CsrfFilter;
import com.financeapp.filter.RoleFilter;
import com.financeapp.model.Role;
import jakarta.servlet.FilterChain;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Web Layer Security and Filters Unit Tests")
class WebSecurityFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @Mock
    private FilterChain chain;

    @Mock
    private RequestDispatcher dispatcher;

    private AuthFilter authFilter;
    private RoleFilter roleFilter;
    private CsrfFilter csrfFilter;

    @BeforeEach
    void setUp() {
        authFilter = new AuthFilter();
        roleFilter = new RoleFilter();
        csrfFilter = new CsrfFilter();
    }

    @Test
    @DisplayName("AuthFilter: Should allow public URLs without active session")
    void testAuthFilterPublicUrlAllowed() throws Exception {
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/login");

        authFilter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        verify(response, never()).sendRedirect(anyString());
    }

    @Test
    @DisplayName("AuthFilter: Should redirect unauthenticated user from protected URL to /login")
    void testAuthFilterProtectedUrlRedirects() throws Exception {
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/dashboard");
        when(request.getSession(false)).thenReturn(null);

        authFilter.doFilter(request, response, chain);

        verify(response, times(1)).sendRedirect("/login");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("RoleFilter: Should block non-admin from /admin/* with 403 Forbidden")
    void testRoleFilterBlocksNonAdmin() throws Exception {
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/admin/dashboard");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(10L);
        when(session.getAttribute("role")).thenReturn(Role.USER);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRequestDispatcher("/WEB-INF/views/error.jsp")).thenReturn(dispatcher);

        roleFilter.doFilter(request, response, chain);

        verify(response, times(1)).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(dispatcher, times(1)).forward(request, response);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("RoleFilter: Should allow ADMIN to access /admin/dashboard")
    void testRoleFilterAllowsAdmin() throws Exception {
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/admin/dashboard");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(1L);
        when(session.getAttribute("role")).thenReturn(Role.ADMIN);

        roleFilter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    @DisplayName("CsrfFilter: Should block POST request without valid CSRF token with 403")
    void testCsrfFilterBlocksInvalidToken() throws Exception {
        when(request.getSession(true)).thenReturn(session);
        when(session.getAttribute(CsrfFilter.CSRF_SESSION_KEY)).thenReturn("valid-session-token");
        when(request.getMethod()).thenReturn("POST");
        when(request.getParameter(CsrfFilter.CSRF_PARAM_NAME)).thenReturn("invalid-or-missing");
        when(request.getRequestDispatcher("/WEB-INF/views/error.jsp")).thenReturn(dispatcher);

        csrfFilter.doFilter(request, response, chain);

        verify(response, times(1)).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(dispatcher, times(1)).forward(request, response);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("CsrfFilter: Should allow POST request with matching token")
    void testCsrfFilterAllowsMatchingToken() throws Exception {
        String token = "my-secret-csrf-token";
        when(request.getSession(true)).thenReturn(session);
        when(session.getAttribute(CsrfFilter.CSRF_SESSION_KEY)).thenReturn(token);
        when(request.getMethod()).thenReturn("POST");
        when(request.getParameter(CsrfFilter.CSRF_PARAM_NAME)).thenReturn(token);

        csrfFilter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }
}
