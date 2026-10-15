<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sign In - FinApp AI</title>
    <!-- Google Fonts & Bootstrap 5.3 CDN -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        body {
            background: linear-gradient(135deg, #0f172a 0%, #1e1b4b 50%, #0f172a 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 1.5rem;
        }
        .auth-container {
            width: 100%;
            max-width: 440px;
        }
    </style>
</head>
<body>
    <div class="auth-container">
        <!-- Logo Header -->
        <div class="text-center mb-4">
            <div class="d-inline-flex align-items-center justify-content-center mb-2" style="width: 48px; height: 48px; border-radius: 12px; background: var(--primary-gradient); box-shadow: 0 4px 14px rgba(79, 70, 229, 0.4);">
                <span class="text-white fs-4 fw-bold">◆</span>
            </div>
            <h4 class="fw-bold text-white mb-1">Welcome Back</h4>
            <p class="text-light small opacity-75">Sign in to your Personal Finance Management Platform</p>
        </div>

        <!-- Flash Message Area -->
        <jsp:include page="/WEB-INF/views/common/flash.jsp"/>

        <c:if test="${not empty errorMessage}">
            <div class="alert alert-danger alert-dismissible fade show small" role="alert">
                <c:out value="${errorMessage}"/>
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        </c:if>

        <!-- Login Card -->
        <div class="card card-fintech shadow-lg p-4 p-md-4">
            <form action="${pageContext.request.contextPath}/login" method="post" class="needs-validation">
                <!-- CSRF Token Hidden Field -->
                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">

                <div class="mb-3">
                    <label for="email" class="form-label-clean">Email Address</label>
                    <input type="email" class="form-control form-control-clean" id="email" name="email" 
                           value="<c:out value='${email}'/>" placeholder="name@example.com" required autofocus>
                    <div class="invalid-feedback small">Please provide a valid registered email address.</div>
                </div>

                <div class="mb-3">
                    <div class="d-flex justify-content-between align-items-center">
                        <label for="password" class="form-label-clean">Password</label>
                    </div>
                    <input type="password" class="form-control form-control-clean" id="password" name="password" 
                           placeholder="••••••••" required>
                </div>

                <button type="submit" class="btn btn-indigo w-100 py-2 fw-semibold mt-2">Sign In to Dashboard →</button>
            </form>

            <div class="text-center mt-3 pt-3 border-top small text-muted">
                Don't have an account yet? 
                <a href="${pageContext.request.contextPath}/register" class="fw-semibold text-decoration-none" style="color: var(--primary-indigo);">Create one now</a>
            </div>

            <!-- Seed Account Quick Selectors -->
            <div class="mt-4 p-3 rounded bg-light border small">
                <div class="fw-bold text-dark mb-2" style="font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.05em;">
                    🔑 Seed Accounts (Click to Fill):
                </div>
                <div class="d-flex flex-column gap-1">
                    <a href="#" class="text-decoration-none d-flex justify-content-between align-items-center text-secondary py-1 px-2 rounded hover-bg" onclick="document.getElementById('email').value='john@example.com'; document.getElementById('password').value='UserPassword123!'; return false;">
                        <span><strong>User:</strong> john@example.com</span>
                        <span class="badge badge-role-user">USER</span>
                    </a>
                    <a href="#" class="text-decoration-none d-flex justify-content-between align-items-center text-secondary py-1 px-2 rounded hover-bg" onclick="document.getElementById('email').value='advisor@financeapp.com'; document.getElementById('password').value='AdvisorPassword123!'; return false;">
                        <span><strong>Advisor:</strong> advisor@financeapp.com</span>
                        <span class="badge badge-role-advisor">ADVISOR</span>
                    </a>
                    <a href="#" class="text-decoration-none d-flex justify-content-between align-items-center text-secondary py-1 px-2 rounded hover-bg" onclick="document.getElementById('email').value='admin@financeapp.com'; document.getElementById('password').value='AdminPassword123!'; return false;">
                        <span><strong>Admin:</strong> admin@financeapp.com</span>
                        <span class="badge badge-role-admin">ADMIN</span>
                    </a>
                </div>
            </div>
        </div>
    </div>

    <!-- Global Toast Container -->
    <div class="toast-container-custom"></div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
    <script src="${pageContext.request.contextPath}/js/charts.js"></script>
</body>
</html>
