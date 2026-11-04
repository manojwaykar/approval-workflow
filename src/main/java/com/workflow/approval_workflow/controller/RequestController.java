package com.workflow.approval_workflow.controller;

import com.workflow.approval_workflow.dto.ActionRequest;
import com.workflow.approval_workflow.dto.CreateRequestDto;
import com.workflow.approval_workflow.dto.HistoryResponse;
import com.workflow.approval_workflow.dto.RequestResponse;
import com.workflow.approval_workflow.service.WorkflowService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Thin HTTP layer: endpoint-level role checks here, all business rules in {@link WorkflowService}. */
@RestController
@RequestMapping("/requests")
public class RequestController {

    private final WorkflowService workflowService;

    public RequestController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping
    @PreAuthorize("hasRole('REQUESTER')")
    public ResponseEntity<RequestResponse> create(@Valid @RequestBody CreateRequestDto body,
                                                  Authentication authentication) {
        RequestResponse created = workflowService.createRequest(body, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('REQUESTER', 'APPROVER', 'ADMIN')")
    public RequestResponse get(@PathVariable Long id, Authentication authentication) {
        return workflowService.getRequest(id, authentication.getName());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")   // ADMIN is allowed so it can override
    public RequestResponse approve(@PathVariable Long id,
                                   @Valid @RequestBody(required = false) ActionRequest body,
                                   Authentication authentication) {
        return workflowService.approve(id, authentication.getName(), commentsOf(body));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
    public RequestResponse reject(@PathVariable Long id,
                                  @Valid @RequestBody(required = false) ActionRequest body,
                                  Authentication authentication) {
        return workflowService.reject(id, authentication.getName(), commentsOf(body));
    }

    @GetMapping("/history/{id}")
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN')")
    public List<HistoryResponse> history(@PathVariable Long id, Authentication authentication) {
        return workflowService.getHistory(id, authentication.getName());
    }

    private static String commentsOf(ActionRequest body) {
        return body == null ? null : body.comments();
    }
}
