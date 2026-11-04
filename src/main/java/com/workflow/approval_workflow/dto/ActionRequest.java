package com.workflow.approval_workflow.dto;

import jakarta.validation.constraints.Size;

/** Optional body of approve / reject calls. */
public record ActionRequest(
        @Size(max = 500, message = "comments must be at most 500 characters") String comments) {
}
