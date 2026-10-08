<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Create Account - Personal Finance Management</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
    <style>
        body { background-color: #f8fafc; }
        .auth-card { max-width: 480px; margin: 3.5rem auto; }
    </style>
</head>
<body class="d-flex flex-column min-vh-100">
    <jsp:include page="/WEB-INF/views/common/navbar.jsp"/>

    <main class="container my-auto">
        <div class="card auth-card shadow-sm border-0 rounded-3">
            <div class="card-body p-4 p-md-5">
                <div class="text-center mb-4">
                    <h3 class="fw-bold text-dark">Create Account</h3>
                    <p class="text-muted small">Start managing your personal finances with AI Smart Planning</p>
                </div>

                <jsp:include page="/WEB-INF/views/common/flash.jsp"/>

                <c:if test="${not empty errorMessage}">
                    <div class="alert alert-danger alert-dismissible fade show small" role="alert">
                        <c:out value="${errorMessage}"/>
                        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                    </div>
                </c:if>

                <form action="${pageContext.request.contextPath}/register" method="post">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">

                    <div class="mb-3">
                        <label for="name" class="form-label small fw-semibold">Full Name</label>
                        <input type="text" class="form-control" id="name" name="name"
                               value="<c:out value='${name}'/>" required autofocus placeholder="John Doe">
                    </div>

                    <div class="mb-3">
                        <label for="email" class="form-label small fw-semibold">Email Address</label>
                        <input type="email" class="form-control" id="email" name="email"
                               value="<c:out value='${email}'/>" required placeholder="name@example.com">
                    </div>

                    <div class="mb-3">
                        <label for="password" class="form-label small fw-semibold">Password</label>
                        <input type="password" class="form-control" id="password" name="password"
                               required minlength="8" placeholder="At least 8 characters">
                    </div>

                    <div class="mb-3">
                        <label for="confirmPassword" class="form-label small fw-semibold">Confirm Password</label>
                        <input type="password" class="form-control" id="confirmPassword" name="confirmPassword"
                               required minlength="8" placeholder="Re-enter password">
                    </div>

                    <button type="submit" class="btn btn-primary w-100 py-2 fw-semibold mt-2">Sign Up</button>
                </form>

                <div class="text-center mt-4 small text-muted">
                    Already registered?
                    <a href="${pageContext.request.contextPath}/login" class="text-decoration-none fw-semibold">Log in here</a>
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
