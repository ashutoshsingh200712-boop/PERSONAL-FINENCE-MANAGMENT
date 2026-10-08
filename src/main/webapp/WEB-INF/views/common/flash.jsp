<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:if test="${not empty sessionScope.flashMessage}">
    <div class="alert alert-${empty sessionScope.flashType ? 'info' : sessionScope.flashType} alert-dismissible fade show my-3 shadow-sm" role="alert">
        <c:out value="${sessionScope.flashMessage}"/>
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
    </div>
    <c:remove var="flashMessage" scope="session"/>
    <c:remove var="flashType" scope="session"/>
</c:if>
