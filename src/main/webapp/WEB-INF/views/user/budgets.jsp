<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.fmt.core" %>

<c:set var="pageTitle" value="Monthly Category Budgets" scope="request"/>
<c:set var="activeNav" value="budgets" scope="request"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<div class="d-flex flex-wrap justify-content-between align-items-center mb-4">
    <div>
        <h4 class="fw-bold mb-1">💼 Category Budgets &amp; Utilisation</h4>
        <p class="text-muted small mb-0">
            Monitoring spending pace for cycle <strong><c:out value="${selectedMonth}"/></strong> &bull;
            Thresholds: <span class="badge bg-info text-dark">70% Info</span>,
            <span class="badge bg-warning text-dark">90% Warning</span>,
            <span class="badge bg-danger">100%+ Critical</span>
        </p>
    </div>
    <div class="d-flex align-items-center gap-2">
        <form method="get" action="${pageContext.request.contextPath}/budgets" class="d-flex align-items-center gap-2">
            <input type="month" name="month" class="form-control form-control-sm" value="<c:out value='${selectedMonth}'/>">
            <button type="submit" class="btn btn-sm btn-outline-primary">Filter</button>
        </form>
        <button class="btn btn-primary btn-sm" data-bs-toggle="modal" data-bs-target="#createBudgetModal">+ Set Budget</button>
    </div>
</div>

<!-- Budgets Utilisation Table -->
<div class="card card-fintech mb-4">
    <div class="card-header-clean">
        <h6><span>📊</span> Budget Utilisation Ledger (<c:out value="${selectedMonth}"/>)</h6>
        <span class="small text-muted">Utilisation = (Spent / Budget) &times; 100</span>
    </div>
    <div class="table-responsive">
        <table class="table table-fintech">
            <thead>
                <tr>
                    <th>Category</th>
                    <th>Source</th>
                    <th>Budget Limit</th>
                    <th>Spent So Far</th>
                    <th style="width: 250px;">Utilisation</th>
                    <th class="text-end">Status Level</th>
                </tr>
            </thead>
            <tbody>
                <c:choose>
                    <c:when test="${empty budgetDetails}">
                        <tr>
                            <td colspan="6" class="text-center py-4">
                                <div class="empty-state py-3">
                                    <div class="empty-state-icon">💼</div>
                                    <h6>No Budgets Set For This Cycle</h6>
                                    <p class="text-muted small mb-3">You can create manual category budgets or accept an AI Smart Spending Plan to generate envelope budgets automatically.</p>
                                    <button class="btn btn-sm btn-primary" data-bs-toggle="modal" data-bs-target="#createBudgetModal">Create Manual Budget</button>
                                </div>
                            </td>
                        </tr>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="detail" items="${budgetDetails}">
                            <c:set var="b" value="${detail.budget}"/>
                            <c:set var="u" value="${detail.utilisation}"/>
                            <tr>
                                <td class="fw-bold text-dark"><c:out value="${b.categoryName}"/></td>
                                <td>
                                    <span class="badge ${b.source == 'AI_PLAN' ? 'bg-primary' : 'bg-secondary'}">
                                        <c:out value="${b.source}"/>
                                    </span>
                                </td>
                                <td class="fw-semibold currency-amount">
                                    Rs. <fmt:formatNumber value="${b.budgetLimit}" pattern="#,##0.00"/>
                                </td>
                                <td class="currency-amount">
                                    Rs. <fmt:formatNumber value="${detail.spent}" pattern="#,##0.00"/>
                                </td>
                                <td>
                                    <div class="d-flex justify-content-between small text-muted mb-1">
                                        <span><c:out value="${u.percentage}"/>%</span>
                                        <span>
                                            <c:choose>
                                                <c:when test="${u.percentage >= 100}">Exceeded</c:when>
                                                <c:otherwise>Rs. <fmt:formatNumber value="${b.budgetLimit - detail.spent}" pattern="#,##0.00"/> left</c:otherwise>
                                            </c:choose>
                                        </span>
                                    </div>
                                    <div class="progress" style="height: 7px;">
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
                                            <span class="badge bg-success">Normal (&lt;70%)</span>
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

<!-- Modal: Create Manual Budget -->
<div class="modal fade" id="createBudgetModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <form method="post" action="${pageContext.request.contextPath}/budgets">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="action" value="create_manual">
                <div class="modal-header">
                    <h5 class="modal-title fw-bold">Set Manual Category Budget</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <div class="mb-3">
                        <label class="form-label small fw-semibold">Target Billing Month</label>
                        <input type="month" name="month" class="form-control" value="<c:out value='${selectedMonth}'/>" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label small fw-semibold">Category</label>
                        <select name="categoryId" class="form-select" required>
                            <c:forEach var="c" items="${categories}">
                                <option value="${c.id}"><c:out value="${c.name}"/></option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="mb-3">
                        <label class="form-label small fw-semibold">Budget Limit Amount (Rs.)</label>
                        <input type="number" step="0.01" min="1" name="budgetLimit" class="form-control" placeholder="e.g. 5000.00" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label small fw-semibold">Alert Notification Threshold (%)</label>
                        <input type="number" step="1" min="1" max="100" name="alertThreshold" class="form-control" value="80">
                        <div class="form-text small">You will receive an in-app alert when spending reaches this percentage.</div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-primary btn-sm">Save Budget (MANUAL)</button>
                </div>
            </form>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
