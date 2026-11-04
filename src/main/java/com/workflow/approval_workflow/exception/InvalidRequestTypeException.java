package com.workflow.approval_workflow.exception;

/** Thrown when no workflow is configured for the requested type (HTTP 400). */
public class InvalidRequestTypeException extends RuntimeException {

    public InvalidRequestTypeException(String message) {
        super(message);
    }
}
