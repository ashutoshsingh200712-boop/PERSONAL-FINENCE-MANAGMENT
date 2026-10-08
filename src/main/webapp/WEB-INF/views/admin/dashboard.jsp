<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin Dashboard - System Management</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
    <style>
        body { background-color: #f1f5f9; }
        .card { border: none; border-radius: 10px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
    </style>
</head>
<body class="d-flex flex-column min-vh-100">
    <jsp:include page="/WEB-INF/views/common/navbar.jsp"/>

    <main class="container-fluid px-4 py-4">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <div>
                <h3 class="fw-bold mb-1">System Administration Console</h3>
                <p class="text-muted small mb-0">Platform user directory, security lockouts, and heuristic settings</p>
            </div>
            <button class="btn btn-primary btn-sm" data-bs-toggle="modal" data-bs-target="#createUserModal">+ Create User</button>
        </div>

        <jsp:include page="/WEB-INF/views/common/flash.jsp"/>

        <div class="row g-4 mb-4">
            <!-- Left: Users Directory -->
            <div class="col-lg-8">
                <div class="card mb-4">
                    <div class="card-header bg-white py-3 fw-bold">
                        👥 User Accounts Directory
                    </div>
                    <div class="table-responsive">
                        <table class="table table-hover align-middle mb-0 small">
                            <thead class="table-light">
                                <tr>
                                    <th>ID</th>
                                    <th>Name</th>
                                    <th>Email</th>
                                    <th>Role</th>
                                    <th>Status</th>
                                    <th>Lockout</th>
                                    <th class="text-end">Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="u" items="${users}">
                                    <tr>
                                        <td>#<c:out value="${u.id}"/></td>
                                        <td class="fw-semibold"><c:out value="${u.name}"/></td>
                                        <td><c:out value="${u.email}"/></td>
                                        <td>
                                            <span class="badge bg-${u.role == 'ADMIN' ? 'danger' : (u.role == 'ADVISOR' ? 'info' : 'secondary')}">
                                                <c:out value="${u.role}"/>
                                            </span>
                                        </td>
                                        <td>
                                            <span class="badge bg-${u.active ? 'success' : 'danger'}">
                                                ${u.active ? 'Active' : 'Inactive'}
                                            </span>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${u.locked}">
                                                    <span class="badge bg-warning text-dark">Locked (${u.failedLogins})</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="text-muted">${u.failedLogins > 0 ? u.failedLogins : 'OK'}</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td class="text-end">
                                            <!-- Toggle Active -->
                                            <form action="${pageContext.request.contextPath}/admin/action" method="post" class="d-inline">
                                                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                                <input type="hidden" name="action" value="toggleActive">
                                                <input type="hidden" name="targetUserId" value="${u.id}">
                                                <button type="submit" class="btn btn-outline-${u.active ? 'warning' : 'success'} btn-sm py-0 px-2 me-1">
                                                    ${u.active ? 'Deactivate' : 'Activate'}
                                                </button>
                                            </form>

                                            <!-- Edit User Modal Trigger -->
                                            <button class="btn btn-outline-secondary btn-sm py-0 px-2 me-1"
                                                    data-bs-toggle="modal" data-bs-target="#editUserModal${u.id}">Edit</button>

                                            <!-- Delete User -->
                                            <form action="${pageContext.request.contextPath}/admin/action" method="post" class="d-inline"
                                                  onsubmit="return confirm('Permanently delete user ${u.email}?');">
                                                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                                <input type="hidden" name="action" value="deleteUser">
                                                <input type="hidden" name="targetUserId" value="${u.id}">
                                                <button type="submit" class="btn btn-outline-danger btn-sm py-0 px-2">Delete</button>
                                            </form>
                                        </td>
                                    </tr>

                                    <!-- Edit User Modal -->
                                    <div class="modal fade" id="editUserModal${u.id}" tabindex="-1" aria-hidden="true">
                                        <div class="modal-dialog">
                                            <div class="modal-content">
                                                <form action="${pageContext.request.contextPath}/admin/action" method="post">
                                                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                                    <input type="hidden" name="action" value="updateUser">
                                                    <input type="hidden" name="targetUserId" value="${u.id}">

                                                    <div class="modal-header">
                                                        <h6 class="modal-title fw-bold">Edit User #${u.id}</h6>
                                                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                                                    </div>
                                                    <div class="modal-body small">
                                                        <div class="mb-2">
                                                            <label class="form-label fw-semibold">Name</label>
                                                            <input type="text" class="form-control form-control-sm" name="name" value="<c:out value='${u.name}'/>" required>
                                                        </div>
                                                        <div class="mb-2">
                                                            <label class="form-label fw-semibold">Email</label>
                                                            <input type="email" class="form-control form-control-sm" name="email" value="<c:out value='${u.email}'/>" required>
                                                        </div>
                                                        <div class="mb-2">
                                                            <label class="form-label fw-semibold">Role</label>
                                                            <select class="form-select form-select-sm" name="role">
                                                                <option value="USER" ${u.role == 'USER' ? 'selected' : ''}>USER</option>
                                                                <option value="ADVISOR" ${u.role == 'ADVISOR' ? 'selected' : ''}>ADVISOR</option>
                                                                <option value="ADMIN" ${u.role == 'ADMIN' ? 'selected' : ''}>ADMIN</option>
                                                            </select>
                                                        </div>
                                                        <div class="mb-2">
                                                            <label class="form-label fw-semibold">Active Status</label>
                                                            <select class="form-select form-select-sm" name="active">
                                                                <option value="true" ${u.active ? 'selected' : ''}>Active</option>
                                                                <option value="false" ${!u.active ? 'selected' : ''}>Inactive / Suspended</option>
                                                            </select>
                                                        </div>
                                                        <div class="mb-2">
                                                            <label class="form-label fw-semibold">Reset Password (leave blank to keep current)</label>
                                                            <input type="password" class="form-control form-control-sm" name="newPassword" placeholder="New password">
                                                        </div>
                                                    </div>
                                                    <div class="modal-footer">
                                                        <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Cancel</button>
                                                        <button type="submit" class="btn btn-primary btn-sm">Save Changes</button>
                                                    </div>
                                                </form>
                                            </div>
                                        </div>
                                    </div>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </div>

                <!-- Audit Logs Summary -->
                <div class="card">
                    <div class="card-header bg-white py-3 fw-bold">
                        🛡️ Recent Security &amp; Audit Trail
                    </div>
                    <div class="table-responsive">
                        <table class="table table-hover align-middle mb-0 small">
                            <thead class="table-light">
                                <tr>
                                    <th>Timestamp</th>
                                    <th>User ID</th>
                                    <th>Action</th>
                                    <th>IP Address</th>
                                    <th>Details</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="log" items="${auditLogs}">
                                    <tr>
                                        <td><c:out value="${log.createdAt}"/></td>
                                        <td><c:out value="${empty log.userId ? 'SYSTEM' : log.userId}"/></td>
                                        <td><span class="badge bg-secondary"><c:out value="${log.action}"/></span></td>
                                        <td><c:out value="${log.ipAddress}"/></td>
                                        <td class="text-truncate" style="max-width: 250px;"><c:out value="${log.details}"/></td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            <!-- Right: System Settings Form -->
            <div class="col-lg-4">
                <div class="card">
                    <div class="card-header bg-white py-3 fw-bold">
                        ⚙️ System Settings &amp; AI Weights
                    </div>
                    <div class="card-body p-3">
                        <form action="${pageContext.request.contextPath}/admin/action" method="post">
                            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="action" value="updateSettings">

                            <h6 class="fw-bold text-primary small mb-2">Default Category Allocations (%)</h6>
                            <div class="mb-2">
                                <label class="form-label small fw-semibold">Food Weight (%)</label>
                                <input type="number" step="1" class="form-control form-control-sm" name="weight_Food"
                                       value="${empty settings['weight_Food'] ? 40 : settings['weight_Food']}" required>
                            </div>
                            <div class="mb-2">
                                <label class="form-label small fw-semibold">Savings Weight (%)</label>
                                <input type="number" step="1" class="form-control form-control-sm" name="weight_Savings"
                                       value="${empty settings['weight_Savings'] ? 20 : settings['weight_Savings']}" required>
                            </div>
                            <div class="mb-2">
                                <label class="form-label small fw-semibold">Emergency Fund (%)</label>
                                <input type="number" step="1" class="form-control form-control-sm" name="weight_Emergency"
                                       value="${empty settings['weight_Emergency'] ? 15 : settings['weight_Emergency']}" required>
                            </div>
                            <div class="mb-2">
                                <label class="form-label small fw-semibold">Entertainment (%)</label>
                                <input type="number" step="1" class="form-control form-control-sm" name="weight_Entertainment"
                                       value="${empty settings['weight_Entertainment'] ? 10 : settings['weight_Entertainment']}" required>
                            </div>
                            <div class="mb-3">
                                <label class="form-label small fw-semibold">Other Discretionary (%)</label>
                                <input type="number" step="1" class="form-control form-control-sm" name="weight_Other"
                                       value="${empty settings['weight_Other'] ? 15 : settings['weight_Other']}" required>
                            </div>

                            <hr>
                            <h6 class="fw-bold text-danger small mb-2">Security &amp; Lockout Thresholds</h6>
                            <div class="mb-2">
                                <label class="form-label small fw-semibold">Max Failed Login Attempts</label>
                                <input type="number" step="1" class="form-control form-control-sm" name="max_failed_logins"
                                       value="${empty settings['max_failed_logins'] ? 5 : settings['max_failed_logins']}" required>
                            </div>
                            <div class="mb-2">
                                <label class="form-label small fw-semibold">Lockout Duration (Minutes)</label>
                                <input type="number" step="1" class="form-control form-control-sm" name="lockout_duration_minutes"
                                       value="${empty settings['lockout_duration_minutes'] ? 15 : settings['lockout_duration_minutes']}" required>
                            </div>
                            <div class="mb-2">
                                <label class="form-label small fw-semibold">Budget Alert Threshold (%)</label>
                                <input type="number" step="0.1" class="form-control form-control-sm" name="default_alert_threshold"
                                       value="${empty settings['default_alert_threshold'] ? 80.0 : settings['default_alert_threshold']}" required>
                            </div>
                            <div class="mb-3">
                                <label class="form-label small fw-semibold">Session Timeout (Minutes)</label>
                                <input type="number" step="1" class="form-control form-control-sm" name="session_timeout_minutes"
                                       value="${empty settings['session_timeout_minutes'] ? 30 : settings['session_timeout_minutes']}">
                            </div>

                            <button type="submit" class="btn btn-primary btn-sm w-100 fw-semibold py-2">Save System Settings</button>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </main>

    <!-- Modal: Create User -->
    <div class="modal fade" id="createUserModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <form action="${pageContext.request.contextPath}/admin/action" method="post">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="createUser">

                    <div class="modal-header">
                        <h6 class="modal-title fw-bold">Create New User Account</h6>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body small">
                        <div class="mb-2">
                            <label class="form-label fw-semibold">Full Name</label>
                            <input type="text" class="form-control form-control-sm" name="name" required placeholder="User Name">
                        </div>
                        <div class="mb-2">
                            <label class="form-label fw-semibold">Email Address</label>
                            <input type="email" class="form-control form-control-sm" name="email" required placeholder="user@example.com">
                        </div>
                        <div class="mb-2">
                            <label class="form-label fw-semibold">Initial Password</label>
                            <input type="password" class="form-control form-control-sm" name="password" required minlength="8" placeholder="Password123!">
                        </div>
                        <div class="mb-2">
                            <label class="form-label fw-semibold">Role</label>
                            <select class="form-select form-select-sm" name="role">
                                <option value="USER" selected>USER</option>
                                <option value="ADVISOR">ADVISOR</option>
                                <option value="ADMIN">ADMIN</option>
                            </select>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Cancel</button>
                        <button type="submit" class="btn btn-primary btn-sm">Create Account</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <footer class="text-center py-3 text-muted border-top bg-white mt-auto small">
        Personal Finance Management Platform &copy; 2026
    </footer>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
