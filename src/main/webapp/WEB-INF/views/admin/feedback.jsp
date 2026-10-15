<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.fmt.core" %>

<c:set var="pageTitle" value="Feedback Ticket Management" scope="request"/>
<c:set var="activeNav" value="feedback" scope="request"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <div>
        <h4 class="fw-bold mb-1">🛡️ Admin Feedback Ticket Center</h4>
        <p class="text-muted small mb-0">Review user feedback, answer technical support tickets, and resolve issues.</p>
    </div>
    <div class="d-flex gap-2">
        <span class="badge bg-light text-dark border"><c:out value="${tickets.size()}"/> Total Tickets</span>
    </div>
</div>

<div class="card card-fintech shadow-sm mb-4">
    <div class="card-header-clean d-flex justify-content-between align-items-center">
        <h6><span>📋</span> All User Feedback Tickets</h6>
    </div>
    <div class="table-responsive">
        <table class="table table-fintech align-middle mb-0">
            <thead>
                <tr>
                    <th>Ticket ID</th>
                    <th>User</th>
                    <th>Subject</th>
                    <th>Status</th>
                    <th>Submitted</th>
                    <th>Admin Reply</th>
                    <th class="text-end">Actions</th>
                </tr>
            </thead>
            <tbody>
                <c:choose>
                    <c:when test="${empty tickets}">
                        <tr>
                            <td colspan="7" class="text-center py-4 text-muted">
                                No feedback tickets currently recorded.
                            </td>
                        </tr>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="t" items="${tickets}">
                            <tr>
                                <td class="fw-bold text-muted small">#<c:out value="${t.id}"/></td>
                                <td>
                                    <div class="fw-bold text-dark"><c:out value="${empty t.userName ? 'User' : t.userName}"/></div>
                                    <div class="text-muted small"><c:out value="${t.userEmail}"/></div>
                                </td>
                                <td>
                                    <div class="fw-semibold"><c:out value="${t.subject}"/></div>
                                    <div class="text-secondary small text-truncate" style="max-width: 250px;"><c:out value="${t.body}"/></div>
                                </td>
                                <td>
                                    <span class="badge ${t.status == 'OPEN' ? 'bg-success' : 'bg-secondary'}">
                                        <c:out value="${t.status}"/>
                                    </span>
                                </td>
                                <td class="text-muted small"><c:out value="${t.createdAt}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty t.adminReply}">
                                            <span class="badge bg-info text-dark">Replied</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge bg-warning text-dark">Pending</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="text-end">
                                    <button class="btn btn-primary btn-sm py-0 px-2"
                                            data-bs-toggle="modal" data-bs-target="#replyModal${t.id}">
                                        Manage Ticket
                                    </button>
                                </td>
                            </tr>

                            <!-- Ticket Reply & Close Modal -->
                            <div class="modal fade" id="replyModal${t.id}" tabindex="-1" aria-hidden="true">
                                <div class="modal-dialog modal-dialog-centered">
                                    <div class="modal-content">
                                        <div class="modal-header">
                                            <div>
                                                <h5 class="modal-title fw-bold">Ticket #<c:out value="${t.id}"/>: <c:out value="${t.subject}"/></h5>
                                                <div class="text-muted small">Submitted by: <c:out value="${t.userName}"/> (<c:out value="${t.userEmail}"/>)</div>
                                            </div>
                                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                                        </div>
                                        <div class="modal-body small">
                                            <div class="mb-3">
                                                <label class="fw-semibold text-muted">User Query / Feedback:</label>
                                                <div class="p-2 bg-light rounded border text-dark">
                                                    <c:out value="${t.body}"/>
                                                </div>
                                            </div>

                                            <form method="post" action="${pageContext.request.contextPath}/admin/feedback" id="replyForm${t.id}">
                                                <input type="hidden" name="csrfToken" value="${csrfToken}">
                                                <input type="hidden" name="ticketId" value="${t.id}">

                                                <div class="mb-3">
                                                    <label class="form-label fw-semibold">Admin Response Note</label>
                                                    <textarea name="adminReply" class="form-control" rows="4" placeholder="Enter official administrative response..." required><c:out value="${t.adminReply}"/></textarea>
                                                </div>

                                                <div class="d-flex justify-content-between align-items-center">
                                                    <div>
                                                        <button type="submit" name="action" value="reply" class="btn btn-outline-primary btn-sm me-2">Save Reply (Keep Open)</button>
                                                        <button type="submit" name="action" value="reply_and_close" class="btn btn-success btn-sm">Reply &amp; Close Ticket</button>
                                                    </div>
                                                </div>
                                            </form>

                                            <c:if test="${t.status == 'OPEN'}">
                                                <hr class="my-3">
                                                <form method="post" action="${pageContext.request.contextPath}/admin/feedback">
                                                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                                                    <input type="hidden" name="ticketId" value="${t.id}">
                                                    <input type="hidden" name="action" value="close">
                                                    <button type="submit" class="btn btn-sm btn-outline-secondary w-100">Close Ticket Without Replying</button>
                                                </form>
                                            </c:if>
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
