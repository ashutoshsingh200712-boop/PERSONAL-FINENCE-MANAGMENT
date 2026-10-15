<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.fmt.core" %>

<c:set var="pageTitle" value="Financial Goals & Feasibility" scope="request"/>
<c:set var="activeNav" value="goals" scope="request"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<div class="d-flex flex-wrap justify-content-between align-items-center mb-4">
    <div>
        <h4 class="fw-bold mb-1">🏆 Savings Goals &amp; Feasibility</h4>
        <p class="text-muted small mb-0">
            Average Monthly Surplus: <strong>Rs. <fmt:formatNumber value="${averageMonthlySurplus}" pattern="#,##0.00"/></strong> &bull;
            Feasibility = (Target - Current) / Months Remaining vs. Average Monthly Surplus
        </p>
    </div>
    <div>
        <button class="btn btn-primary btn-sm" data-bs-toggle="modal" data-bs-target="#createGoalModal">+ New Savings Goal</button>
    </div>
</div>

<!-- Goals Cards Grid -->
<div class="row g-3 mb-4">
    <c:choose>
        <c:when test="${empty goalResults}">
            <div class="col-12">
                <div class="card card-fintech p-5 text-center">
                    <div class="empty-state py-3">
                        <div class="empty-state-icon">🎯</div>
                        <h5>No Financial Goals Created Yet</h5>
                        <p class="text-muted small mb-3">Set up savings targets like Emergency Fund, Vacation, or New Vehicle to compute monthly savings feasibility.</p>
                        <button class="btn btn-primary btn-sm" data-bs-toggle="modal" data-bs-target="#createGoalModal">+ Create First Goal</button>
                    </div>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <c:forEach var="res" items="${goalResults}">
                <c:set var="g" value="${res.goal}"/>
                <div class="col-md-6 col-xl-4">
                    <div class="card card-fintech h-100 shadow-sm border">
                        <div class="card-body p-3 d-flex flex-column justify-content-between">
                            <div>
                                <div class="d-flex justify-content-between align-items-start mb-2">
                                    <h6 class="fw-bold text-dark mb-0"><c:out value="${g.name}"/></h6>
                                    <span class="badge bg-${res.status.cssClass}">
                                        <c:out value="${res.status.label}"/>
                                    </span>
                                </div>
                                <div class="text-muted small mb-3">
                                    Target Date: <strong><c:out value="${g.targetDate}"/></strong>
                                    (<c:out value="${res.monthsRemaining}"/> month<c:if test="${res.monthsRemaining != 1}">s</c:if> left)
                                </div>

                                <div class="d-flex justify-content-between small text-secondary mb-1">
                                    <span>Saved: Rs. <fmt:formatNumber value="${g.currentAmount}" pattern="#,##0.00"/></span>
                                    <span>Target: Rs. <fmt:formatNumber value="${g.targetAmount}" pattern="#,##0.00"/></span>
                                </div>
                                <div class="progress mb-3" style="height: 8px;">
                                    <div class="progress-bar bg-primary" style="width: ${g.progressPercentage > 100 ? 100 : g.progressPercentage}%;"></div>
                                </div>

                                <div class="bg-light p-2 rounded border small mb-3">
                                    <div class="d-flex justify-content-between mb-1">
                                        <span class="text-muted">Required Saving:</span>
                                        <span class="fw-bold text-primary">Rs. <fmt:formatNumber value="${res.requiredMonthlySavings}" pattern="#,##0.00"/> / mo</span>
                                    </div>
                                    <div class="d-flex justify-content-between">
                                        <span class="text-muted">Current Surplus:</span>
                                        <span class="fw-semibold">Rs. <fmt:formatNumber value="${res.averageMonthlySurplus}" pattern="#,##0.00"/> / mo</span>
                                    </div>
                                </div>

                                <div class="alert alert-${res.status.cssClass} small py-2 px-3 mb-0">
                                    <strong>Assessment:</strong> <c:out value="${res.explanation}"/>
                                </div>
                            </div>

                            <div class="d-flex justify-content-between align-items-center mt-3 pt-2 border-top">
                                <button class="btn btn-outline-success btn-sm py-0 px-2"
                                        data-bs-toggle="modal" data-bs-target="#addFundsModal${g.id}">
                                    + Add Funds
                                </button>
                                <form method="post" action="${pageContext.request.contextPath}/goals" class="d-inline" onsubmit="return confirm('Delete this goal?');">
                                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                                    <input type="hidden" name="action" value="delete">
                                    <input type="hidden" name="goalId" value="${g.id}">
                                    <button type="submit" class="btn btn-outline-danger btn-sm py-0 px-2">Delete</button>
                                </form>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Add Funds Modal -->
                <div class="modal fade" id="addFundsModal${g.id}" tabindex="-1" aria-hidden="true">
                    <div class="modal-dialog modal-dialog-centered modal-sm">
                        <div class="modal-content">
                            <form method="post" action="${pageContext.request.contextPath}/goals">
                                <input type="hidden" name="csrfToken" value="${csrfToken}">
                                <input type="hidden" name="action" value="update_progress">
                                <input type="hidden" name="goalId" value="${g.id}">
                                <div class="modal-header">
                                    <h6 class="modal-title fw-bold">Deposit to: <c:out value="${g.name}"/></h6>
                                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                                </div>
                                <div class="modal-body">
                                    <label class="form-label small fw-semibold">Amount Saved (Rs.)</label>
                                    <input type="number" step="0.01" min="1" name="amount" class="form-control" placeholder="e.g. 500.00" required>
                                </div>
                                <div class="modal-footer">
                                    <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Cancel</button>
                                    <button type="submit" class="btn btn-success btn-sm">Add Savings</button>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </c:otherwise>
    </c:choose>
</div>

<!-- Modal: Create Goal -->
<div class="modal fade" id="createGoalModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <form method="post" action="${pageContext.request.contextPath}/goals">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="action" value="create">
                <div class="modal-header">
                    <h5 class="modal-title fw-bold">Create New Savings Goal</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <div class="mb-3">
                        <label class="form-label small fw-semibold">Goal Name</label>
                        <input type="text" name="name" class="form-control" placeholder="e.g. Emergency Fund or Vacation" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label small fw-semibold">Target Amount (Rs.)</label>
                        <input type="number" step="0.01" min="1" name="targetAmount" class="form-control" placeholder="e.g. 50000.00" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label small fw-semibold">Current Savings Initial Deposit (Rs.)</label>
                        <input type="number" step="0.01" min="0" name="currentAmount" class="form-control" value="0.00">
                    </div>
                    <div class="mb-3">
                        <label class="form-label small fw-semibold">Target Completion Date</label>
                        <input type="date" name="targetDate" class="form-control" required>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-primary btn-sm">Create Goal</button>
                </div>
            </form>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
