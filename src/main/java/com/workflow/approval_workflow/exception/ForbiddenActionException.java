package com.workflow.approval_workflow.exception;

/** Thrown when a business authorization rule forbids the action (HTTP 403). */
public class ForbiddenActionException extends RuntimeException {

    public ForbiddenActionException(String message) {
        super(message);
    }
}
