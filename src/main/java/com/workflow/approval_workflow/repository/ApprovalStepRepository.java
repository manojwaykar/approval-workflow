package com.workflow.approval_workflow.repository;

import com.workflow.approval_workflow.domain.ApprovalStep;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Reads the workflow configuration. The engine only ever asks the database "what is the next step?". */
public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Long> {

    Optional<ApprovalStep> findFirstByRequestTypeOrderByStepOrderAsc(String requestType);

    Optional<ApprovalStep> findByRequestTypeAndStepOrder(String requestType, int stepOrder);

    Optional<ApprovalStep> findFirstByRequestTypeAndStepOrderGreaterThanOrderByStepOrderAsc(
            String requestType, int stepOrder);
}
