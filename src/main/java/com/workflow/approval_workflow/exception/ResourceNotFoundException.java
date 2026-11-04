package com.workflow.approval_workflow.exception;

/** Thrown when a request does not exist (HTTP 404). */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
