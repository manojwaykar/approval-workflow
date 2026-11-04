package com.workflow.approval_workflow.domain;

/** Every entry written to the audit trail has one of these actions. */
public enum ActionType {
    CREATED,
    APPROVED,
    REJECTED,
    OVERRIDE_APPROVED,
    OVERRIDE_REJECTED
}
