package com.workflow.approval_workflow.domain;

/** Lifecycle of a request. PENDING means "still moving through its approval steps". */
public enum RequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}
