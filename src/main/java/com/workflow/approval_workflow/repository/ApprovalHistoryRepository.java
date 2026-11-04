package com.workflow.approval_workflow.repository;

import com.workflow.approval_workflow.domain.ApprovalHistory;
import java.util.List;
import org.springframework.data.repository.Repository;

/**
 * Deliberately NOT a JpaRepository: only insert and read are exposed, so the audit trail cannot be
 * updated or deleted through the application.
 */
public interface ApprovalHistoryRepository extends Repository<ApprovalHistory, Long> {

    ApprovalHistory save(ApprovalHistory entry);

    List<ApprovalHistory> findByRequestIdOrderByIdAsc(Long requestId);
}
