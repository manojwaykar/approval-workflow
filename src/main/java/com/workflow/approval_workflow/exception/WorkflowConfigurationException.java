package com.workflow.approval_workflow.exception;

/** Thrown when the workflow configuration in the database is inconsistent (HTTP 500). */
public class WorkflowConfigurationException extends RuntimeException {

    public WorkflowConfigurationException(String message) {
        super(message);
    }
}
