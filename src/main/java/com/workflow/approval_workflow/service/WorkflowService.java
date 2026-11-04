package com.workflow.approval_workflow.service;

import com.workflow.approval_workflow.domain.ActingMode;
import com.workflow.approval_workflow.domain.ActionType;
import com.workflow.approval_workflow.domain.AppUser;
import com.workflow.approval_workflow.domain.ApprovalHistory;
import com.workflow.approval_workflow.domain.ApprovalStep;
import com.workflow.approval_workflow.domain.Request;
import com.workflow.approval_workflow.dto.CreateRequestDto;
import com.workflow.approval_workflow.dto.HistoryResponse;
import com.workflow.approval_workflow.dto.RequestResponse;
import com.workflow.approval_workflow.exception.InvalidRequestTypeException;
import com.workflow.approval_workflow.exception.ResourceNotFoundException;
import com.workflow.approval_workflow.exception.WorkflowConfigurationException;
import com.workflow.approval_workflow.repository.ApprovalHistoryRepository;
import com.workflow.approval_workflow.repository.ApprovalStepRepository;
import com.workflow.approval_workflow.repository.RequestRepository;
import com.workflow.approval_workflow.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The workflow engine. It knows nothing about LEAVE, EXPENSE or any role: it asks the database for the
 * steps of the request's type and walks them in order.
 *
 * Every public state-changing method is a single transaction: the request update and the audit entry
 * are committed together or not at all.
 */
@Service
public class WorkflowService {

    private final RequestRepository requestRepository;
    private final ApprovalStepRepository stepRepository;
    private final ApprovalHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final ActionAuthorizer authorizer;
    private final Clock clock;

    public WorkflowService(RequestRepository requestRepository, ApprovalStepRepository stepRepository,
                           ApprovalHistoryRepository historyRepository, UserRepository userRepository,
                           ActionAuthorizer authorizer, Clock clock) {
        this.requestRepository = requestRepository;
        this.stepRepository = stepRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.authorizer = authorizer;
        this.clock = clock;
    }

    /** Creates a request and initialises its workflow at the first configured step. */
    @Transactional
    public RequestResponse createRequest(CreateRequestDto dto, String creatorUsername) {
        String type = dto.type().trim().toUpperCase(Locale.ROOT);
        ApprovalStep firstStep = stepRepository.findFirstByRequestTypeOrderByStepOrderAsc(type)
                .orElseThrow(() -> new InvalidRequestTypeException(
                        "No approval workflow is configured for request type '" + type + "'"));

        Request saved = requestRepository.save(
                Request.create(type, dto.description(), creatorUsername, firstStep.getStepOrder(), now()));
        recordHistory(saved.getId(), ActionType.CREATED, creatorUsername, firstStep.getStepOrder(), null);
        return RequestResponse.from(saved, firstStep);
    }

    @Transactional(readOnly = true)
    public RequestResponse getRequest(Long id, String actorUsername) {
        Request request = findRequest(id);
        authorizer.ensureCanView(loadUser(actorUsername), request);

        ApprovalStep currentStep = request.getCurrentStepOrder() == null
                ? null
                : stepRepository.findByRequestTypeAndStepOrder(request.getType(), request.getCurrentStepOrder())
                        .orElse(null);
        return RequestResponse.from(request, currentStep);
    }

    /** Approves the current step and moves to the next configured step, or completes the workflow. */
    @Transactional
    public RequestResponse approve(Long id, String actorUsername, String comments) {
        Request request = lockPendingRequest(id);
        AppUser actor = loadUser(actorUsername);
        ApprovalStep current = currentStepOf(request);
        ActingMode mode = authorizer.authorize(actor, request, current);

        Optional<ApprovalStep> next = mode.completesWorkflowOnApprove()
                ? Optional.empty()
                : stepRepository.findFirstByRequestTypeAndStepOrderGreaterThanOrderByStepOrderAsc(
                        request.getType(), current.getStepOrder());

        next.ifPresentOrElse(step -> request.advanceTo(step.getStepOrder()), request::approve);
        recordHistory(request.getId(), mode.approveAction(), actorUsername, current.getStepOrder(), comments);
        return RequestResponse.from(request, next.orElse(null));
    }

    /** Rejects the request and terminates the workflow. */
    @Transactional
    public RequestResponse reject(Long id, String actorUsername, String comments) {
        Request request = lockPendingRequest(id);
        AppUser actor = loadUser(actorUsername);
        ApprovalStep current = currentStepOf(request);
        ActingMode mode = authorizer.authorize(actor, request, current);

        request.reject();
        recordHistory(request.getId(), mode.rejectAction(), actorUsername, current.getStepOrder(), comments);
        return RequestResponse.from(request, null);
    }

    @Transactional(readOnly = true)
    public List<HistoryResponse> getHistory(Long id, String actorUsername) {
        Request request = findRequest(id);
        authorizer.ensureCanView(loadUser(actorUsername), request);
        return historyRepository.findByRequestIdOrderByIdAsc(id).stream().map(HistoryResponse::from).toList();
    }

    // ------------------------------------------------------------------ helpers

    private Request findRequest(Long id) {
        return requestRepository.findById(id).orElseThrow(() -> notFound(id));
    }

    /** Locks the row, then rejects any attempt to act on a finished request (invalid transition -> 409). */
    private Request lockPendingRequest(Long id) {
        Request request = requestRepository.findByIdForUpdate(id).orElseThrow(() -> notFound(id));
        request.ensurePending();
        return request;
    }

    private ApprovalStep currentStepOf(Request request) {
        return stepRepository.findByRequestTypeAndStepOrder(request.getType(), request.getCurrentStepOrder())
                .orElseThrow(() -> new WorkflowConfigurationException("Request " + request.getId()
                        + " points to step " + request.getCurrentStepOrder() + " of type '" + request.getType()
                        + "', but that step is not configured"));
    }

    private AppUser loadUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User '" + username + "' does not exist"));
    }

    private void recordHistory(Long requestId, ActionType action, String actor, Integer stepOrder, String comments) {
        historyRepository.save(new ApprovalHistory(requestId, action, actor, now(), stepOrder, comments));
    }

    private Instant now() {
        return clock.instant();
    }

    private static ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("Request " + id + " not found");
    }
}
