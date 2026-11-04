package com.workflow.approval_workflow.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.workflow.approval_workflow.exception.InvalidWorkflowStateException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RequestTest {

    private Request newRequest() {
        return Request.create("LEAVE", "Two days off", "alice", 1, Instant.parse("2026-01-01T10:00:00Z"));
    }

    @Test
    void newRequestIsPendingAtFirstStep() {
        Request request = newRequest();

        assertThat(request.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(request.getCurrentStepOrder()).isEqualTo(1);
        assertThat(request.getType()).isEqualTo("LEAVE");
        assertThat(request.getDescription()).isEqualTo("Two days off");
        assertThat(request.getCreatedBy()).isEqualTo("alice");
        assertThat(request.getCreatedAt()).isEqualTo(Instant.parse("2026-01-01T10:00:00Z"));
    }

    @Test
    void advanceMovesToTheNextStepAndKeepsRequestPending() {
        Request request = newRequest();

        request.advanceTo(2);

        assertThat(request.getCurrentStepOrder()).isEqualTo(2);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.PENDING);
    }

    @Test
    void approveCompletesTheWorkflow() {
        Request request = newRequest();

        request.approve();

        assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(request.getCurrentStepOrder()).isNull();
    }

    @Test
    void rejectTerminatesTheWorkflow() {
        Request request = newRequest();

        request.reject();

        assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(request.getCurrentStepOrder()).isNull();
    }

    @Test
    void finishedRequestRejectsEveryFurtherTransition() {
        Request approved = newRequest();
        approved.approve();
        Request rejected = newRequest();
        rejected.reject();

        assertThatThrownBy(approved::approve).isInstanceOf(InvalidWorkflowStateException.class);
        assertThatThrownBy(approved::reject).isInstanceOf(InvalidWorkflowStateException.class);
        assertThatThrownBy(() -> approved.advanceTo(2)).isInstanceOf(InvalidWorkflowStateException.class);
        assertThatThrownBy(rejected::approve).isInstanceOf(InvalidWorkflowStateException.class);
        assertThatThrownBy(rejected::ensurePending)
                .isInstanceOf(InvalidWorkflowStateException.class)
                .hasMessageContaining("REJECTED");
    }
}
