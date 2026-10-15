<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.fmt.core" %>

<c:set var="pageTitle" value="Personal Financial Dashboard" scope="request"/>
<c:set var="activeNav" value="dashboard" scope="request"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<!-- Month Selector Bar -->
<div class="d-flex flex-wrap justify-content-between align-items-center mb-4">
    <div>
        <h4 class="fw-bold mb-1">Financial Cycle Overview</h4>
        <p class="text-muted small mb-0">
            Billing Cycle: <strong><c:out value="${selectedMonth}"/></strong> 
            (Day <c:out value="${dayOfMonth}"/> of <c:out value="${daysInMonth}"/> &bull; <c:out value="${daysLeft}"/> days remaining)
        </p>
    </div>
    <form method="get" action="${pageContext.request.contextPath}/user/dashboard" class="d-flex align-items-center gap-2">
        <label for="monthInput" class="form-label mb-0 small fw-semibold text-secondary">Cycle:</label>
        <input type="month" id="monthInput" name="month" class="form-control form-control-sm" value="<c:out value='${selectedMonth}'/>">
        <button type="submit" class="btn btn-sm btn-outline-primary">View</button>
    </form>
</div>

<!-- Four Summary Metric Cards -->
<div class="row g-3 mb-4">
    <div class="col-sm-6 col-xl-3">
        <div class="card card-fintech metric-card">
            <div class="metric-header">
                <span class="metric-title">Monthly Income</span>
                <span class="metric-icon-pill" style="background: #dcfce7; color: #16a34a;">💰</span>
            </div>
            <div class="metric-value text-success currency-amount">
                Rs. <fmt:formatNumber value="${totalIncome}" pattern="#,##0.00"/>
            </div>
            <div class="metric-sub">
                <span class="text-muted">Verified Inflows</span>
            </div>
        </div>
    </div>

    <div class="col-sm-6 col-xl-3">
        <div class="card card-fintech metric-card">
            <div class="metric-header">
                <span class="metric-title">Fixed Obligations</span>
                <span class="metric-icon-pill" style="background: #fee2e2; color: #dc2626;">📑</span>
            </div>
            <div class="metric-value text-danger currency-amount">
                Rs. <fmt:formatNumber value="${totalFixed}" pattern="#,##0.00"/>
            </div>
            <div class="metric-sub">
                <span class="text-muted">Rent, Bills &amp; Insurance</span>
            </div>
        </div>
    </div>

    <div class="col-sm-6 col-xl-3">
        <div class="card card-fintech metric-card">
            <div class="metric-header">
                <span class="metric-title">Available Envelope</span>
                <span class="metric-icon-pill" style="background: #e0e7ff; color: #4f46e5;">✨</span>
            </div>
            <div class="metric-value text-primary currency-amount">
                Rs. <fmt:formatNumber value="${empty plan ? 0.00 : plan.disposableIncome}" pattern="#,##0.00"/>
            </div>
            <div class="metric-sub">
                <c:choose>
                    <c:when test="${overallStatus == 'UNDER'}">
                        <span class="badge badge-status status-under">UNDER (Surplus)</span>
                    </c:when>
                    <c:when test="${overallStatus == 'OVER'}">
                        <span class="badge badge-status status-over">OVER (Pacing High)</span>
                    </c:when>
                    <c:otherwise>
                        <span class="badge badge-status status-ontrack">ON_TRACK</span>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>

    <div class="col-sm-6 col-xl-3">
        <div class="card card-fintech metric-card">
            <div class="metric-header">
                <span class="metric-title">Spent So Far</span>
                <span class="metric-icon-pill" style="background: #f1f5f9; color: #475569;">💳</span>
            </div>
            <div class="metric-value currency-amount">
                Rs. <fmt:formatNumber value="${totalSpent}" pattern="#,##0.00"/>
            </div>
            <div class="metric-sub">
                <span class="text-muted">
                    <c:choose>
                        <c:when test="${not empty plan && plan.disposableIncome > 0}">
                            <fmt:formatNumber value="${(totalSpent / plan.disposableIncome) * 100}" maxFractionDigits="1"/>% of disposable
                        </c:when>
                        <c:otherwise>0% of disposable</c:otherwise>
                    </c:choose>
                </span>
            </div>
        </div>
    </div>
</div>

<!-- AI Limits Strip -->
<c:if test="${not empty plan}">
    <div class="card card-fintech mb-4 bg-white p-3 border-start border-4 border-primary">
        <div class="d-flex flex-wrap justify-content-between align-items-center gap-3">
            <div class="d-flex align-items-center gap-3">
                <span class="fs-4">🎯</span>
                <div>
                    <strong class="d-block text-dark">AI Pacing Targets for <c:out value="${selectedMonth}"/>:</strong>
                    <div class="d-flex flex-wrap gap-2 mt-1">
                        <span class="badge bg-primary-subtle text-primary border fs-6">
                            Daily Limit: Rs. <fmt:formatNumber value="${dailyLimit}" pattern="#,##0.00"/> / day
                        </span>
                        <span class="badge bg-dark-subtle text-dark border fs-6">
                            Weekly Pace: Rs. <fmt:formatNumber value="${weeklyLimit}" pattern="#,##0.00"/> / week
                        </span>
                    </div>
                </div>
            </div>
            <div class="small text-muted text-end">
                Disposable Base: <strong class="text-dark">Rs. <fmt:formatNumber value="${plan.disposableIncome}" pattern="#,##0.00"/></strong>
            </div>
        </div>
    </div>
</c:if>

<!-- Charts Section: Doughnut Category Split & Line Trajectory -->
<div class="row g-4 mb-4">
    <!-- Chart 1: Doughnut Breakdown -->
    <div class="col-lg-5">
        <div class="card card-fintech h-100">
            <div class="card-header-clean">
                <h6><span>🥧</span> Spending by Category</h6>
                <span class="small text-muted">Allocated Distribution</span>
            </div>
            <div class="card-body-clean d-flex align-items-center justify-content-center p-3">
                <div style="position: relative; height: 260px; width: 100%;">
                    <canvas id="categoryDoughnutChart" 
                            data-labels='<c:out value="${chartLabelsJson}" escapeXml="false"/>' 
                            data-values='<c:out value="${chartDataJson}" escapeXml="false"/>'>
                    </canvas>
                </div>
            </div>
        </div>
    </div>

    <!-- Chart 2: Line Daily Spend vs Benchmark -->
    <div class="col-lg-7">
        <div class="card card-fintech h-100">
            <div class="card-header-clean">
                <h6><span>📈</span> Daily Spending vs Daily Limit Pace</h6>
                <span class="badge bg-danger-subtle text-danger border">
                    Limit: Rs. <fmt:formatNumber value="${dailyLimit}" pattern="#,##0.00"/>/day
                </span>
            </div>
            <div class="card-body-clean p-3">
                <div style="position: relative; height: 260px; width: 100%;">
                    <canvas id="dailySpendingLineChart"></canvas>
                </div>
            </div>
        </div>
    </div>
</div>

<!-- Section 3: AI Planner Configuration & Envelopes Table -->
<div class="row g-4 mb-4" id="planner">
    <div class="col-lg-12">
        <!-- AI Plan Setup Form -->
        <div class="card card-fintech mb-4">
            <div class="card-header-clean">
                <h6><span>⚡</span> Generate / Recalculate AI Spending Plan</h6>
                <c:if test="${not empty plan}">
                    <span class="badge bg-success-subtle text-success border">Active Plan (<c:out value="${selectedMonth}"/>)</span>
                </c:if>
            </div>
            <div class="card-body-clean">
                <form action="${pageContext.request.contextPath}/plan/setup" method="post">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="month" value="<c:out value='${selectedMonth}'/>">

                    <div class="row g-3">
                        <div class="col-md-3">
                            <label class="form-label-clean">Monthly Total Income (Rs.)</label>
                            <input type="number" step="0.01" min="0" class="form-control form-control-clean" name="income"
                                   value="<c:out value='${totalIncome}'/>" required placeholder="5000.00">
                        </div>
                        <div class="col-md-3">
                            <label class="form-label-clean">Fixed Living Obligations (Rs.)</label>
                            <input type="number" step="0.01" min="0" class="form-control form-control-clean" name="fixedExpenses"
                                   value="<c:out value='${totalFixed}'/>" required placeholder="2000.00">
                        </div>
                        <div class="col-md-3">
                            <label class="form-label-clean">Savings Target Goal (Rs.)</label>
                            <input type="number" step="0.01" min="0" class="form-control form-control-clean" name="savingsGoal"
                                   placeholder="e.g. 600.00">
                        </div>
                        <div class="col-md-3">
                            <label class="form-label-clean">AI Heuristic Strategy</label>
                            <select class="form-select form-select-clean" name="strategy">
                                <option value="default" selected>Default Heuristic (Food 40%, Savings 20%, Emergency 15%, Entertainment 10%, Other 15%)</option>
                                <option value="savings">Savings-Focused Dynamic Scaling (20% - 40%)</option>
                            </select>
                        </div>
                        <div class="col-12 text-end">
                            <button type="submit" class="btn btn-indigo btn-sm px-4">Generate &amp; Save AI Plan →</button>
                        </div>
                    </div>
                </form>
            </div>
        </div>

        <!-- Envelopes Breakdown Table -->
        <c:if test="${not empty plan}">
            <div class="card card-fintech mb-4">
                <div class="card-header-clean d-flex justify-content-between align-items-center">
                    <div>
                        <h6 class="mb-0"><span>📦</span> Category Allocation Envelopes</h6>
                        <span class="small text-muted">Automatic Rounding Protection</span>
                    </div>
                    <form method="post" action="${pageContext.request.contextPath}/budgets/accept-plan" class="d-inline">
                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                        <input type="hidden" name="planId" value="${plan.id}">
                        <input type="hidden" name="month" value="${selectedMonth}">
                        <input type="hidden" name="redirect" value="dashboard">
                        <button type="submit" class="btn btn-success btn-sm d-flex align-items-center gap-1 shadow-sm">
                            <span>✓</span>
                            <span>Accept Plan as Budget (AI_PLAN)</span>
                        </button>
                    </form>
                </div>
                <div class="table-responsive">
                    <table class="table table-fintech">
                        <thead>
                            <tr>
                                <th>Category</th>
                                <th>Share %</th>
                                <th>Allocated Amount</th>
                                <th>Spent So Far</th>
                                <th style="width: 220px;">Progress</th>
                                <th class="text-end">Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="item" items="${plan.items}">
                                <c:set var="catSpent" value="${empty spentPerCategory[item.categoryName] ? 0.00 : spentPerCategory[item.categoryName]}"/>
                                <tr>
                                    <td>
                                        <span class="fw-bold text-dark"><c:out value="${item.categoryName}"/></span>
                                    </td>
                                    <td><span class="badge bg-light text-dark border"><c:out value="${item.weightPercentage}"/>%</span></td>
                                    <td class="fw-semibold currency-amount">
                                        Rs. <fmt:formatNumber value="${item.allocatedAmount}" pattern="#,##0.00"/>
                                    </td>
                                    <td class="currency-amount">
                                        Rs. <fmt:formatNumber value="${catSpent}" pattern="#,##0.00"/>
                                    </td>
                                    <td>
                                        <c:set var="pct" value="${item.allocatedAmount > 0 ? (catSpent / item.allocatedAmount) * 100 : 0}"/>
                                        <div class="d-flex justify-content-between small text-muted mb-1">
                                            <span><fmt:formatNumber value="${pct}" maxFractionDigits="0"/>%</span>
                                        </div>
                                        <div class="progress" style="height: 6px;">
                                            <div class="progress-bar ${catSpent > item.allocatedAmount ? 'bg-danger' : (pct > 75 ? 'bg-warning' : 'bg-success')}" 
                                                 style="width: ${pct > 100 ? 100 : pct}%;"></div>
                                        </div>
                                    </td>
                                    <td class="text-end">
                                        <c:choose>
                                            <c:when test="${catSpent > item.allocatedAmount}">
                                                <span class="badge badge-status status-over">OVER</span>
                                            </c:when>
                                            <c:when test="${pct < 50}">
                                                <span class="badge badge-status status-under">UNDER</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge badge-status status-ontrack">ON_TRACK</span>
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
</div>

<!-- Category Budgets & Utilisation Section -->
<div class="card card-fintech mb-4" id="budgets">
    <div class="card-header-clean d-flex justify-content-between align-items-center">
        <div>
            <h6 class="mb-0"><span>💼</span> Active Category Budgets &amp; Utilisation (<c:out value="${selectedMonth}"/>)</h6>
            <span class="small text-muted">Utilisation = (Spent / Budget) &times; 100 &bull; <span class="badge bg-info text-dark">70% Info</span> <span class="badge bg-warning text-dark">90% Warning</span> <span class="badge bg-danger">100%+ Critical</span></span>
        </div>
        <a href="${pageContext.request.contextPath}/budgets?month=${selectedMonth}" class="btn btn-outline-primary btn-sm">Manage Budgets &rarr;</a>
    </div>
    <div class="table-responsive">
        <table class="table table-fintech align-middle mb-0">
            <thead>
                <tr>
                    <th>Category</th>
                    <th>Source</th>
                    <th>Budget Limit</th>
                    <th>Spent So Far</th>
                    <th style="width: 220px;">Utilisation</th>
                    <th class="text-end">Level</th>
                </tr>
            </thead>
            <tbody>
                <c:choose>
                    <c:when test="${empty budgetDetails}">
                        <tr>
                            <td colspan="6" class="text-center py-3 text-muted small">
                                No category budgets configured for <c:out value="${selectedMonth}"/>.
                                <c:if test="${not empty plan}">
                                    Click <strong>Accept Plan as Budget (AI_PLAN)</strong> above to copy envelopes to budgets!
                                </c:if>
                            </td>
                        </tr>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="det" items="${budgetDetails}">
                            <c:set var="b" value="${det.budget}"/>
                            <c:set var="u" value="${det.utilisation}"/>
                            <tr>
                                <td class="fw-bold"><c:out value="${b.categoryName}"/></td>
                                <td>
                                    <span class="badge ${b.source == 'AI_PLAN' ? 'bg-primary' : 'bg-secondary'}">
                                        <c:out value="${b.source}"/>
                                    </span>
                                </td>
                                <td class="fw-semibold currency-amount">Rs. <fmt:formatNumber value="${b.budgetLimit}" pattern="#,##0.00"/></td>
                                <td class="currency-amount">Rs. <fmt:formatNumber value="${det.spent}" pattern="#,##0.00"/></td>
                                <td>
                                    <div class="d-flex justify-content-between small text-muted mb-1">
                                        <span><c:out value="${u.percentage}"/>%</span>
                                    </div>
                                    <div class="progress" style="height: 6px;">
                                        <div class="progress-bar ${u.critical ? 'bg-danger' : (u.warning ? 'bg-warning' : (u.info ? 'bg-info' : 'bg-success'))}"
                                             style="width: ${u.percentage > 100 ? 100 : u.percentage}%;"></div>
                                    </div>
                                </td>
                                <td class="text-end">
                                    <c:choose>
                                        <c:when test="${u.critical}">
                                            <span class="badge bg-danger">Critical (&ge;100%)</span>
                                        </c:when>
                                        <c:when test="${u.warning}">
                                            <span class="badge bg-warning text-dark">Warning (&ge;90%)</span>
                                        </c:when>
                                        <c:when test="${u.info}">
                                            <span class="badge bg-info text-dark">Info (&ge;70%)</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge bg-success">Normal</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </tbody>
        </table>
    </div>
</div>

<!-- Savings Goals Feasibility Section -->
<div class="card card-fintech mb-4" id="goals">
    <div class="card-header-clean d-flex justify-content-between align-items-center">
        <div>
            <h6 class="mb-0"><span>🏆</span> Savings Goals &amp; Feasibility</h6>
            <span class="small text-muted">Feasibility = (Target - Current) / Months Remaining vs. Average Monthly Surplus</span>
        </div>
        <a href="${pageContext.request.contextPath}/goals" class="btn btn-outline-primary btn-sm">+ Manage Goals</a>
    </div>
    <div class="card-body-clean p-3">
        <c:choose>
            <c:when test="${empty goalResults}">
                <div class="text-center py-3 text-muted small">
                    No savings goals created yet. <a href="${pageContext.request.contextPath}/goals" class="text-primary fw-semibold">Create a goal</a> to track monthly feasibility.
                </div>
            </c:when>
            <c:otherwise>
                <div class="row g-3">
                    <c:forEach var="res" items="${goalResults}">
                        <c:set var="g" value="${res.goal}"/>
                        <div class="col-md-6">
                            <div class="border rounded p-3 bg-white shadow-sm h-100 d-flex flex-column justify-content-between">
                                <div>
                                    <div class="d-flex justify-content-between align-items-start mb-1">
                                        <strong class="text-dark"><c:out value="${g.name}"/></strong>
                                        <span class="badge bg-${res.status.cssClass}"><c:out value="${res.status.label}"/></span>
                                    </div>
                                    <div class="text-muted small mb-2">Target Date: <c:out value="${g.targetDate}"/> (<c:out value="${res.monthsRemaining}"/> month(s) left)</div>
                                    <div class="d-flex justify-content-between small text-secondary mb-1">
                                        <span>Saved: Rs. <fmt:formatNumber value="${g.currentAmount}" pattern="#,##0.00"/></span>
                                        <span>Target: Rs. <fmt:formatNumber value="${g.targetAmount}" pattern="#,##0.00"/></span>
                                    </div>
                                    <div class="progress mb-2" style="height: 6px;">
                                        <div class="progress-bar bg-primary" style="width: ${g.progressPercentage > 100 ? 100 : g.progressPercentage}%;"></div>
                                    </div>
                                    <div class="alert alert-${res.status.cssClass} small py-1 px-2 mb-0">
                                        <c:out value="${res.explanation}"/>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<!-- Section 4: Explainable AI Advice & Advisor Guidance -->
<div class="mb-4" id="advice">
    <div class="d-flex justify-content-between align-items-center mb-3">
        <h5 class="fw-bold mb-0">💡 Explainable AI Advice &amp; Advisor Guidance</h5>
        <span class="small text-muted">Deterministic Rules &amp; Supervised Guidance</span>
    </div>

    <c:choose>
        <c:when test="${empty recalculations}">
            <div class="card card-fintech p-4 text-center text-muted small">
                Configure your income and fixed commitments above to activate AI pace monitoring.
            </div>
        </c:when>
        <c:otherwise>
            <c:set var="hasAnyAdvice" value="false"/>
            <c:forEach var="recalc" items="${recalculations}">
                <c:forEach var="adv" items="${recalc.adviceList}">
                    <c:set var="hasAnyAdvice" value="true"/>
                    <div class="advice-card ${recalc.status == 'OVER' ? 'advice-over' : (recalc.status == 'UNDER' ? 'advice-surplus' : 'advice-ontrack')}">
                        <div class="d-flex justify-content-between align-items-start mb-2">
                            <div>
                                <span class="badge badge-status ${recalc.status == 'OVER' ? 'status-over' : (recalc.status == 'UNDER' ? 'status-under' : 'status-ontrack')} me-2">
                                    STATUS: <c:out value="${recalc.status}"/>
                                </span>
                                <span class="advice-rule-chip"><c:out value="${recalc.categoryName}"/> Analysis</span>
                            </div>
                        </div>
                        <h6 class="fw-bold text-dark mb-1"><c:out value="${recalc.categoryName}"/> Recommendation</h6>
                        <p class="text-secondary small mb-2"><c:out value="${adv}"/></p>
                        <div class="small text-muted">
                            Adjusted Pace Limit: <strong class="text-primary">Rs. <fmt:formatNumber value="${recalc.adjustedDailyLimit}" pattern="#,##0.00"/> / day</strong> 
                            across remaining <c:out value="${recalc.daysLeft}"/> days.
                        </div>
                    </div>
                </c:forEach>
            </c:forEach>
            <c:if test="${not hasAnyAdvice}">
                <div class="card card-fintech p-3 bg-white text-secondary small border-success">
                    ✓ All category expenditures are currently within healthy linear trajectory bounds.
                </div>
            </c:if>
        </c:otherwise>
    </c:choose>

    <!-- Advisor Comments & Personalized Guidance -->
    <c:if test="${not empty adviceHistory}">
        <div class="mt-3">
            <h6 class="fw-bold text-dark mb-2">👨‍💼 Advisor Personalized Notes &amp; Guidance</h6>
            <div class="row g-2">
                <c:forEach var="a" items="${adviceHistory}">
                    <c:if test="${not empty a.advisorComment || a.ruleName == 'ADVISOR_MANUAL'}">
                        <div class="col-md-6">
                            <div class="border rounded p-3 bg-white shadow-sm h-100">
                                <div class="d-flex justify-content-between align-items-start mb-1">
                                    <strong class="text-teal"><c:out value="${a.title}"/></strong>
                                    <span class="badge ${a.status == 'ACCEPTED' ? 'bg-success' : 'bg-primary'}"><c:out value="${a.status}"/></span>
                                </div>
                                <p class="small text-secondary mb-2"><c:out value="${a.explanation}"/></p>
                                <c:if test="${not empty a.advisorComment}">
                                    <div class="p-2 bg-light rounded border border-teal small text-dark">
                                        <strong class="text-teal">Advisor Note:</strong> <c:out value="${a.advisorComment}"/>
                                    </div>
                                </c:if>
                            </div>
                        </div>
                    </c:if>
                </c:forEach>
            </div>
        </div>
    </c:if>

    <!-- Assigned Advisor Messages Dialog Quick-View -->
    <c:if test="${not empty assignedAdvisor}">
        <div class="card card-fintech mt-3 shadow-sm border">
            <div class="card-header-clean d-flex justify-content-between align-items-center">
                <h6><span>✉️</span> Direct Messages from Certified Advisor: <c:out value="${assignedAdvisor.name}"/></h6>
                <a href="${pageContext.request.contextPath}/messages" class="btn btn-outline-primary btn-sm">Full Dialogue &rarr;</a>
            </div>
            <div class="card-body-clean p-3">
                <c:choose>
                    <c:when test="${empty advisorMessages}">
                        <div class="text-muted small">No direct messages yet. <a href="${pageContext.request.contextPath}/messages">Click here to send a question to your advisor.</a></div>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="m" items="${advisorMessages}">
                            <div class="mb-2 d-flex flex-column ${m.senderId == sessionScope.userId ? 'align-items-end' : 'align-items-start'}">
                                <div class="p-2 rounded ${m.senderId == sessionScope.userId ? 'bg-primary text-white' : 'bg-light border text-dark'} small" style="max-width: 80%;">
                                    <c:out value="${m.body}"/>
                                </div>
                                <span class="text-muted" style="font-size: 0.65rem;"><c:out value="${m.createdAt}"/></span>
                            </div>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </c:if>
</div>

<!-- Section 5: Expenses Table & Modal Management -->
<div class="card card-fintech mb-4" id="expenses">
    <div class="card-header-clean d-flex justify-content-between align-items-center">
        <div>
            <h6 class="mb-0"><span>💳</span> Expense Ledger (<c:out value="${selectedMonth}"/>)</h6>
            <span class="small text-muted">Daily discretionary transactions</span>
        </div>
        <div>
            <a href="${pageContext.request.contextPath}/expenses/export?month=${selectedMonth}" class="btn btn-outline-secondary btn-sm me-2">
                📥 Export CSV
            </a>
            <button class="btn btn-indigo btn-sm" data-bs-toggle="modal" data-bs-target="#addExpenseModal">+ Add Expense</button>
        </div>
    </div>
    <div class="table-responsive">
        <table class="table table-fintech">
            <thead>
                <tr>
                    <th>Date</th>
                    <th>Category</th>
                    <th>Description</th>
                    <th>Payment Method</th>
                    <th>Amount</th>
                    <th class="text-end">Actions</th>
                </tr>
            </thead>
            <tbody>
                <c:choose>
                    <c:when test="${empty expenses}">
                        <tr>
                            <td colspan="6" class="text-center py-4 text-muted">
                                <div class="empty-state py-3">
                                    <div class="empty-state-icon">💸</div>
                                    <h6 class="empty-state-title">No expenses recorded for <c:out value="${selectedMonth}"/></h6>
                                    <p class="empty-state-desc mb-2">Click "+ Add Expense" above to log your first expenditure.</p>
                                </div>
                            </td>
                        </tr>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="exp" items="${expenses}">
                            <tr>
                                <td class="text-muted"><c:out value="${exp.date}"/></td>
                                <td><span class="badge bg-primary-subtle text-primary border"><c:out value="${exp.categoryName}"/></span></td>
                                <td class="fw-semibold text-dark"><c:out value="${exp.description}"/></td>
                                <td><span class="badge bg-light text-dark border"><c:out value="${exp.paymentMethod}"/></span></td>
                                <td class="fw-bold text-danger currency-amount">
                                    -Rs. <fmt:formatNumber value="${exp.amount}" pattern="#,##0.00"/>
                                </td>
                                <td class="text-end">
                                    <button class="btn btn-sm btn-outline-secondary py-0 px-2 me-1" 
                                            data-bs-toggle="modal" data-bs-target="#editExpenseModal${exp.id}">Edit</button>
                                    <form action="${pageContext.request.contextPath}/expenses" method="post" class="d-inline"
                                          onsubmit="return confirm('Delete this expense?');">
                                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="action" value="delete">
                                        <input type="hidden" name="expenseId" value="${exp.id}">
                                        <input type="hidden" name="month" value="<c:out value='${selectedMonth}'/>">
                                        <button type="submit" class="btn btn-sm btn-outline-danger py-0 px-2">Delete</button>
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
                                            <input type="hidden" name="month" value="<c:out value='${selectedMonth}'/>">

                                            <div class="modal-header">
                                                <h6 class="modal-title fw-bold">Edit Expense #<c:out value="${exp.id}"/></h6>
                                                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                                            </div>
                                            <div class="modal-body small">
                                                <div class="mb-2">
                                                    <label class="form-label-clean">Category</label>
                                                    <select class="form-select form-select-sm" name="categoryId" required>
                                                        <c:forEach var="c" items="${categories}">
                                                            <option value="${c.id}" ${c.id == exp.categoryId ? 'selected' : ''}><c:out value="${c.name}"/></option>
                                                        </c:forEach>
                                                    </select>
                                                </div>
                                                <div class="mb-2">
                                                    <label class="form-label-clean">Amount (Rs.)</label>
                                                    <input type="number" step="0.01" min="0.01" class="form-control form-control-sm"
                                                           name="amount" value="${exp.amount}" required>
                                                </div>
                                                <div class="mb-2">
                                                    <label class="form-label-clean">Date</label>
                                                    <input type="date" class="form-control form-control-sm" name="expenseDate"
                                                           value="${exp.date}" required>
                                                </div>
                                                <div class="mb-2">
                                                    <label class="form-label-clean">Description</label>
                                                    <input type="text" class="form-control form-control-sm" name="description"
                                                           value="<c:out value='${exp.description}'/>" required>
                                                </div>
                                                <div class="mb-2">
                                                    <label class="form-label-clean">Payment Method</label>
                                                    <select class="form-select form-select-sm" name="paymentMethod">
                                                        <option value="DEBIT_CARD" ${exp.paymentMethod == 'DEBIT_CARD' ? 'selected' : ''}>Debit Card</option>
                                                        <option value="CREDIT_CARD" ${exp.paymentMethod == 'CREDIT_CARD' ? 'selected' : ''}>Credit Card</option>
                                                        <option value="CASH" ${exp.paymentMethod == 'CASH' ? 'selected' : ''}>Cash</option>
                                                        <option value="TRANSFER" ${exp.paymentMethod == 'TRANSFER' ? 'selected' : ''}>Online Transfer</option>
                                                    </select>
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

<!-- Modal: Add Expense -->
<div class="modal fade" id="addExpenseModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <form action="${pageContext.request.contextPath}/expenses" method="post">
                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" value="add">
                <input type="hidden" name="month" value="<c:out value='${selectedMonth}'/>">

                <div class="modal-header">
                    <h6 class="modal-title fw-bold">Log New Expense</h6>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body small">
                    <div class="mb-2">
                        <label class="form-label-clean">Category</label>
                        <select class="form-select form-select-sm" name="categoryId" required>
                            <c:forEach var="c" items="${categories}">
                                <option value="${c.id}"><c:out value="${c.name}"/></option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="mb-2">
                        <label class="form-label-clean">Amount (Rs.)</label>
                        <input type="number" step="0.01" min="0.01" class="form-control form-control-sm"
                               name="amount" required placeholder="0.00">
                    </div>
                    <div class="mb-2">
                        <label class="form-label-clean">Date</label>
                        <input type="date" class="form-control form-control-sm" name="expenseDate" required>
                    </div>
                    <div class="mb-2">
                        <label class="form-label-clean">Description</label>
                        <input type="text" class="form-control form-control-sm" name="description" required placeholder="e.g. Weekly Supermarket Groceries">
                    </div>
                    <div class="mb-2">
                        <label class="form-label-clean">Payment Method</label>
                        <select class="form-select form-select-sm" name="paymentMethod">
                            <option value="DEBIT_CARD">Debit Card</option>
                            <option value="CREDIT_CARD">Credit Card</option>
                            <option value="CASH">Cash</option>
                            <option value="TRANSFER">Online Transfer</option>
                        </select>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-indigo btn-sm">Log Expense</button>
                </div>
            </form>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
