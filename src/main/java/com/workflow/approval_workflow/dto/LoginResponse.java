package com.workflow.approval_workflow.dto;

public record LoginResponse(String token, String tokenType, long expiresInMs, String username, String role) {
}
