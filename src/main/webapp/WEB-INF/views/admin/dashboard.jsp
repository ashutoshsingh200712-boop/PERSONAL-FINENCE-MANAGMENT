<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.fmt.core" %>

<c:set var="pageTitle" value="System Administration Console" />
<c:set var="activeNav" value="users" />

<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <div>
        <h4 class="fw-bold mb-1">System Administration Console</h4>
        <p class="text-muted small mb-0">Platform user directory, security lockouts, and heuristic AI allocation weights</p>
    </div>
    <button class="btn btn-indigo btn-sm d-flex align-items-center gap-1 shadow-sm" data-bs-toggle="modal" data-bs-target="#createUserModal">
        <span>+</span>
        <span>Create User Account</span>
    </button>
</div>

<!-- Section: User Directory & System Settings -->
<div class="row g-4 mb-4">
    <!-- Left: Users Directory -->
    <div class="col-lg-8" id="users">
        <div class="card card-fintech mb-4">
            <div class="card-header-clean">
                <h6><span>👥</span> User Accounts Directory</h6>
                <span class="small text-muted">Total: <c:out value="${users.size()}"/> accounts</span>
            </div>
            <div class="table-responsive">
                <table class="table table-fintech">
                    <thead>
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
                        <c:choose>
                            <c:when test="${empty users}">
                                <tr>
                                    <td colspan="7">
                                        <div class="empty-state py-4">
                                            <div class="empty-state-icon">👥</div>
                                            <h6>No User Accounts Found</h6>
                                            <p class="text-muted small">No registered accounts currently exist in the database.</p>
                                        </div>
                                    </td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <c:forEach var="u" items="${users}">
                                    <tr>
                                        <td class="text-muted small">#<c:out value="${u.id}"/></td>
                                        <td class="fw-bold text-dark"><c:out value="${u.name}"/></td>
                                        <td class="text-secondary small"><c:out value="${u.email}"/></td>
                                        <td>
                                            <span class="badge ${u.role == 'ADMIN' ? 'badge-role-admin' : (u.role == 'ADVISOR' ? 'badge-role-advisor' : 'badge-role-user')} user-role-badge">
                                                <c:out value="${u.role}"/>
                                            </span>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${u.active}">
                                                    <span class="badge bg-success-subtle text-success border">Active</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge bg-danger-subtle text-danger border">Suspended</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${u.locked}">
                                                    <span class="badge bg-warning text-dark">Locked (<c:out value="${u.failedLogins}"/> attempts)</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="text-muted small"><c:out value="${u.failedLogins > 0 ? u.failedLogins : '0'}"/> failed</span>
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
                                                    <c:out value="${u.active ? 'Deactivate' : 'Activate'}"/>
                                                </button>
                                            </form>

                                            <!-- Edit User Modal Trigger -->
                                            <button class="btn btn-outline-secondary btn-sm py-0 px-2 me-1"
                                                    data-bs-toggle="modal" data-bs-target="#editUserModal${u.id}">Edit</button>

                                            <!-- Delete User -->
                                            <form action="${pageContext.request.contextPath}/admin/action" method="post" class="d-inline"
                                                  onsubmit="return confirm('Permanently delete user account <c:out value="${u.email}"/>?');">
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
                                                        <h6 class="modal-title fw-bold">Edit Account #<c:out value="${u.id}"/></h6>
                                                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                                                    </div>
                                                    <div class="modal-body small">
                                                        <div class="mb-3">
                                                            <label class="form-label-clean">Full Name</label>
                                                            <input type="text" class="form-control form-control-sm" name="name" value="<c:out value='${u.name}'/>" required>
                                                        </div>
                                                        <div class="mb-3">
                                                            <label class="form-label-clean">Email Address</label>
                                                            <input type="email" class="form-control form-control-sm" name="email" value="<c:out value='${u.email}'/>" required>
                                                        </div>
                                                        <div class="mb-3">
                                                            <label class="form-label-clean">Assigned Role</label>
                                                            <select class="form-select form-select-sm" name="role">
                                                                <option value="USER" ${u.role == 'USER' ? 'selected' : ''}>USER</option>
                                                                <option value="ADVISOR" ${u.role == 'ADVISOR' ? 'selected' : ''}>ADVISOR</option>
                                                                <option value="ADMIN" ${u.role == 'ADMIN' ? 'selected' : ''}>ADMIN</option>
                                                            </select>
                                                        </div>
                                                        <div class="mb-3">
                                                            <label class="form-label-clean">Account Status</label>
                                                            <select class="form-select form-select-sm" name="active">
                                                                <option value="true" ${u.active ? 'selected' : ''}>Active</option>
                                                                <option value="false" ${!u.active ? 'selected' : ''}>Inactive / Suspended</option>
                                                            </select>
                                                        </div>
                                                        <div class="mb-2">
                                                            <label class="form-label-clean">Reset Password <span class="text-muted fw-normal">(leave blank to keep unchanged)</span></label>
                                                            <input type="password" class="form-control form-control-sm" name="newPassword" placeholder="New strong password">
                                                        </div>
                                                    </div>
                                                    <div class="modal-footer">
                                                        <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Cancel</button>
                                                        <button type="submit" class="btn btn-indigo btn-sm">Save Changes</button>
                                                    </div>
                                                </form>
                                            </div>
                                        </div>
                                    </div>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>
            </div>
        </div>

        <!-- Audit Logs Summary -->
        <div class="card card-fintech" id="audit">
            <div class="card-header-clean">
                <h6><span>🛡️</span> Security &amp; Compliance Audit Trail</h6>
                <span class="small text-muted">Recent 30 entries</span>
            </div>
            <div class="table-responsive">
                <table class="table table-fintech">
                    <thead>
                        <tr>
                            <th>Timestamp</th>
                            <th>Actor ID</th>
                            <th>Action</th>
                            <th>IP Address</th>
                            <th>Details</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${empty auditLogs}">
                                <tr>
                                    <td colspan="5">
                                        <div class="empty-state py-4">
                                            <div class="empty-state-icon">🛡️</div>
                                            <h6>No Audit Events Logged</h6>
                                            <p class="text-muted small">System actions and authentication attempts will appear here.</p>
                                        </div>
                                    </td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <c:forEach var="log" items="${auditLogs}">
                                    <tr>
                                        <td class="text-muted small text-nowrap"><c:out value="${log.createdAt}"/></td>
                                        <td>
                                            <span class="badge bg-light text-dark border">
                                                <c:out value="${empty log.userId ? 'SYSTEM' : log.userId}"/>
                                            </span>
                                        </td>
                                        <td>
                                            <span class="badge bg-secondary-subtle text-secondary border">
                                                <c:out value="${log.action}"/>
                                            </span>
                                        </td>
                                        <td class="small font-monospace"><c:out value="${log.ipAddress}"/></td>
                                        <td class="text-truncate small text-secondary" style="max-width: 280px;" title="<c:out value='${log.details}'/>">
                                            <c:out value="${log.details}"/>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

    <!-- Right: System Settings Form -->
    <div class="col-lg-4" id="settings">
        <div class="card card-fintech">
            <div class="card-header-clean">
                <h6><span>⚖️</span> System Settings &amp; AI Weights</h6>
                <span class="badge bg-success" id="weightSumBadge">100% (Balanced)</span>
            </div>
            <div class="card-body-clean">
                <form action="${pageContext.request.contextPath}/admin/action" method="post">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="updateSettings">

                    <h6 class="fw-bold text-indigo small mb-1">Baseline Category Allocations (%)</h6>
                    <p class="text-muted small mb-3">Live validation ensures the sum of all categories equals exactly 100%.</p>

                    <div class="mb-2">
                        <label class="form-label-clean">Food &amp; Groceries (%)</label>
                        <div class="input-group input-group-sm">
                            <input type="number" step="1" min="0" max="100" class="form-control weight-input" name="weight_Food"
                                   value="${empty settings['weight_Food'] ? 40 : settings['weight_Food']}" required>
                            <span class="input-group-text">%</span>
                        </div>
                    </div>
                    <div class="mb-2">
                        <label class="form-label-clean">Savings &amp; Wealth (%)</label>
                        <div class="input-group input-group-sm">
                            <input type="number" step="1" min="0" max="100" class="form-control weight-input" name="weight_Savings"
                                   value="${empty settings['weight_Savings'] ? 20 : settings['weight_Savings']}" required>
                            <span class="input-group-text">%</span>
                        </div>
                    </div>
                    <div class="mb-2">
                        <label class="form-label-clean">Emergency Fund (%)</label>
                        <div class="input-group input-group-sm">
                            <input type="number" step="1" min="0" max="100" class="form-control weight-input" name="weight_Emergency"
                                   value="${empty settings['weight_Emergency'] ? 15 : settings['weight_Emergency']}" required>
                            <span class="input-group-text">%</span>
                        </div>
                    </div>
                    <div class="mb-2">
                        <label class="form-label-clean">Entertainment &amp; Leisure (%)</label>
                        <div class="input-group input-group-sm">
                            <input type="number" step="1" min="0" max="100" class="form-control weight-input" name="weight_Entertainment"
                                   value="${empty settings['weight_Entertainment'] ? 10 : settings['weight_Entertainment']}" required>
                            <span class="input-group-text">%</span>
                        </div>
                    </div>
                    <div class="mb-3">
                        <label class="form-label-clean">Other Discretionary (%)</label>
                        <div class="input-group input-group-sm">
                            <input type="number" step="1" min="0" max="100" class="form-control weight-input" name="weight_Other"
                                   value="${empty settings['weight_Other'] ? 15 : settings['weight_Other']}" required>
                            <span class="input-group-text">%</span>
                        </div>
                    </div>

                    <hr class="my-3">
                    <h6 class="fw-bold text-danger small mb-1">Security &amp; Lockout Thresholds</h6>
                    <p class="text-muted small mb-3">Enforce brute-force lockout rules and budget pacing alerts.</p>

                    <div class="mb-2">
                        <label class="form-label-clean">Max Failed Login Attempts</label>
                        <input type="number" step="1" min="1" max="20" class="form-control form-control-sm" name="max_failed_logins"
                               value="${empty settings['max_failed_logins'] ? 5 : settings['max_failed_logins']}" required>
                    </div>
                    <div class="mb-2">
                        <label class="form-label-clean">Lockout Duration (Minutes)</label>
                        <input type="number" step="1" min="1" max="1440" class="form-control form-control-sm" name="lockout_duration_minutes"
                               value="${empty settings['lockout_duration_minutes'] ? 15 : settings['lockout_duration_minutes']}" required>
                    </div>
                    <div class="mb-2">
                        <label class="form-label-clean">Budget Alert Threshold (%)</label>
                        <div class="input-group input-group-sm">
                            <input type="number" step="0.1" min="50" max="100" class="form-control form-control-sm" name="default_alert_threshold"
                                   value="${empty settings['default_alert_threshold'] ? 80.0 : settings['default_alert_threshold']}" required>
                            <span class="input-group-text">%</span>
                        </div>
                    </div>
                    <div class="mb-3">
                        <label class="form-label-clean">Session Timeout (Minutes)</label>
                        <input type="number" step="1" min="5" max="480" class="form-control form-control-sm" name="session_timeout_minutes"
                               value="${empty settings['session_timeout_minutes'] ? 30 : settings['session_timeout_minutes']}">
                    </div>

                    <button type="submit" class="btn btn-indigo btn-sm w-100 fw-semibold py-2" id="saveSettingsBtn">
                        Save System Settings
                    </button>
                </form>
            </div>
        </div>
    </div>
</div>

<!-- Modal: Create User Account -->
<div class="modal fade" id="createUserModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <form action="${pageContext.request.contextPath}/admin/action" method="post" class="needs-validation">
                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" value="createUser">

                <div class="modal-header">
                    <h6 class="modal-title fw-bold">Create New User Account</h6>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body small">
                    <div class="mb-3">
                        <label class="form-label-clean">Full Name</label>
                        <input type="text" class="form-control form-control-sm" name="name" required placeholder="User Full Name">
                    </div>
                    <div class="mb-3">
                        <label class="form-label-clean">Email Address</label>
                        <input type="email" class="form-control form-control-sm" name="email" required placeholder="user@example.com">
                    </div>
                    <div class="mb-3">
                        <label class="form-label-clean">Initial Password</label>
                        <input type="password" class="form-control form-control-sm" name="password" required minlength="8" placeholder="At least 8 characters">
                    </div>
                    <div class="mb-2">
                        <label class="form-label-clean">Role</label>
                        <select class="form-select form-select-sm" name="role">
                            <option value="USER" selected>USER (Standard)</option>
                            <option value="ADVISOR">ADVISOR (Financial Advisor)</option>
                            <option value="ADMIN">ADMIN (System Administrator)</option>
                        </select>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-indigo btn-sm">Create Account</button>
                </div>
            </form>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
