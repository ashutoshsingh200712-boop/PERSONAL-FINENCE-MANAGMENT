<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.fmt.core" %>

<c:set var="pageTitle" value="Client Supervision - ${client.name}" />
<c:set var="activeNav" value="portfolio" />
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<div class="d-flex flex-wrap justify-content-between align-items-center mb-4">
    <div>
        <div class="mb-1">
            <a href="${pageContext.request.contextPath}/advisor/dashboard" class="text-decoration-none small text-muted">&larr; Back to Client Portfolio</a>
        </div>
        <h4 class="fw-bold mb-1">Supervising: <c:out value="${client.name}"/></h4>
        <p class="text-muted small mb-0">
            Email: <strong><c:out value="${client.email}"/></strong> &bull; Client ID: <strong>#<c:out value="${client.id}"/></strong> &bull; Period: <strong><c:out value="${selectedMonth}"/></strong>
        </p>
    </div>
    <div class="d-flex gap-2">
        <button class="btn btn-teal btn-sm" data-bs-toggle="modal" data-bs-target="#manualAdviceModal">
            💡 Provide Manual Advice
        </button>
        <button class="btn btn-primary btn-sm" data-bs-toggle="modal" data-bs-target="#sendMessageModal">
            ✉️ Send Direct Message
        </button>
    </div>
</div>

<div class="row g-4 mb-4">
    <!-- Left Column: Spending Plan & Goals -->
    <div class="col-lg-7">
        <!-- Monthly Spending Plan -->
        <div class="card card-fintech shadow-sm mb-4">
            <div class="card-header-clean d-flex justify-content-between align-items-center">
                <h6><span>📊</span> Monthly Spending Plan &amp; Category Allocations (<c:out value="${selectedMonth}"/>)</h6>
                <c:if test="${not empty plan}">
                    <span class="badge bg-success">Status: <c:out value="${plan.status}"/></span>
                </c:if>
            </div>
            <div class="card-body-clean p-3">
                <c:choose>
                    <c:when test="${not empty plan}">
                        <div class="row g-2 mb-3 bg-light p-2 rounded border small">
                            <div class="col-4">
                                <div class="text-muted text-uppercase" style="font-size: 0.7rem;">Income</div>
                                <div class="fw-bold text-success">Rs. <fmt:formatNumber value="${plan.totalIncome}" pattern="#,##0.00"/></div>
                            </div>
                            <div class="col-4">
                                <div class="text-muted text-uppercase" style="font-size: 0.7rem;">Fixed</div>
                                <div class="fw-bold text-danger">Rs. <fmt:formatNumber value="${plan.totalFixedExpenses}" pattern="#,##0.00"/></div>
                            </div>
                            <div class="col-4">
                                <div class="text-muted text-uppercase" style="font-size: 0.7rem;">Disposable</div>
                                <div class="fw-bold text-primary">Rs. <fmt:formatNumber value="${plan.disposableIncome}" pattern="#,##0.00"/></div>
                            </div>
                        </div>

                        <div class="table-responsive">
                            <table class="table table-sm table-fintech align-middle mb-0">
                                <thead>
                                    <tr>
                                        <th>Category</th>
                                        <th>Weight</th>
                                        <th class="text-end">Envelope Budget</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="item" items="${plan.items}">
                                        <tr>
                                            <td class="fw-bold text-dark"><c:out value="${item.categoryName}"/></td>
                                            <td><span class="badge bg-light text-dark border"><c:out value="${item.weightPercentage}"/>%</span></td>
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
                        <div class="text-center py-4 text-muted small">
                            No active spending plan has been generated for this client in <c:out value="${selectedMonth}"/>.
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <!-- Savings Goals -->
        <div class="card card-fintech shadow-sm mb-4">
            <div class="card-header-clean">
                <h6><span>🏆</span> Client Savings Goals &amp; Feasibility</h6>
            </div>
            <div class="card-body-clean p-3">
                <c:choose>
                    <c:when test="${empty goals}">
                        <div class="text-center py-3 text-muted small">Client has not registered any savings goals.</div>
                    </c:when>
                    <c:otherwise>
                        <div class="table-responsive">
                            <table class="table table-sm table-fintech align-middle mb-0">
                                <thead>
                                    <tr>
                                        <th>Goal</th>
                                        <th>Progress</th>
                                        <th>Feasibility</th>
                                        <th>Target Date</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="gr" items="${goals}">
                                        <c:set var="g" value="${gr.goal}"/>
                                        <tr>
                                            <td class="fw-bold"><c:out value="${g.name}"/></td>
                                            <td>Rs. <fmt:formatNumber value="${g.currentAmount}" pattern="#,##0.00"/> / Rs. <fmt:formatNumber value="${g.targetAmount}" pattern="#,##0.00"/></td>
                                            <td><span class="badge bg-${gr.status.cssClass}"><c:out value="${gr.status.label}"/></span></td>
                                            <td class="small text-muted"><c:out value="${g.targetDate}"/></td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>

    <!-- Right Column: Advice History & Dialogue -->
    <div class="col-lg-5">
        <!-- Advice History & Advisor Commenting -->
        <div class="card card-fintech shadow-sm mb-4">
            <div class="card-header-clean d-flex justify-content-between align-items-center">
                <h6><span>💡</span> Advice History &amp; Comments</h6>
                <span class="small text-muted"><c:out value="${adviceHistory.size()}"/> Items</span>
            </div>
            <div class="card-body-clean p-3" style="max-height: 480px; overflow-y: auto;">
                <c:choose>
                    <c:when test="${empty adviceHistory}">
                        <div class="text-center py-4 text-muted small">
                            No advice records found for this client. You can provide manual advice using the button above.
                        </div>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="adv" items="${adviceHistory}">
                            <div class="border rounded p-3 mb-3 bg-white shadow-sm">
                                <div class="d-flex justify-content-between align-items-start mb-1">
                                    <h6 class="fw-bold text-dark mb-0"><c:out value="${adv.title}"/></h6>
                                    <span class="badge ${adv.status == 'ACCEPTED' ? 'bg-success' : (adv.status == 'IGNORED' ? 'bg-secondary' : 'bg-primary')}">
                                        <c:out value="${adv.status}"/>
                                    </span>
                                </div>
                                <div class="text-muted small mb-2">Rule: <code><c:out value="${adv.ruleName}"/></code> &bull; <c:out value="${adv.createdAt}"/></div>
                                <p class="small text-secondary mb-2"><c:out value="${adv.explanation}"/></p>

                                <c:choose>
                                    <c:when test="${not empty adv.advisorComment}">
                                        <div class="p-2 bg-light rounded border border-teal small mb-2">
                                            <strong class="text-teal">Your Existing Comment:</strong>
                                            <div><c:out value="${adv.advisorComment}"/></div>
                                        </div>
                                    </c:when>
                                    <c:otherwise>
                                        <div class="text-muted small fst-italic mb-2">No advisor comment added yet.</div>
                                    </c:otherwise>
                                </c:choose>

                                <!-- Comment Form -->
                                <form method="post" action="${pageContext.request.contextPath}/advisor/action" class="mt-2">
                                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                                    <input type="hidden" name="action" value="comment_advice">
                                    <input type="hidden" name="clientId" value="${client.id}">
                                    <input type="hidden" name="adviceId" value="${adv.id}">
                                    <div class="input-group input-group-sm">
                                        <input type="text" name="comment" class="form-control" placeholder="Add or update advisor comment..." value="<c:out value='${adv.advisorComment}'/>" required>
                                        <button type="submit" class="btn btn-outline-teal">Save Note</button>
                                    </div>
                                </form>
                            </div>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <!-- Direct Messaging Box -->
        <div class="card card-fintech shadow-sm">
            <div class="card-header-clean d-flex justify-content-between align-items-center">
                <h6><span>✉️</span> Client Conversation</h6>
            </div>
            <div class="card-body-clean p-3">
                <div class="p-2 border rounded bg-light mb-3" style="max-height: 250px; overflow-y: auto;">
                    <c:choose>
                        <c:when test="${empty conversation}">
                            <div class="text-center py-3 text-muted small">No direct messages exchanged yet.</div>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="m" items="${conversation}">
                                <div class="mb-2 d-flex flex-column ${m.senderId == sessionScope.userId ? 'align-items-end' : 'align-items-start'}">
                                    <span class="small text-muted" style="font-size: 0.7rem;">
                                        <c:out value="${m.senderId == sessionScope.userId ? 'You' : client.name}"/> &bull; <c:out value="${m.createdAt}"/>
                                    </span>
                                    <div class="p-2 rounded ${m.senderId == sessionScope.userId ? 'bg-primary text-white' : 'bg-white border text-dark'} small" style="max-width: 85%;">
                                        <c:out value="${m.body}"/>
                                    </div>
                                </div>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </div>

                <form method="post" action="${pageContext.request.contextPath}/advisor/action">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="action" value="send_message">
                    <input type="hidden" name="clientId" value="${client.id}">
                    <div class="input-group input-group-sm">
                        <input type="text" name="body" class="form-control" placeholder="Message client directly..." required>
                        <button type="submit" class="btn btn-primary">Send →</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<!-- Modal: Manual Advice -->
<div class="modal fade" id="manualAdviceModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <form method="post" action="${pageContext.request.contextPath}/advisor/action">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="action" value="manual_advice">
                <input type="hidden" name="clientId" value="${client.id}">
                <c:if test="${not empty plan}">
                    <input type="hidden" name="planId" value="${plan.id}">
                </c:if>
                <div class="modal-header">
                    <h5 class="modal-title fw-bold">Provide Custom Advice for <c:out value="${client.name}"/></h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body small">
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Advice Title</label>
                        <input type="text" name="title" class="form-control" placeholder="e.g. Discretionary Dining Out Restructuring" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Detailed Guidance / Explanation</label>
                        <textarea name="explanation" class="form-control" rows="4" placeholder="Explain your financial rationale and recommended steps..." required></textarea>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Advisor Note / Action Item</label>
                        <input type="text" name="advisorComment" class="form-control" placeholder="e.g. Target reducing weekend dining out by Rs. 2,000">
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-teal btn-sm">Publish Advice to Client</button>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- Modal: Send Message -->
<div class="modal fade" id="sendMessageModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <form method="post" action="${pageContext.request.contextPath}/advisor/action">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="action" value="send_message">
                <input type="hidden" name="clientId" value="${client.id}">
                <div class="modal-header">
                    <h5 class="modal-title fw-bold">Direct Message to <c:out value="${client.name}"/></h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body small">
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Message</label>
                        <textarea name="body" class="form-control" rows="4" placeholder="Type your message to the client..." required></textarea>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-primary btn-sm">Send Message</button>
                </div>
            </form>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
