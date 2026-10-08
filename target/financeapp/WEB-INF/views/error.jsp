<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Error - Personal Finance Management</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
</head>
<body class="bg-light d-flex flex-column min-vh-100">
    <jsp:include page="/WEB-INF/views/common/navbar.jsp"/>

    <main class="container my-auto py-5">
        <div class="row justify-content-center">
            <div class="col-md-7 col-lg-6">
                <div class="card border-0 shadow-sm text-center p-4 p-md-5">
                    <div class="mb-3">
                        <span class="badge bg-danger fs-5 px-3 py-2">
                            HTTP <c:out value="${empty statusCode ? (empty pageContext.errorData.statusCode ? 500 : pageContext.errorData.statusCode) : statusCode}"/>
                        </span>
                    </div>
                    <h2 class="fw-bold mb-3 text-dark">Something Went Wrong</h2>
                    <p class="text-secondary mb-4">
                        <c:choose>
                            <c:when test="${not empty errorMessage}">
                                <c:out value="${errorMessage}"/>
                            </c:when>
                            <c:when test="${not empty pageContext.exception}">
                                <c:out value="${pageContext.exception.message}"/>
                            </c:when>
                            <c:otherwise>
                                An unexpected server error occurred. Our administrative team has been notified.
                            </c:otherwise>
                        </c:choose>
                    </p>
                    <div>
                        <a href="${pageContext.request.contextPath}/dashboard" class="btn btn-primary px-4 me-2">Return to Dashboard</a>
                        <a href="${pageContext.request.contextPath}/login" class="btn btn-outline-secondary px-4">Log In</a>
                    </div>
                </div>
            </div>
        </div>
    </main>

    <footer class="text-center py-3 text-muted border-top bg-white mt-auto small">
        Personal Finance Management Platform &copy; 2026
    </footer>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
