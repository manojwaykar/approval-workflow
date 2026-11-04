package com.workflow.approval_workflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Immutable;

/**
 * One immutable audit entry. {@link Immutable} makes Hibernate ignore updates, every column is
 * {@code updatable = false}, and the repository exposes only "save" and "find" (no update/delete).
 */
@Entity
@Immutable
@Table(name = "approval_history", indexes = @Index(name = "idx_history_request", columnList = "request_id"))
public class ApprovalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, updatable = false)
    private Long requestId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private ActionType action;

    @Column(name = "action_by", nullable = false, updatable = false, length = 100)
    private String actionBy;

    @Column(name = "action_at", nullable = false, updatable = false)
    private Instant actionAt;

    @Column(name = "step_order", updatable = false)
    private Integer stepOrder;

    @Column(length = 500, updatable = false)
    private String comments;

    protected ApprovalHistory() {
        // required by JPA
    }

    public ApprovalHistory(Long requestId, ActionType action, String actionBy, Instant actionAt,
                           Integer stepOrder, String comments) {
        this.requestId = requestId;
        this.action = action;
        this.actionBy = actionBy;
        this.actionAt = actionAt;
        this.stepOrder = stepOrder;
        this.comments = comments;
    }

    public Long getId() {
        return id;
    }

    public Long getRequestId() {
        return requestId;
    }

    public ActionType getAction() {
        return action;
    }

    public String getActionBy() {
        return actionBy;
    }

    public Instant getActionAt() {
        return actionAt;
    }

    public Integer getStepOrder() {
        return stepOrder;
    }

    public String getComments() {
        return comments;
    }
}
