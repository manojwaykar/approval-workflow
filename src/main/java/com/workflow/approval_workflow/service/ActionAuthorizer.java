package com.workflow.approval_workflow.service;

import com.workflow.approval_workflow.domain.ActingMode;
import com.workflow.approval_workflow.domain.AppUser;
import com.workflow.approval_workflow.domain.ApprovalStep;
import com.workflow.approval_workflow.domain.Request;
import com.workflow.approval_workflow.exception.ForbiddenActionException;
import org.springframework.stereotype.Component;

/**
 * Business authorization rules. Every decision is based on DATA (the step's role and the role's
 * capability flags), never on hardcoded role names or request types.
 */
@Component
public class ActionAuthorizer {

    /**
     * Decides whether {@code actor} may approve/reject {@code request} at {@code currentStep}.
     * <ol>
     *   <li>Nobody may act on a request they created.</li>
     *   <li>The role assigned to the current step may act normally.</li>
     *   <li>A role with the override capability may act on any step.</li>
     *   <li>Everyone else is refused.</li>
     * </ol>
     */
    public ActingMode authorize(AppUser actor, Request request, ApprovalStep currentStep) {
        if (request.getCreatedBy().equals(actor.getUsername())) {
            throw new ForbiddenActionException("A requester cannot approve or reject their own request");
        }
        String actorRole = actor.getRole().getName();
        if (currentStep.getRole().equals(actorRole)) {
            return ActingMode.STEP;
        }
        if (actor.getRole().isOverrideAllowed()) {
            return ActingMode.OVERRIDE;
        }
        throw new ForbiddenActionException("Step " + currentStep.getStepOrder() + " of this request requires role "
                + currentStep.getRole() + "; your role is " + actorRole);
    }

    /** Requests are visible to their creator and to roles with the view-all capability. */
    public void ensureCanView(AppUser actor, Request request) {
        boolean isOwner = request.getCreatedBy().equals(actor.getUsername());
        if (!isOwner && !actor.getRole().isViewAllAllowed()) {
            throw new ForbiddenActionException("You can only access requests that you created");
        }
    }
}
