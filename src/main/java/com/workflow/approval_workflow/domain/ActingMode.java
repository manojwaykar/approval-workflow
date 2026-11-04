package com.workflow.approval_workflow.domain;

/**
 * How an actor is allowed to act on the current step. The mode carries the behaviour
 * (which audit action to record, whether approval ends the workflow) so the service
 * never needs an if/else on role names.
 */
public enum ActingMode {

    /** The actor holds the role assigned to the current step: normal flow. */
    STEP(ActionType.APPROVED, ActionType.REJECTED, false),

    /** The actor's role carries the override capability: acts on any step and finalises the workflow. */
    OVERRIDE(ActionType.OVERRIDE_APPROVED, ActionType.OVERRIDE_REJECTED, true);

    private final ActionType approveAction;
    private final ActionType rejectAction;
    private final boolean completesWorkflowOnApprove;

    ActingMode(ActionType approveAction, ActionType rejectAction, boolean completesWorkflowOnApprove) {
        this.approveAction = approveAction;
        this.rejectAction = rejectAction;
        this.completesWorkflowOnApprove = completesWorkflowOnApprove;
    }

    public ActionType approveAction() {
        return approveAction;
    }

    public ActionType rejectAction() {
        return rejectAction;
    }

    public boolean completesWorkflowOnApprove() {
        return completesWorkflowOnApprove;
    }
}
