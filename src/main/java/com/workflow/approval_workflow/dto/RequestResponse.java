package com.workflow.approval_workflow.dto;

import com.workflow.approval_workflow.domain.ApprovalStep;
import com.workflow.approval_workflow.domain.Request;
import com.workflow.approval_workflow.domain.RequestStatus;
import java.time.Instant;

/** Request details plus the step that is currently waiting for action (null when finished). */
public record RequestResponse(Long id, String type, String description, RequestStatus status, String createdBy,
                              Instant createdAt, StepResponse currentStep) {

    public static RequestResponse from(Request request, ApprovalStep currentStep) {
        StepResponse step = currentStep == null ? null : StepResponse.from(currentStep);
        return new RequestResponse(request.getId(), request.getType(), request.getDescription(),
                request.getStatus(), request.getCreatedBy(), request.getCreatedAt(), step);
    }
}
