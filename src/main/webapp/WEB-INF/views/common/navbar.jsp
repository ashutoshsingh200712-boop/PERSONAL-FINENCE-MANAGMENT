<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<nav class="navbar navbar-expand-lg navbar-dark bg-dark shadow-sm">
    <div class="container-fluid px-4">
        <a class="navbar-brand fw-bold text-primary" href="${pageContext.request.contextPath}/dashboard">
            <span class="text-white">◆ FinApp</span> AI
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarMain">
            <span class="navbar-toggler-icon"></span>
        </button>

        <div class="collapse navbar-collapse" id="navbarMain">
            <ul class="navbar-nav me-auto mb-2 mb-lg-0">
                <li class="nav-item">
                    <a class="nav-link active" href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
                </li>
                <c:if test="${sessionScope.role == 'ADMIN'}">
                    <li class="nav-item">
                        <a class="nav-link text-warning" href="${pageContext.request.contextPath}/admin/dashboard">Admin Panel</a>
                    </li>
                </c:if>
                <c:if test="${sessionScope.role == 'ADVISOR'}">
                    <li class="nav-item">
                        <a class="nav-link text-info" href="${pageContext.request.contextPath}/advisor/dashboard">Advisor Panel</a>
                    </li>
                </c:if>
            </ul>

            <ul class="navbar-nav ms-auto align-items-center">
                <c:if test="${not empty sessionScope.userName}">
                    <li class="nav-item me-3 text-light">
                        <span class="badge bg-secondary me-2">${sessionScope.role}</span>
                        <span class="fw-semibold">${sessionScope.userName}</span>
                    </li>
                    <li class="nav-item">
                        <form action="${pageContext.request.contextPath}/logout" method="post" class="d-inline">
                            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                            <button type="submit" class="btn btn-outline-light btn-sm">Log Out</button>
                        </form>
                    </li>
                </c:if>
                <c:if test="${empty sessionScope.userName}">
                    <li class="nav-item">
                        <a class="btn btn-outline-light btn-sm me-2" href="${pageContext.request.contextPath}/login">Log In</a>
                    </li>
                    <li class="nav-item">
                        <a class="btn btn-primary btn-sm" href="${pageContext.request.contextPath}/register">Register</a>
                    </li>
                </c:if>
            </ul>
        </div>
    </div>
</nav>
