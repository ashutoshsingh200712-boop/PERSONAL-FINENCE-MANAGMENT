package com.financeapp.filter;

import com.financeapp.util.SecurityUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Filter enforcing Cross-Site Request Forgery (CSRF) protection on all POST requests.
 */
@WebFilter(filterName = "CsrfFilter", urlPatterns = {"/*"})
public class CsrfFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(CsrfFilter.class.getName());
    public static final String CSRF_SESSION_KEY = "csrfToken";
    public static final String CSRF_PARAM_NAME = "csrfToken";
    public static final String CSRF_HEADER_NAME = "X-CSRF-Token";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Ensure session has a valid CSRF token
        HttpSession session = httpRequest.getSession(true);
        String sessionToken = (String) session.getAttribute(CSRF_SESSION_KEY);
        if (sessionToken == null) {
            sessionToken = SecurityUtil.generateCsrfToken();
            session.setAttribute(CSRF_SESSION_KEY, sessionToken);
        }

        // On every POST request, validate the token
        if ("POST".equalsIgnoreCase(httpRequest.getMethod())) {
            String submittedToken = httpRequest.getParameter(CSRF_PARAM_NAME);
            if (submittedToken == null || submittedToken.isEmpty()) {
                submittedToken = httpRequest.getHeader(CSRF_HEADER_NAME);
            }

            if (submittedToken == null || !submittedToken.equals(sessionToken)) {
                LOGGER.warning("CSRF token verification failed for URI: " + httpRequest.getRequestURI());
                httpRequest.setAttribute("errorMessage", "Security verification failed: Invalid or missing CSRF token.");
                httpRequest.setAttribute("statusCode", HttpServletResponse.SC_FORBIDDEN);
                httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                httpRequest.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(httpRequest, httpResponse);
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
