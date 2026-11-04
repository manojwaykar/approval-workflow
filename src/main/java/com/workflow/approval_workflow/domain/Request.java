package com.workflow.approval_workflow.domain;

import com.workflow.approval_workflow.exception.InvalidWorkflowStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/**
 * A request travelling through its workflow. All state transitions go through the methods below so an
 * invalid transition (acting on an already finished request) can never be performed by accident.
 */
@Entity
@Table(name = "requests")
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_type", nullable = false, length = 50)
    private String type;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    @Column(name = "created_by", nullable = false, updatable = false, length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** step_order of the step currently waiting for action; null once the workflow is finished. */
    @Column(name = "current_step_order")
    private Integer currentStepOrder;

    /** Optimistic lock: a second line of defence next to the pessimistic lock used by the service. */
    @Version
    private Long version;

    protected Request() {
        // required by JPA
    }

    private Request(String type, String description, String createdBy, int firstStepOrder, Instant createdAt) {
        this.type = type;
        this.description = description;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.status = RequestStatus.PENDING;
        this.currentStepOrder = firstStepOrder;
    }

    public static Request create(String type, String description, String createdBy, int firstStepOrder,
                                 Instant createdAt) {
        return new Request(type, description, createdBy, firstStepOrder, createdAt);
    }

    /** Fails with a conflict when the request has already been approved or rejected. */
    public void ensurePending() {
        if (status != RequestStatus.PENDING) {
            throw new InvalidWorkflowStateException(
                    "Request " + id + " is already " + status + "; no further actions are allowed");
        }
    }

    public void advanceTo(int nextStepOrder) {
        ensurePending();
        this.currentStepOrder = nextStepOrder;
    }

    public void approve() {
        ensurePending();
        this.status = RequestStatus.APPROVED;
        this.currentStepOrder = null;
    }

    public void reject() {
        ensurePending();
        this.status = RequestStatus.REJECTED;
        this.currentStepOrder = null;
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Integer getCurrentStepOrder() {
        return currentStepOrder;
    }
}
