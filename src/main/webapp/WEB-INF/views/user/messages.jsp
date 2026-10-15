<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.fmt.core" %>

<c:set var="pageTitle" value="Financial Advisor Messages" scope="request"/>
<c:set var="activeNav" value="messages" scope="request"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <div>
        <h4 class="fw-bold mb-1">✉️ Financial Advisor Messages</h4>
        <p class="text-muted small mb-0">Direct consultation and personalized feedback from your certified wealth advisor.</p>
    </div>
</div>

<div class="row g-4">
    <div class="col-lg-4">
        <div class="card card-fintech shadow-sm mb-4">
            <div class="card-body p-3 text-center">
                <div class="rounded-circle bg-teal text-white d-inline-flex align-items-center justify-content-center mb-2" style="width: 60px; height: 60px; font-size: 1.5rem; background: #0d9488;">
                    👨‍💼
                </div>
                <c:choose>
                    <c:when test="${not empty advisor}">
                        <h6 class="fw-bold mb-0 text-dark"><c:out value="${advisor.name}"/></h6>
                        <div class="text-muted small mb-2"><c:out value="${advisor.email}"/></div>
                        <span class="badge bg-success">Assigned Wealth Advisor</span>
                        <hr class="my-3">
                        <p class="text-secondary small text-start mb-0">
                            Your advisor reviews your monthly spending pace, monitors trajectory risks, and provides tailored recommendations directly into your plan.
                        </p>
                    </c:when>
                    <c:otherwise>
                        <h6 class="fw-bold mb-0 text-dark">No Advisor Assigned</h6>
                        <p class="text-muted small mt-2">An administrator will assign a personal financial advisor to your account soon.</p>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>

    <div class="col-lg-8">
        <div class="card card-fintech shadow-sm">
            <div class="card-header-clean d-flex justify-content-between align-items-center">
                <h6><span>💬</span> Advisor Dialogue History</h6>
                <span class="small text-muted"><c:out value="${conversation.size()}"/> Messages Exchanged</span>
            </div>
            <div class="card-body-clean p-3">
                <div class="conversation-box border rounded p-3 bg-light mb-3" style="max-height: 400px; overflow-y: auto;">
                    <c:choose>
                        <c:when test="${empty conversation}">
                            <div class="text-center py-4 text-muted small">
                                No direct messages yet. Send a message below to start consulting with your advisor!
                            </div>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="msg" items="${conversation}">
                                <div class="mb-3 d-flex flex-column ${msg.senderId == sessionScope.userId ? 'align-items-end' : 'align-items-start'}">
                                    <div class="small fw-semibold text-muted mb-1">
                                        <c:choose>
                                            <c:when test="${msg.senderId == sessionScope.userId}">You</c:when>
                                            <c:otherwise><c:out value="${empty msg.senderName ? 'Advisor' : msg.senderName}"/></c:otherwise>
                                        </c:choose>
                                        &bull; <span class="fw-normal"><c:out value="${msg.createdAt}"/></span>
                                    </div>
                                    <div class="p-3 rounded shadow-sm ${msg.senderId == sessionScope.userId ? 'bg-primary text-white' : 'bg-white border text-dark'}" style="max-width: 75%;">
                                        <c:out value="${msg.body}"/>
                                    </div>
                                </div>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </div>

                <c:if test="${not empty advisor}">
                    <form method="post" action="${pageContext.request.contextPath}/messages">
                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                        <div class="input-group">
                            <input type="text" name="body" class="form-control" placeholder="Ask your advisor a question or reply..." required>
                            <button type="submit" class="btn btn-primary px-4">Send Message →</button>
                        </div>
                    </form>
                </c:if>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
