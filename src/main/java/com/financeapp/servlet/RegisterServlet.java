package com.financeapp.servlet;

import com.financeapp.dao.UserDAO;
import com.financeapp.dao.jdbc.JdbcUserDAO;
import com.financeapp.exception.DuplicateEmailException;
import com.financeapp.model.RegularUser;
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
import java.util.logging.Logger;

/**
 * Servlet handling user self-registration.
 */
@WebServlet(name = "RegisterServlet", urlPatterns = {"/register"})
public class RegisterServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(RegisterServlet.class.getName());
    private final UserDAO userDAO = new JdbcUserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("userId") != null) {
            resp.sendRedirect(req.getContextPath() + "/dashboard");
            return;
        }
        req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String name = req.getParameter("name");
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");

        if (name == null || name.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            password == null || password.isEmpty()) {
            req.setAttribute("errorMessage", "All fields are required.");
            req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
            return;
        }

        if (!password.equals(confirmPassword)) {
            req.setAttribute("errorMessage", "Passwords do not match.");
            req.setAttribute("name", name);
            req.setAttribute("email", email);
            req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
            return;
        }

        if (password.length() < 8) {
            req.setAttribute("errorMessage", "Password must be at least 8 characters long.");
            req.setAttribute("name", name);
            req.setAttribute("email", email);
            req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
            return;
        }

        try {
            String hash = SecurityUtil.hashPassword(password);
            User user = new RegularUser();
            user.setName(name);
            user.setEmail(email);
            user.setPasswordHash(hash);
            user.setActive(true);
            user.setCreatedAt(LocalDateTime.now());

            userDAO.create(user);
            LOGGER.info("Successfully registered user: " + email);

            HttpSession session = req.getSession(true);
            session.setAttribute("flashMessage", "Registration successful! Please log in.");
            session.setAttribute("flashType", "success");
            resp.sendRedirect(req.getContextPath() + "/login");
        } catch (DuplicateEmailException e) {
            req.setAttribute("errorMessage", "An account with that email address already exists.");
            req.setAttribute("name", name);
            req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
        } catch (Exception e) {
            LOGGER.severe("Registration error: " + e.getMessage());
            req.setAttribute("errorMessage", "A system error occurred. Please try again.");
            req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
        }
    }
}
