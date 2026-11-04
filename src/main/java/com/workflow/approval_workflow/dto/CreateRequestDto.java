package com.workflow.approval_workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateRequestDto(
        @NotBlank(message = "type is required")
        @Size(max = 50, message = "type must be at most 50 characters")
        @Pattern(regexp = "^[A-Za-z0-9_-]*$", message = "type may only contain letters, digits, '_' and '-'")
        String type,

        @Size(max = 500, message = "description must be at most 500 characters")
        String description) {
}
