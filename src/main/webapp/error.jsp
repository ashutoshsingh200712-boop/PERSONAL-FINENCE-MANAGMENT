<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" %>
<%@ page import="java.util.logging.Logger, java.util.logging.Level" %>
<%
    Logger logger = Logger.getLogger("com.financeapp.web.ErrorLog");
    Throwable ex = (Throwable) request.getAttribute("jakarta.servlet.error.exception");
    Integer status = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
    String uri = (String) request.getAttribute("jakarta.servlet.error.request_uri");

    if (ex != null) {
        logger.log(Level.SEVERE, "Handled Web Application Error on URI [" + uri + "], Status: " + status + " - " + ex.getMessage(), ex);
    } else {
        logger.log(Level.WARNING, "Handled HTTP Error status [" + status + "] on URI [" + uri + "]");
    }

    request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
%>
