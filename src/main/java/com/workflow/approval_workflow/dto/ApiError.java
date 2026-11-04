package com.workflow.approval_workflow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;

/** Uniform error body returned for every failure. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(Instant timestamp, int status, String error, String message, String path,
                       List<String> details) {

    public static ApiError of(HttpStatus status, String message, String path, List<String> details) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, details);
    }
}
