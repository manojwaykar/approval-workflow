package com.workflow.approval_workflow.dto;

import com.workflow.approval_workflow.domain.ActionType;
import com.workflow.approval_workflow.domain.ApprovalHistory;
import java.time.Instant;

public record HistoryResponse(Long id, Long requestId, ActionType action, String actionBy, Instant actionAt,
                              Integer stepOrder, String comments) {

    public static HistoryResponse from(ApprovalHistory entry) {
        return new HistoryResponse(entry.getId(), entry.getRequestId(), entry.getAction(), entry.getActionBy(),
                entry.getActionAt(), entry.getStepOrder(), entry.getComments());
    }
}
