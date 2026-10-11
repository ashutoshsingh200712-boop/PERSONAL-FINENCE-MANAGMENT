<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.fmt.core" %>

<c:set var="pageTitle" value="Financial Advisor Portfolio" />
<c:set var="activeNav" value="portfolio" />

<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <div>
        <h4 class="fw-bold mb-1">Financial Advisor Portfolio</h4>
        <p class="text-muted small mb-0">Assigned client accounts and spending trajectory monitoring for <strong><c:out value="${currentMonth}"/></strong></p>
    </div>
    <div class="d-flex align-items-center gap-2">
        <span class="badge bg-light text-dark border"><c:out value="${clients.size()}"/> Clients Supervised</span>
        <a href="${pageContext.request.contextPath}/advisor/dashboard" class="btn btn-teal btn-sm d-flex align-items-center gap-1 shadow-sm">
            <span>🔄</span>
            <span>Refresh Trajectories</span>
        </a>
    </div>
</div>

<!-- Advisor Client Summary Cards -->
<div class="row g-3 mb-4">
    <div class="col-md-4">
        <div class="card card-fintech metric-card">
            <div class="metric-header">
                <span class="metric-title">Active Supervised Clients</span>
                <span class="metric-icon-pill" style="background: #e0f2fe; color: #0284c7;">👥</span>
            </div>
            <div class="metric-value"><c:out value="${clients.size()}"/> Clients</div>
            <div class="metric-sub"><span class="text-muted">Under active financial guidance</span></div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card card-fintech metric-card">
            <div class="metric-header">
                <span class="metric-title">Pacing Sustainable</span>
                <span class="metric-icon-pill" style="background: #dcfce7; color: #16a34a;">✓</span>
            </div>
            <c:set var="sustainableCount" value="0"/>
            <c:forEach var="c" items="${clients}">
                <c:if test="${clientStatuses[c.id] != 'OVER'}">
                    <c:set var="sustainableCount" value="${sustainableCount + 1}"/>
                </c:if>
            </c:forEach>
            <div class="metric-value text-success"><c:out value="${sustainableCount}"/> Clients</div>
            <div class="metric-sub"><span class="badge badge-status status-ontrack">ON_TRACK or UNDER</span></div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card card-fintech metric-card">
            <div class="metric-header">
                <span class="metric-title">Over-Pacing Risk</span>
                <span class="metric-icon-pill" style="background: #fee2e2; color: #dc2626;">⚠️</span>
            </div>
            <c:set var="overCount" value="0"/>
            <c:forEach var="c" items="${clients}">
                <c:if test="${clientStatuses[c.id] == 'OVER'}">
                    <c:set var="overCount" value="${overCount + 1}"/>
                </c:if>
            </c:forEach>
            <div class="metric-value text-danger"><c:out value="${overCount}"/> Clients</div>
            <div class="metric-sub"><span class="badge badge-status status-over">Action Required</span></div>
        </div>
    </div>
</div>

<!-- Advisor Progress Bar Chart (Chart.js) -->
<div class="card card-fintech mb-4">
    <div class="card-header-clean">
        <h6><span>📊</span> Client Budget Consumption Trajectory (% Used So Far)</h6>
        <span class="small text-muted">Benchmark: Expected &le; 40% at mid-month</span>
    </div>
    <div class="card-body-clean p-3">
        <div style="position: relative; height: 220px; width: 100%;">
            <canvas id="advisorClientProgressChart"></canvas>
        </div>
    </div>
</div>

<!-- Clients Table -->
<div class="card card-fintech mb-4">
    <div class="card-header-clean">
        <h6><span>📋</span> Assigned Client Accounts</h6>
        <span class="small text-muted">Read-Only Advisory Surveillance</span>
    </div>
    <div class="table-responsive">
        <table class="table table-fintech">
            <thead>
                <tr>
                    <th>Client ID</th>
                    <th>Full Name</th>
                    <th>Email Address</th>
                    <th>Disposable Envelope</th>
                    <th>Current Plan Status</th>
                    <th>Envelopes</th>
                    <th class="text-end">Actions</th>
                </tr>
            </thead>
            <tbody>
                <c:choose>
                    <c:when test="${empty clients}">
                        <tr>
                            <td colspan="7">
                                <div class="empty-state py-4">
                                    <div class="empty-state-icon">👥</div>
                                    <h6>No Assigned Clients Found</h6>
                                    <p class="text-muted small">No client accounts are currently assigned to your advisor profile in this organization.</p>
                                </div>
                            </td>
                        </tr>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="client" items="${clients}">
                            <c:set var="plan" value="${clientPlans[client.id]}"/>
                            <c:set var="status" value="${clientStatuses[client.id]}"/>
                            <tr>
                                <td class="text-muted small">#<c:out value="${client.id}"/></td>
                                <td class="fw-bold text-dark"><c:out value="${client.name}"/></td>
                                <td class="text-secondary small"><c:out value="${client.email}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty plan}">
                                            <span class="currency-amount fw-bold">
                                                Rs. <fmt:formatNumber value="${plan.disposableIncome}" pattern="#,##0.00"/>
                                            </span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="text-muted small">No Plan Created</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${status == 'OVER'}">
                                            <span class="badge badge-status status-over">OVER (Risk)</span>
                                        </c:when>
                                        <c:when test="${status == 'UNDER'}">
                                            <span class="badge badge-status status-under">UNDER (Surplus)</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge badge-status status-ontrack">ON_TRACK</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty plan}">
                                            <span class="badge bg-light text-dark border">
                                                <c:out value="${plan.items.size()}"/> Categories
                                            </span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="text-muted">-</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="text-end">
                                    <button class="btn btn-outline-secondary btn-sm py-0 px-2"
                                            data-bs-toggle="modal" data-bs-target="#clientModal${client.id}">
                                        View Details
                                    </button>
                                </td>
                            </tr>

                            <!-- Client Details Modal -->
                            <div class="modal fade" id="clientModal${client.id}" tabindex="-1" aria-hidden="true">
                                <div class="modal-dialog modal-dialog-centered">
                                    <div class="modal-content">
                                        <div class="modal-header">
                                            <div>
                                                <h6 class="modal-title fw-bold">Client: <c:out value="${client.name}"/></h6>
                                                <div class="text-muted small">Plan Period: <c:out value="${currentMonth}"/></div>
                                            </div>
                                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                                        </div>
                                        <div class="modal-body small">
                                            <c:choose>
                                                <c:when test="${not empty plan}">
                                                    <div class="row g-2 mb-3 bg-light p-2 rounded border">
                                                        <div class="col-4">
                                                            <div class="text-muted text-uppercase" style="font-size: 0.7rem;">Income</div>
                                                            <div class="fw-bold">Rs. <fmt:formatNumber value="${plan.totalIncome}" pattern="#,##0.00"/></div>
                                                        </div>
                                                        <div class="col-4">
                                                            <div class="text-muted text-uppercase" style="font-size: 0.7rem;">Fixed</div>
                                                            <div class="fw-bold">Rs. <fmt:formatNumber value="${plan.totalFixedExpenses}" pattern="#,##0.00"/></div>
                                                        </div>
                                                        <div class="col-4">
                                                            <div class="text-muted text-uppercase" style="font-size: 0.7rem;">Available</div>
                                                            <div class="fw-bold text-teal">Rs. <fmt:formatNumber value="${plan.disposableIncome}" pattern="#,##0.00"/></div>
                                                        </div>
                                                    </div>

                                                    <h6 class="fw-bold small mb-2">Category Allocations</h6>
                                                    <div class="table-responsive">
                                                        <table class="table table-sm table-bordered align-middle mb-0">
                                                            <thead class="table-light">
                                                                <tr>
                                                                    <th>Category</th>
                                                                    <th>Weight</th>
                                                                    <th class="text-end">Limit</th>
                                                                </tr>
                                                            </thead>
                                                            <tbody>
                                                                <c:forEach var="item" items="${plan.items}">
                                                                    <tr>
                                                                        <td><c:out value="${item.categoryName}"/></td>
                                                                        <td><c:out value="${item.weightPercentage}"/>%</td>
                                                                        <td class="text-end fw-semibold">
                                                                            Rs. <fmt:formatNumber value="${item.allocatedAmount}" pattern="#,##0.00"/>
                                                                        </td>
                                                                    </tr>
                                                                </c:forEach>
                                                            </tbody>
                                                        </table>
                                                    </div>
                                                </c:when>
                                                <c:otherwise>
                                                    <div class="empty-state py-3">
                                                        <div class="empty-state-icon">🎯</div>
                                                        <h6>No Spending Plan Generated</h6>
                                                        <p class="text-muted small mb-0">The client has not yet generated an active AI Smart Spending Plan for this month.</p>
                                                    </div>
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

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
