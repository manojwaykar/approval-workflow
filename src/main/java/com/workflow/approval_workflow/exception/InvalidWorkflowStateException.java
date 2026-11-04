package com.workflow.approval_workflow.exception;

/** Thrown when an action is attempted on a request that is no longer PENDING (HTTP 409). */
public class InvalidWorkflowStateException extends RuntimeException {

    public InvalidWorkflowStateException(String message) {
        super(message);
    }
}
