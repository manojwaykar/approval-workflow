package com.workflow.approval_workflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * One row of workflow configuration: "for requests of this type, step N must be performed by this role".
 * The whole workflow engine is driven by these rows.
 */
@Entity
@Table(name = "approval_steps",
        uniqueConstraints = @UniqueConstraint(name = "uk_type_step", columnNames = {"request_type", "step_order"}))
public class ApprovalStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_type", nullable = false, length = 50)
    private String requestType;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(name = "role_name", nullable = false, length = 50)
    private String role;

    protected ApprovalStep() {
        // required by JPA
    }

    public ApprovalStep(String requestType, int stepOrder, String role) {
        this.requestType = requestType;
        this.stepOrder = stepOrder;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getRequestType() {
        return requestType;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    public String getRole() {
        return role;
    }
}
