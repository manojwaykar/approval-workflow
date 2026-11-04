package com.workflow.approval_workflow.dto;

import com.workflow.approval_workflow.domain.ApprovalStep;

public record StepResponse(int stepOrder, String role) {

    public static StepResponse from(ApprovalStep step) {
        return new StepResponse(step.getStepOrder(), step.getRole());
    }
}
