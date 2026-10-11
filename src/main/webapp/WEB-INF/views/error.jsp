<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Error - FinApp AI</title>
    <!-- Google Fonts & Bootstrap 5.3 CDN -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="d-flex flex-column min-vh-100" style="background-color: var(--bg-soft);">
    <main class="container my-auto py-5">
        <div class="row justify-content-center">
            <div class="col-md-7 col-lg-5">
                <div class="card card-fintech text-center p-4 p-md-5 shadow-sm border-0">
                    <div class="mb-3">
                        <span class="badge bg-danger-subtle text-danger border fs-6 px-3 py-2 fw-bold">
                            HTTP <c:out value="${empty statusCode ? (empty pageContext.errorData.statusCode ? 500 : pageContext.errorData.statusCode) : statusCode}"/>
                        </span>
                    </div>
                    <h3 class="fw-bold mb-2 text-dark">Something Went Wrong</h3>
                    <p class="text-secondary small mb-4">
                        <c:choose>
                            <c:when test="${not empty errorMessage}">
                                <c:out value="${errorMessage}"/>
                            </c:when>
                            <c:when test="${not empty pageContext.exception}">
                                <c:out value="${pageContext.exception.message}"/>
                            </c:when>
                            <c:otherwise>
                                An unexpected server exception occurred. The incident has been recorded in audit logs.
                            </c:otherwise>
                        </c:choose>
                    </p>
                    <div class="d-flex justify-content-center gap-2">
                        <a href="${pageContext.request.contextPath}/dashboard" class="btn btn-indigo btn-sm px-4">Dashboard</a>
                        <a href="${pageContext.request.contextPath}/login" class="btn btn-outline-secondary btn-sm px-4">Log In</a>
                    </div>
                </div>
            </div>
        </div>
    </main>

    <footer class="app-footer text-center py-3 text-muted border-top bg-white mt-auto small">
        Personal Finance Management Platform &copy; 2026 • FinApp AI
    </footer>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
