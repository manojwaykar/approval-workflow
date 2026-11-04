package com.workflow.approval_workflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.workflow.approval_workflow.domain.ActingMode;
import com.workflow.approval_workflow.domain.ActionType;
import com.workflow.approval_workflow.domain.AppUser;
import com.workflow.approval_workflow.domain.ApprovalStep;
import com.workflow.approval_workflow.domain.Request;
import com.workflow.approval_workflow.domain.Role;
import com.workflow.approval_workflow.exception.ForbiddenActionException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ActionAuthorizerTest {

    private final ActionAuthorizer authorizer = new ActionAuthorizer();

    private final Role requesterRole = new Role("REQUESTER", false, false);
    private final Role approverRole = new Role("APPROVER", false, true);
    private final Role adminRole = new Role("ADMIN", true, true);

    private final AppUser alice = new AppUser("alice", "x", requesterRole);
    private final AppUser carol = new AppUser("carol", "x", approverRole);
    private final AppUser admin = new AppUser("admin", "x", adminRole);

    private final ApprovalStep approverStep = new ApprovalStep("LEAVE", 1, "APPROVER");

    private Request requestBy(String creator) {
        return Request.create("LEAVE", "x", creator, 1, Instant.now());
    }

    @Test
    void roleAssignedToCurrentStepActsNormally() {
        assertThat(authorizer.authorize(carol, requestBy("alice"), approverStep)).isEqualTo(ActingMode.STEP);
    }

    @Test
    void roleWithOverrideCapabilityCanActOnAnyStep() {
        assertThat(authorizer.authorize(admin, requestBy("alice"), approverStep)).isEqualTo(ActingMode.OVERRIDE);
    }

    @Test
    void roleThatDoesNotMatchTheStepAndHasNoOverrideIsRefused() {
        ApprovalStep adminStep = new ApprovalStep("LEAVE", 2, "ADMIN");

        assertThatThrownBy(() -> authorizer.authorize(carol, requestBy("alice"), adminStep))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessageContaining("requires role ADMIN");
    }

    @Test
    void creatorCanNeverActOnTheirOwnRequest() {
        AppUser approverWhoCreated = new AppUser("carol", "x", approverRole);

        assertThatThrownBy(() -> authorizer.authorize(approverWhoCreated, requestBy("carol"), approverStep))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessageContaining("own request");
    }

    @Test
    void selfApprovalIsBlockedEvenForOverrideRoles() {
        assertThatThrownBy(() -> authorizer.authorize(admin, requestBy("admin"), approverStep))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void ownerCanViewTheirRequest() {
        assertThatCode(() -> authorizer.ensureCanView(alice, requestBy("alice"))).doesNotThrowAnyException();
    }

    @Test
    void viewAllRolesCanViewAnyRequest() {
        assertThatCode(() -> authorizer.ensureCanView(carol, requestBy("alice"))).doesNotThrowAnyException();
        assertThatCode(() -> authorizer.ensureCanView(admin, requestBy("alice"))).doesNotThrowAnyException();
    }

    @Test
    void requesterCannotViewSomeoneElsesRequest() {
        AppUser bob = new AppUser("bob", "x", requesterRole);

        assertThatThrownBy(() -> authorizer.ensureCanView(bob, requestBy("alice")))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void actingModesCarryTheirAuditActionsAndBehaviour() {
        assertThat(ActingMode.STEP.approveAction()).isEqualTo(ActionType.APPROVED);
        assertThat(ActingMode.STEP.rejectAction()).isEqualTo(ActionType.REJECTED);
        assertThat(ActingMode.STEP.completesWorkflowOnApprove()).isFalse();
        assertThat(ActingMode.OVERRIDE.approveAction()).isEqualTo(ActionType.OVERRIDE_APPROVED);
        assertThat(ActingMode.OVERRIDE.rejectAction()).isEqualTo(ActionType.OVERRIDE_REJECTED);
        assertThat(ActingMode.OVERRIDE.completesWorkflowOnApprove()).isTrue();
    }
}
