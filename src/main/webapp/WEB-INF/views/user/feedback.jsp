<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.fmt.core" %>

<c:set var="pageTitle" value="Help & Feedback Support" scope="request"/>
<c:set var="activeNav" value="feedback" scope="request"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <div>
        <h4 class="fw-bold mb-1">💬 Platform Feedback &amp; Support Tickets</h4>
        <p class="text-muted small mb-0">Share your platform experience, report issues, or request features directly to administrators.</p>
    </div>
</div>

<div class="row g-4 mb-4">
    <div class="col-lg-5">
        <div class="card card-fintech shadow-sm">
            <div class="card-header-clean">
                <h6><span>📝</span> Submit Feedback Ticket</h6>
            </div>
            <div class="card-body-clean p-3">
                <form method="post" action="${pageContext.request.contextPath}/feedback">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <div class="mb-3">
                        <label class="form-label small fw-semibold">Subject / Topic</label>
                        <input type="text" name="subject" class="form-control" placeholder="e.g. Question about goal calculations" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label small fw-semibold">Detailed Description</label>
                        <textarea name="body" class="form-control" rows="5" placeholder="Explain your feedback or question in detail..." required></textarea>
                    </div>
                    <button type="submit" class="btn btn-primary btn-sm px-4">Submit Feedback Ticket →</button>
                </form>
            </div>
        </div>
    </div>

    <div class="col-lg-7">
        <div class="card card-fintech shadow-sm">
            <div class="card-header-clean d-flex justify-content-between align-items-center">
                <h6><span>📋</span> Your Submitted Tickets</h6>
                <span class="small text-muted"><c:out value="${userTickets.size()}"/> Tickets Logged</span>
            </div>
            <div class="card-body-clean p-3">
                <c:choose>
                    <c:when test="${empty userTickets}">
                        <div class="text-center py-4 text-muted small">
                            You have not submitted any feedback tickets yet.
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="accordion" id="ticketsAccordion">
                            <c:forEach var="t" items="${userTickets}" varStatus="status">
                                <div class="accordion-item mb-2 border rounded">
                                    <h2 class="accordion-header" id="heading${t.id}">
                                        <button class="accordion-button ${status.first ? '' : 'collapsed'} py-2 px-3 small" type="button"
                                                data-bs-toggle="collapse" data-bs-target="#collapse${t.id}">
                                            <span class="badge ${t.status == 'OPEN' ? 'bg-success' : 'bg-secondary'} me-2">
                                                <c:out value="${t.status}"/>
                                            </span>
                                            <strong class="me-2">#<c:out value="${t.id}"/></strong>
                                            <span class="text-truncate" style="max-width: 250px;"><c:out value="${t.subject}"/></span>
                                        </button>
                                    </h2>
                                    <div id="collapse${t.id}" class="accordion-collapse collapse ${status.first ? 'show' : ''}"
                                         data-bs-parent="#ticketsAccordion">
                                        <div class="accordion-body small bg-light">
                                            <div class="text-muted small mb-1">Submitted on: <c:out value="${t.createdAt}"/></div>
                                            <div class="p-2 bg-white rounded border mb-3">
                                                <c:out value="${t.body}"/>
                                            </div>

                                            <c:choose>
                                                <c:when test="${not empty t.adminReply}">
                                                    <div class="p-3 bg-white rounded border border-primary">
                                                        <div class="fw-bold text-primary mb-1">🛡️ Administrator Official Reply:</div>
                                                        <div><c:out value="${t.adminReply}"/></div>
                                                    </div>
                                                </c:when>
                                                <c:otherwise>
                                                    <div class="text-muted fst-italic">
                                                        Awaiting review and reply from an administrator.
                                                    </div>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                    </div>
                                </div>
                            </c:forEach>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
