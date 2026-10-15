package com.financeapp.model;

import java.time.LocalDateTime;

/**
 * Model representing rule-based or advisor-created financial advice.
 */
public class Advice {

    public enum Status {
        NEW,
        ACCEPTED,
        IGNORED;

        public static Status fromString(String val) {
            if (val == null) return NEW;
            try {
                return Status.valueOf(val.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return NEW;
            }
        }
    }

    private Long id;
    private Long userId;
    private Long planId;
    private String ruleName;
    private String title;
    private String explanation;
    private Status status = Status.NEW;
    private String advisorComment;
    private LocalDateTime createdAt;

    public Advice() {
    }

    public Advice(Long id, Long userId, Long planId, String ruleName, String title,
                  String explanation, Status status, String advisorComment, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.planId = planId;
        this.ruleName = ruleName;
        this.title = title;
        this.explanation = explanation;
        this.status = (status != null) ? status : Status.NEW;
        this.advisorComment = advisorComment;
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getAdvisorComment() {
        return advisorComment;
    }

    public void setAdvisorComment(String advisorComment) {
        this.advisorComment = advisorComment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
