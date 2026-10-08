<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.fmt.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Financial Dashboard - Personal Finance Management</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        body { background-color: #f1f5f9; }
        .card { border: none; border-radius: 10px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
        .badge-status { font-size: 0.9rem; padding: 0.4rem 0.8rem; }
    </style>
</head>
<body class="d-flex flex-column min-vh-100">
    <jsp:include page="/WEB-INF/views/common/navbar.jsp"/>

    <main class="container-fluid px-4 py-4">
        <!-- Month Selector & Header -->
        <div class="d-flex flex-wrap justify-content-between align-items-center mb-4">
            <div>
                <h3 class="fw-bold mb-1">Financial Dashboard</h3>
                <p class="text-muted small mb-0">Overview for billing cycle <strong><c:out value="${selectedMonth}"/></strong> (Day ${dayOfMonth} of ${daysInMonth}, ${daysLeft} days remaining)</p>
            </div>
            <form method="get" action="${pageContext.request.contextPath}/dashboard" class="d-flex align-items-center">
                <label for="monthInput" class="form-label me-2 mb-0 small fw-semibold">Select Month:</label>
                <input type="month" id="monthInput" name="month" class="form-control form-control-sm me-2" value="${selectedMonth}">
                <button type="submit" class="btn btn-sm btn-outline-primary">View</button>
            </form>
        </div>

        <jsp:include page="/WEB-INF/views/common/flash.jsp"/>

        <!-- Notifications & Background Task Alerts -->
        <c:if test="${not empty notifications}">
            <div class="card mb-4 border-start border-4 border-warning shadow-sm">
                <div class="card-header bg-white d-flex justify-content-between align-items-center py-2 px-3">
                    <h6 class="fw-bold mb-0 text-dark d-flex align-items-center">
                        <span class="badge bg-danger rounded-pill me-2">${notifications.size()}</span>
                        Notifications &amp; AI System Alerts
                    </h6>
                    <form method="post" action="${pageContext.request.contextPath}/notifications" class="mb-0">
                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                        <input type="hidden" name="action" value="markAllRead">
                        <button type="submit" class="btn btn-sm btn-outline-secondary py-0">Mark All as Read</button>
                    </form>
                </div>
                <div class="card-body p-3">
                    <div class="list-group list-group-flush">
                        <c:forEach var="notif" items="${notifications}">
                            <div class="list-group-item d-flex justify-content-between align-items-start px-0 py-2 border-bottom">
                                <div class="ms-2 me-auto">
                                    <div class="fw-bold d-flex align-items-center">
                                        <c:choose>
                                            <c:when test="${notif.type == 'ALERT'}">
                                                <span class="badge bg-danger me-2">ALERT</span>
                                            </c:when>
                                            <c:when test="${notif.type == 'WARNING'}">
                                                <span class="badge bg-warning text-dark me-2">WARNING</span>
                                            </c:when>
                                            <c:when test="${notif.type == 'ADVICE'}">
                                                <span class="badge bg-info text-dark me-2">ADVICE</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge bg-primary me-2">INFO</span>
                                            </c:otherwise>
                                        </c:choose>
                                        <c:out value="${notif.title}"/>
                                    </div>
                                    <div class="text-secondary small mt-1"><c:out value="${notif.message}"/></div>
                                    <div class="text-muted small mt-1" style="font-size: 0.75rem;"><c:out value="${notif.createdAt}"/></div>
                                </div>
                                <form method="post" action="${pageContext.request.contextPath}/notifications" class="ms-2 mb-0">
                                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                                    <input type="hidden" name="action" value="markRead">
                                    <input type="hidden" name="id" value="${notif.id}">
                                    <button type="submit" class="btn btn-sm btn-outline-success py-0 px-2" title="Mark as read">&check;</button>
                                </form>
                            </div>
                        </c:forEach>
                    </div>
                </div>
            </div>
        </c:if>

        <!-- Key Metrics Cards -->
        <div class="row g-3 mb-4">
            <div class="col-6 col-md-3">
                <div class="card p-3">
                    <span class="text-muted small fw-semibold">Total Income</span>
                    <h4 class="fw-bold text-success mb-0">$<c:out value="${totalIncome}"/></h4>
                </div>
            </div>
            <div class="col-6 col-md-3">
                <div class="card p-3">
                    <span class="text-muted small fw-semibold">Fixed Obligations</span>
                    <h4 class="fw-bold text-danger mb-0">$<c:out value="${totalFixed}"/></h4>
                </div>
            </div>
            <div class="col-6 col-md-3">
                <div class="card p-3">
                    <span class="text-muted small fw-semibold">Disposable Income</span>
                    <h4 class="fw-bold text-primary mb-0">$<c:out value="${empty plan ? 0.00 : plan.disposableIncome}"/></h4>
                </div>
            </div>
            <div class="col-6 col-md-3">
                <div class="card p-3">
                    <span class="text-muted small fw-semibold">Current Pace Status</span>
                    <div class="mt-1">
                        <c:choose>
                            <c:when test="${overallStatus == 'UNDER'}">
                                <span class="badge bg-success badge-status">UNDER (Surplus)</span>
                            </c:when>
                            <c:when test="${overallStatus == 'OVER'}">
                                <span class="badge bg-danger badge-status">OVER (Pacing High)</span>
                            </c:when>
                            <c:otherwise>
                                <span class="badge bg-info text-dark badge-status">ON TRACK</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>
        </div>

        <!-- Limits Strip -->
        <c:if test="${not empty plan}">
            <div class="alert alert-secondary d-flex flex-wrap justify-content-between align-items-center py-2 px-3 mb-4">
                <div>
                    <span class="fw-bold me-2">AI Plan Daily Spending Limit:</span>
                    <span class="badge bg-primary fs-6 me-3">$<c:out value="${dailyLimit}"/> / day</span>
                    <span class="fw-bold me-2">Weekly Pace:</span>
                    <span class="badge bg-dark fs-6">$<c:out value="${weeklyLimit}"/> / week</span>
                </div>
                <div class="small text-muted">
                    Total Spent This Month: <strong>$<c:out value="${totalSpent}"/></strong>
                </div>
            </div>
        </c:if>

        <div class="row g-4 mb-4">
            <!-- Left Column: Setup Form & Plan Envelopes -->
            <div class="col-lg-7">
                <!-- AI Plan Setup Form -->
                <div class="card mb-4">
                    <div class="card-header bg-white fw-bold py-3 d-flex justify-content-between align-items-center">
                        <span>⚡ Generate / Update AI Spending Plan</span>
                        <c:if test="${not empty plan}">
                            <span class="badge bg-success">Active Plan for ${selectedMonth}</span>
                        </c:if>
                    </div>
                    <div class="card-body">
                        <form action="${pageContext.request.contextPath}/plan/setup" method="post">
                            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="month" value="${selectedMonth}">

                            <div class="row g-3">
                                <div class="col-md-4">
                                    <label class="form-label small fw-semibold">Monthly Income ($)</label>
                                    <input type="number" step="0.01" min="0" class="form-control form-control-sm" name="income"
                                           value="${totalIncome}" required placeholder="5000.00">
                                </div>
                                <div class="col-md-4">
                                    <label class="form-label small fw-semibold">Fixed Expenses ($)</label>
                                    <input type="number" step="0.01" min="0" class="form-control form-control-sm" name="fixedExpenses"
                                           value="${totalFixed}" required placeholder="2000.00">
                                </div>
                                <div class="col-md-4">
                                    <label class="form-label small fw-semibold">Savings Target ($)</label>
                                    <input type="number" step="0.01" min="0" class="form-control form-control-sm" name="savingsGoal"
                                           placeholder="e.g. 900.00">
                                </div>
                                <div class="col-md-8">
                                    <label class="form-label small fw-semibold">AI Strategy</label>
                                    <select class="form-select form-select-sm" name="strategy">
                                        <option value="default">Default Balanced (Food 40%, Savings 20%, Emergency 15%, Entertainment 10%, Other 15%)</option>
                                        <option value="savings">Savings-Focused (Clamped 20%-40%, Dynamic Scaling)</option>
                                    </select>
                                </div>
                                <div class="col-md-4 d-flex align-items-end">
                                    <button type="submit" class="btn btn-primary btn-sm w-100 py-2 fw-semibold">Calculate &amp; Save Plan</button>
                                </div>
                            </div>
                        </form>
                    </div>
                </div>

                <!-- Plan Envelopes Table -->
                <c:if test="${not empty plan}">
                    <div class="card mb-4">
                        <div class="card-header bg-white fw-bold py-3">
                            <span>📦 AI Category Envelopes</span>
                        </div>
                        <div class="table-responsive">
                            <table class="table table-hover align-middle mb-0 small">
                                <thead class="table-light">
                                    <tr>
                                        <th>Category</th>
                                        <th>Share</th>
                                        <th>Allocated Budget</th>
                                        <th>Spent So Far</th>
                                        <th>Pace Status</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="item" items="${plan.items}">
                                        <c:set var="catSpent" value="${empty spentPerCategory[item.categoryName] ? 0.00 : spentPerCategory[item.categoryName]}"/>
                                        <tr>
                                            <td class="fw-semibold"><c:out value="${item.categoryName}"/></td>
                                            <td><c:out value="${item.weightPercentage}"/>%</td>
                                            <td class="fw-bold">$<c:out value="${item.allocatedAmount}"/></td>
                                            <td>$<c:out value="${catSpent}"/></td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${catSpent > item.allocatedAmount}">
                                                        <span class="badge bg-danger">Over Budget</span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="badge bg-success">Within Limit</span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </c:if>
            </div>

            <!-- Right Column: Pie Chart & Advice Cards -->
            <div class="col-lg-5">
                <!-- Chart.js Pie Chart -->
                <div class="card mb-4">
                    <div class="card-header bg-white fw-bold py-3">
                        <span>📊 Expense Breakdown by Category</span>
                    </div>
                    <div class="card-body d-flex justify-content-center p-3">
                        <div style="width: 100%; max-width: 320px;">
                            <canvas id="expenseChart"></canvas>
                        </div>
                    </div>
                </div>

                <!-- Explainable Advice Cards -->
                <div class="card">
                    <div class="card-header bg-white fw-bold py-3">
                        <span>💡 AI Smart Advice &amp; Trajectory Alerts</span>
                    </div>
                    <div class="card-body p-3">
                        <c:choose>
                            <c:when test="${empty recalculations}">
                                <p class="text-muted small mb-0">Generate a spending plan to activate AI pace guidance.</p>
                            </c:when>
                            <c:otherwise>
                                <c:set var="anyAdvice" value="false"/>
                                <c:forEach var="recalc" items="${recalculations}">
                                    <c:forEach var="adv" items="${recalc.adviceList}">
                                        <c:set var="anyAdvice" value="true"/>
                                        <div class="alert alert-${recalc.status == 'OVER' ? 'danger' : (recalc.status == 'UNDER' ? 'success' : 'info')} py-2 px-3 mb-2 small shadow-sm">
                                            <c:out value="${adv}"/>
                                        </div>
                                    </c:forEach>
                                </c:forEach>
                                <c:if test="${not anyAdvice}">
                                    <div class="alert alert-light border small text-muted mb-0">
                                        All category expenditures are currently sustainable and pacing normally.
                                    </div>
                                </c:if>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>
        </div>

        <!-- Row 3: Expense Log & Modals -->
        <div class="card">
            <div class="card-header bg-white py-3 d-flex justify-content-between align-items-center">
                <span class="fw-bold">💳 Expense Transactions (${selectedMonth})</span>
                <button class="btn btn-primary btn-sm" data-bs-toggle="modal" data-bs-target="#addExpenseModal">+ Add Expense</button>
            </div>
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0 small">
                    <thead class="table-light">
                        <tr>
                            <th>Date</th>
                            <th>Category</th>
                            <th>Description</th>
                            <th>Payment</th>
                            <th>Amount</th>
                            <th class="text-end">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${empty expenses}">
                                <tr>
                                    <td colspan="6" class="text-center py-4 text-muted">No expenses recorded for this month. Click "+ Add Expense" above.</td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <c:forEach var="exp" items="${expenses}">
                                    <tr>
                                        <td><c:out value="${exp.date}"/></td>
                                        <td><span class="badge bg-light text-dark border"><c:out value="${exp.categoryName}"/></span></td>
                                        <td><c:out value="${exp.description}"/></td>
                                        <td><c:out value="${exp.paymentMethod}"/></td>
                                        <td class="fw-bold text-danger">-$<c:out value="${exp.amount}"/></td>
                                        <td class="text-end">
                                            <button class="btn btn-outline-secondary btn-sm py-0 px-2 me-1"
                                                    data-bs-toggle="modal" data-bs-target="#editExpenseModal${exp.id}">Edit</button>
                                            <form action="${pageContext.request.contextPath}/expenses" method="post" class="d-inline"
                                                  onsubmit="return confirm('Are you sure you want to delete this expense?');">
                                                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                                <input type="hidden" name="action" value="delete">
                                                <input type="hidden" name="expenseId" value="${exp.id}">
                                                <input type="hidden" name="month" value="${selectedMonth}">
                                                <button type="submit" class="btn btn-outline-danger btn-sm py-0 px-2">Delete</button>
                                            </form>
                                        </td>
                                    </tr>

                                    <!-- Edit Modal for each expense -->
                                    <div class="modal fade" id="editExpenseModal${exp.id}" tabindex="-1" aria-hidden="true">
                                        <div class="modal-dialog">
                                            <div class="modal-content">
                                                <form action="${pageContext.request.contextPath}/expenses" method="post">
                                                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                                    <input type="hidden" name="action" value="edit">
                                                    <input type="hidden" name="expenseId" value="${exp.id}">
                                                    <input type="hidden" name="month" value="${selectedMonth}">

                                                    <div class="modal-header">
                                                        <h6 class="modal-title fw-bold">Edit Expense</h6>
                                                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                                                    </div>
                                                    <div class="modal-body small">
                                                        <div class="mb-2">
                                                            <label class="form-label fw-semibold">Category</label>
                                                            <select class="form-select form-select-sm" name="categoryId" required>
                                                                <c:forEach var="c" items="${categories}">
                                                                    <option value="${c.id}" ${c.id == exp.categoryId ? 'selected' : ''}>${c.name}</option>
                                                                </c:forEach>
                                                            </select>
                                                        </div>
                                                        <div class="mb-2">
                                                            <label class="form-label fw-semibold">Amount ($)</label>
                                                            <input type="number" step="0.01" min="0.01" class="form-control form-control-sm"
                                                                   name="amount" value="${exp.amount}" required>
                                                        </div>
                                                        <div class="mb-2">
                                                            <label class="form-label fw-semibold">Date</label>
                                                            <input type="date" class="form-control form-control-sm" name="expenseDate"
                                                                   value="${exp.date}" required>
                                                        </div>
                                                        <div class="mb-2">
                                                            <label class="form-label fw-semibold">Description</label>
                                                            <input type="text" class="form-control form-control-sm" name="description"
                                                                   value="<c:out value='${exp.description}'/>" required>
                                                        </div>
                                                        <div class="mb-2">
                                                            <label class="form-label fw-semibold">Payment Method</label>
                                                            <select class="form-select form-select-sm" name="paymentMethod">
                                                                <option value="CARD" ${exp.paymentMethod == 'CARD' ? 'selected' : ''}>Card</option>
                                                                <option value="CASH" ${exp.paymentMethod == 'CASH' ? 'selected' : ''}>Cash</option>
                                                                <option value="TRANSFER" ${exp.paymentMethod == 'TRANSFER' ? 'selected' : ''}>Bank Transfer</option>
                                                            </select>
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
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>
            </div>
        </div>
    </main>

    <!-- Modal: Add Expense -->
    <div class="modal fade" id="addExpenseModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <form action="${pageContext.request.contextPath}/expenses" method="post">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="add">
                    <input type="hidden" name="month" value="${selectedMonth}">

                    <div class="modal-header">
                        <h6 class="modal-title fw-bold">Log New Expense</h6>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body small">
                        <div class="mb-2">
                            <label class="form-label fw-semibold">Category</label>
                            <select class="form-select form-select-sm" name="categoryId" required>
                                <c:forEach var="c" items="${categories}">
                                    <option value="${c.id}">${c.name}</option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="mb-2">
                            <label class="form-label fw-semibold">Amount ($)</label>
                            <input type="number" step="0.01" min="0.01" class="form-control form-control-sm"
                                   name="amount" required placeholder="0.00">
                        </div>
                        <div class="mb-2">
                            <label class="form-label fw-semibold">Date</label>
                            <input type="date" class="form-control form-control-sm" name="expenseDate" required>
                        </div>
                        <div class="mb-2">
                            <label class="form-label fw-semibold">Description</label>
                            <input type="text" class="form-control form-control-sm" name="description" required placeholder="e.g. Weekly Groceries">
                        </div>
                        <div class="mb-2">
                            <label class="form-label fw-semibold">Payment Method</label>
                            <select class="form-select form-select-sm" name="paymentMethod">
                                <option value="CARD">Debit / Credit Card</option>
                                <option value="CASH">Cash</option>
                                <option value="TRANSFER">Online Bank Transfer</option>
                            </select>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Cancel</button>
                        <button type="submit" class="btn btn-primary btn-sm">Log Expense</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <footer class="text-center py-3 text-muted border-top bg-white mt-auto small">
        Personal Finance Management Platform &copy; 2026
    </footer>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        // Render Chart.js Pie Chart
        document.addEventListener("DOMContentLoaded", function() {
            const ctx = document.getElementById('expenseChart').getContext('2d');
            const labels = ${chartLabelsJson};
            const data = ${chartDataJson};

            new Chart(ctx, {
                type: 'pie',
                data: {
                    labels: labels,
                    datasets: [{
                        data: data,
                        backgroundColor: [
                            '#6366F1', '#10B981', '#F59E0B', '#EC4899', '#06B6D4', '#8B5CF6', '#64748B'
                        ],
                        borderWidth: 1
                    }]
                },
                options: {
                    responsive: true,
                    plugins: {
                        legend: { position: 'bottom', labels: { boxWidth: 12, font: { size: 11 } } }
                    }
                }
            });
        });
    </script>
</body>
</html>
