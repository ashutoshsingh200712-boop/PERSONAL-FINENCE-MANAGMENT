<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Financial Advisor Portal - Personal Finance Management</title>
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
                <h3 class="fw-bold mb-1">Financial Advisor Portfolio</h3>
                <p class="text-muted small mb-0">Assigned client accounts and spending trajectory monitoring for <strong><c:out value="${currentMonth}"/></strong></p>
            </div>
        </div>

        <jsp:include page="/WEB-INF/views/common/flash.jsp"/>

        <!-- Advisor Client Summary Cards -->
        <div class="row g-3 mb-4">
            <div class="col-md-4">
                <div class="card p-3">
                    <span class="text-muted small fw-semibold">Assigned Clients</span>
                    <h4 class="fw-bold text-primary mb-0">${clients.size()}</h4>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card p-3">
                    <span class="text-muted small fw-semibold">Pacing Sustainable (On Track / Surplus)</span>
                    <h4 class="fw-bold text-success mb-0">
                        <c:set var="sustainableCount" value="0"/>
                        <c:forEach var="c" items="${clients}">
                            <c:if test="${clientStatuses[c.id] != 'OVER'}">
                                <c:set var="sustainableCount" value="${sustainableCount + 1}"/>
                            </c:if>
                        </c:forEach>
                        ${sustainableCount}
                    </h4>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card p-3">
                    <span class="text-muted small fw-semibold">Clients Over-Pacing</span>
                    <h4 class="fw-bold text-danger mb-0">
                        <c:set var="overCount" value="0"/>
                        <c:forEach var="c" items="${clients}">
                            <c:if test="${clientStatuses[c.id] == 'OVER'}">
                                <c:set var="overCount" value="${overCount + 1}"/>
                            </c:if>
                        </c:forEach>
                        ${overCount}
                    </h4>
                </div>
            </div>
        </div>

        <!-- Clients Table -->
        <div class="card">
            <div class="card-header bg-white py-3 fw-bold">
                📋 Assigned Client Accounts
            </div>
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0 small">
                    <thead class="table-light">
                        <tr>
                            <th>Client ID</th>
                            <th>Name</th>
                            <th>Email</th>
                            <th>Disposable Income</th>
                            <th>Current Plan Status</th>
                            <th>Envelopes</th>
                            <th class="text-end">Details</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${empty clients}">
                                <tr>
                                    <td colspan="7" class="text-center py-4 text-muted">No client accounts assigned to your advisor profile yet.</td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <c:forEach var="client" items="${clients}">
                                    <c:set var="plan" value="${clientPlans[client.id]}"/>
                                    <c:set var="status" value="${clientStatuses[client.id]}"/>
                                    <tr>
                                        <td>#<c:out value="${client.id}"/></td>
                                        <td class="fw-semibold"><c:out value="${client.name}"/></td>
                                        <td><c:out value="${client.email}"/></td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${not empty plan}">
                                                    <strong>$<c:out value="${plan.disposableIncome}"/></strong>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="text-muted">No Plan</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${status == 'OVER'}">
                                                    <span class="badge bg-danger">OVER (Risk)</span>
                                                </c:when>
                                                <c:when test="${status == 'UNDER'}">
                                                    <span class="badge bg-success">UNDER (Surplus)</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge bg-info text-dark">ON TRACK</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${not empty plan}">
                                                    <span class="badge bg-light text-dark border">${plan.items.size()} Categories</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="text-muted">-</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td class="text-end">
                                            <button class="btn btn-outline-primary btn-sm py-0 px-2"
                                                    data-bs-toggle="modal" data-bs-target="#clientModal${client.id}">
                                                View Plan
                                            </button>
                                        </td>
                                    </tr>

                                    <!-- Client Details Modal -->
                                    <div class="modal fade" id="clientModal${client.id}" tabindex="-1" aria-hidden="true">
                                        <div class="modal-dialog">
                                            <div class="modal-content">
                                                <div class="modal-header">
                                                    <h6 class="modal-title fw-bold">Client: <c:out value="${client.name}"/> (<c:out value="${currentMonth}"/>)</h6>
                                                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                                                </div>
                                                <div class="modal-body small">
                                                    <c:choose>
                                                        <c:when test="${not empty plan}">
                                                            <div class="mb-3">
                                                                <div><strong>Total Income:</strong> $<c:out value="${plan.totalIncome}"/></div>
                                                                <div><strong>Fixed Expenses:</strong> $<c:out value="${plan.totalFixedExpenses}"/></div>
                                                                <div><strong>Disposable Income:</strong> $<c:out value="${plan.disposableIncome}"/></div>
                                                            </div>
                                                            <h6 class="fw-bold small mb-2">Category Allocations:</h6>
                                                            <ul class="list-group list-group-flush border">
                                                                <c:forEach var="item" items="${plan.items}">
                                                                    <li class="list-group-item d-flex justify-content-between align-items-center py-1">
                                                                        <span><c:out value="${item.categoryName}"/> (<c:out value="${item.weightPercentage}"/>%)</span>
                                                                        <span class="fw-semibold">$<c:out value="${item.allocatedAmount}"/></span>
                                                                    </li>
                                                                </c:forEach>
                                                            </ul>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <p class="text-muted mb-0">Client has not yet generated an active AI Smart Spending Plan for this month.</p>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </div>
                                                <div class="modal-footer">
                                                    <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Close</button>
                                                </div>
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
    </main>

    <footer class="text-center py-3 text-muted border-top bg-white mt-auto small">
        Personal Finance Management Platform &copy; 2026
    </footer>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
