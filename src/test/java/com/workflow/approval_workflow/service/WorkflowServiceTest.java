package com.workflow.approval_workflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workflow.approval_workflow.domain.ActionType;
import com.workflow.approval_workflow.domain.AppUser;
import com.workflow.approval_workflow.domain.ApprovalHistory;
import com.workflow.approval_workflow.domain.ApprovalStep;
import com.workflow.approval_workflow.domain.Request;
import com.workflow.approval_workflow.domain.RequestStatus;
import com.workflow.approval_workflow.domain.Role;
import com.workflow.approval_workflow.dto.CreateRequestDto;
import com.workflow.approval_workflow.dto.HistoryResponse;
import com.workflow.approval_workflow.dto.RequestResponse;
import com.workflow.approval_workflow.exception.ForbiddenActionException;
import com.workflow.approval_workflow.exception.InvalidRequestTypeException;
import com.workflow.approval_workflow.exception.InvalidWorkflowStateException;
import com.workflow.approval_workflow.exception.ResourceNotFoundException;
import com.workflow.approval_workflow.exception.WorkflowConfigurationException;
import com.workflow.approval_workflow.repository.ApprovalHistoryRepository;
import com.workflow.approval_workflow.repository.ApprovalStepRepository;
import com.workflow.approval_workflow.repository.RequestRepository;
import com.workflow.approval_workflow.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkflowServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @Mock
    private RequestRepository requestRepository;
    @Mock
    private ApprovalStepRepository stepRepository;
    @Mock
    private ApprovalHistoryRepository historyRepository;
    @Mock
    private UserRepository userRepository;

    private final Role requesterRole = new Role("REQUESTER", false, false);
    private final Role approverRole = new Role("APPROVER", false, true);
    private final Role adminRole = new Role("ADMIN", true, true);

    private final AppUser alice = new AppUser("alice", "x", requesterRole);
    private final AppUser bob = new AppUser("bob", "x", requesterRole);
    private final AppUser carol = new AppUser("carol", "x", approverRole);
    private final AppUser admin = new AppUser("admin", "x", adminRole);

    private final ApprovalStep leave1 = new ApprovalStep("LEAVE", 1, "APPROVER");
    private final ApprovalStep leave2 = new ApprovalStep("LEAVE", 2, "ADMIN");

    private WorkflowService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new WorkflowService(requestRepository, stepRepository, historyRepository, userRepository,
                new ActionAuthorizer(), clock);

        for (AppUser user : List.of(alice, bob, carol, admin)) {
            when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        }
        when(stepRepository.findFirstByRequestTypeOrderByStepOrderAsc("LEAVE")).thenReturn(Optional.of(leave1));
        when(stepRepository.findByRequestTypeAndStepOrder("LEAVE", 1)).thenReturn(Optional.of(leave1));
        when(stepRepository.findByRequestTypeAndStepOrder("LEAVE", 2)).thenReturn(Optional.of(leave2));
        when(stepRepository.findFirstByRequestTypeAndStepOrderGreaterThanOrderByStepOrderAsc("LEAVE", 1))
                .thenReturn(Optional.of(leave2));
        when(requestRepository.save(any(Request.class))).thenAnswer(invocation -> {
            Request saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            return saved;
        });
    }

    private Request pendingLeaveRequest(int stepOrder) {
        Request request = Request.create("LEAVE", "trip", "alice", 1, NOW);
        ReflectionTestUtils.setField(request, "id", 10L);
        if (stepOrder != 1) {
            request.advanceTo(stepOrder);
        }
        when(requestRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(request));
        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));
        return request;
    }

    private ApprovalHistory savedHistory() {
        ArgumentCaptor<ApprovalHistory> captor = ArgumentCaptor.forClass(ApprovalHistory.class);
        verify(historyRepository).save(captor.capture());
        return captor.getValue();
    }

    // ------------------------------------------------------------------ create

    @Test
    void createInitialisesWorkflowAtFirstConfiguredStepAndWritesAuditEntry() {
        RequestResponse response = service.createRequest(new CreateRequestDto(" leave ", "Family trip"), "alice");

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.type()).isEqualTo("LEAVE");
        assertThat(response.status()).isEqualTo(RequestStatus.PENDING);
        assertThat(response.createdBy()).isEqualTo("alice");
        assertThat(response.createdAt()).isEqualTo(NOW);
        assertThat(response.currentStep().stepOrder()).isEqualTo(1);
        assertThat(response.currentStep().role()).isEqualTo("APPROVER");

        ApprovalHistory entry = savedHistory();
        assertThat(entry.getAction()).isEqualTo(ActionType.CREATED);
        assertThat(entry.getActionBy()).isEqualTo("alice");
        assertThat(entry.getRequestId()).isEqualTo(10L);
        assertThat(entry.getActionAt()).isEqualTo(NOW);
    }

    @Test
    void createFailsWhenNoWorkflowIsConfiguredForTheType() {
        assertThatThrownBy(() -> service.createRequest(new CreateRequestDto("TRAVEL", null), "alice"))
                .isInstanceOf(InvalidRequestTypeException.class)
                .hasMessageContaining("TRAVEL");

        verify(requestRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ approve

    @Test
    void approveMovesToNextConfiguredStep() {
        Request request = pendingLeaveRequest(1);

        RequestResponse response = service.approve(10L, "carol", "looks fine");

        assertThat(request.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(request.getCurrentStepOrder()).isEqualTo(2);
        assertThat(response.currentStep().stepOrder()).isEqualTo(2);
        assertThat(response.currentStep().role()).isEqualTo("ADMIN");

        ApprovalHistory entry = savedHistory();
        assertThat(entry.getAction()).isEqualTo(ActionType.APPROVED);
        assertThat(entry.getActionBy()).isEqualTo("carol");
        assertThat(entry.getStepOrder()).isEqualTo(1);
        assertThat(entry.getComments()).isEqualTo("looks fine");
    }

    @Test
    void approveOnLastStepCompletesTheWorkflow() {
        Request request = pendingLeaveRequest(2);

        RequestResponse response = service.approve(10L, "admin", null);

        assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(request.getCurrentStepOrder()).isNull();
        assertThat(response.status()).isEqualTo(RequestStatus.APPROVED);
        assertThat(response.currentStep()).isNull();
        assertThat(savedHistory().getAction()).isEqualTo(ActionType.APPROVED);
    }

    @Test
    void adminOverrideApproveFinalisesWorkflowImmediately() {
        Request request = pendingLeaveRequest(1);

        service.approve(10L, "admin", "urgent");

        assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(savedHistory().getAction()).isEqualTo(ActionType.OVERRIDE_APPROVED);
        verify(stepRepository, never())
                .findFirstByRequestTypeAndStepOrderGreaterThanOrderByStepOrderAsc("LEAVE", 1);
    }

    @Test
    void approveByWrongRoleIsForbiddenAndNothingIsWritten() {
        Request request = pendingLeaveRequest(2);

        assertThatThrownBy(() -> service.approve(10L, "carol", null))
                .isInstanceOf(ForbiddenActionException.class);

        assertThat(request.getCurrentStepOrder()).isEqualTo(2);
        verify(historyRepository, never()).save(any());
    }

    @Test
    void approveOwnRequestIsForbidden() {
        pendingLeaveRequest(1);
        AppUser creatorWhoIsAlsoApprover = new AppUser("alice", "x", approverRole);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(creatorWhoIsAlsoApprover));

        assertThatThrownBy(() -> service.approve(10L, "alice", null))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessageContaining("own request");
    }

    @Test
    void approveFinishedRequestFailsWithInvalidState() {
        Request request = pendingLeaveRequest(1);
        request.approve();

        assertThatThrownBy(() -> service.approve(10L, "carol", null))
                .isInstanceOf(InvalidWorkflowStateException.class);
        verify(historyRepository, never()).save(any());
    }

    @Test
    void approveUnknownRequestFailsWithNotFound() {
        assertThatThrownBy(() -> service.approve(99L, "carol", null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void approveFailsClearlyWhenWorkflowConfigurationIsMissing() {
        pendingLeaveRequest(1);
        when(stepRepository.findByRequestTypeAndStepOrder("LEAVE", 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(10L, "carol", null))
                .isInstanceOf(WorkflowConfigurationException.class);
    }

    // ------------------------------------------------------------------ reject

    @Test
    void rejectTerminatesWorkflowAndRecordsHistory() {
        Request request = pendingLeaveRequest(1);

        RequestResponse response = service.reject(10L, "carol", "not now");

        assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(response.status()).isEqualTo(RequestStatus.REJECTED);
        assertThat(response.currentStep()).isNull();
        ApprovalHistory entry = savedHistory();
        assertThat(entry.getAction()).isEqualTo(ActionType.REJECTED);
        assertThat(entry.getComments()).isEqualTo("not now");
    }

    @Test
    void adminOverrideRejectIsAuditedAsOverride() {
        pendingLeaveRequest(1);

        service.reject(10L, "admin", null);

        assertThat(savedHistory().getAction()).isEqualTo(ActionType.OVERRIDE_REJECTED);
    }

    @Test
    void rejectFinishedRequestFailsWithInvalidState() {
        Request request = pendingLeaveRequest(1);
        request.reject();

        assertThatThrownBy(() -> service.reject(10L, "carol", null))
                .isInstanceOf(InvalidWorkflowStateException.class);
    }

    @Test
    void rejectByWrongRoleIsForbidden() {
        pendingLeaveRequest(2);

        assertThatThrownBy(() -> service.reject(10L, "carol", null))
                .isInstanceOf(ForbiddenActionException.class);
    }

    // ------------------------------------------------------------------ read

    @Test
    void ownerCanReadRequestWithCurrentStep() {
        pendingLeaveRequest(2);

        RequestResponse response = service.getRequest(10L, "alice");

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.currentStep().role()).isEqualTo("ADMIN");
    }

    @Test
    void finishedRequestHasNoCurrentStep() {
        Request request = pendingLeaveRequest(1);
        request.approve();

        assertThat(service.getRequest(10L, "carol").currentStep()).isNull();
    }

    @Test
    void otherRequesterCannotReadRequest() {
        pendingLeaveRequest(1);

        assertThatThrownBy(() -> service.getRequest(10L, "bob")).isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void readUnknownRequestFailsWithNotFound() {
        assertThatThrownBy(() -> service.getRequest(404L, "alice")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void unknownAuthenticatedUserIsRejected() {
        pendingLeaveRequest(1);

        assertThatThrownBy(() -> service.getRequest(10L, "ghost")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void historyIsReturnedInInsertionOrder() {
        pendingLeaveRequest(1);
        when(historyRepository.findByRequestIdOrderByIdAsc(10L)).thenReturn(List.of(
                new ApprovalHistory(10L, ActionType.CREATED, "alice", NOW, 1, null),
                new ApprovalHistory(10L, ActionType.APPROVED, "carol", NOW, 1, "ok")));

        List<HistoryResponse> history = service.getHistory(10L, "alice");

        assertThat(history).extracting(HistoryResponse::action)
                .containsExactly(ActionType.CREATED, ActionType.APPROVED);
        assertThat(history.get(1).actionBy()).isEqualTo("carol");
    }

    @Test
    void historyIsHiddenFromOtherRequesters() {
        pendingLeaveRequest(1);

        assertThatThrownBy(() -> service.getHistory(10L, "bob")).isInstanceOf(ForbiddenActionException.class);
    }
}
