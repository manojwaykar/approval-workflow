package com.workflow.approval_workflow.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.workflow.approval_workflow.domain.Request;
import com.workflow.approval_workflow.dto.ApiError;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/requests/1/approve");

    @Test
    void concurrentModificationBecomesConflict() {
        ResponseEntity<ApiError> response =
                handler.handleConcurrency(new ObjectOptimisticLockingFailureException(Request.class, 1L), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).contains("retry");
        assertThat(response.getBody().path()).isEqualTo("/requests/1/approve");
    }

    @Test
    void inconsistentConfigurationBecomesServerError() {
        ResponseEntity<ApiError> response =
                handler.handleConfiguration(new WorkflowConfigurationException("step 3 missing"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).contains("step 3 missing");
    }

    @Test
    void unexpectedErrorsNeverLeakInternalDetails() {
        ResponseEntity<ApiError> response = handler.handleUnexpected(new IllegalStateException("db password"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).doesNotContain("db password");
    }
}
