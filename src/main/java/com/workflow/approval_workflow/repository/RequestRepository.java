package com.workflow.approval_workflow.repository;

import com.workflow.approval_workflow.domain.Request;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequestRepository extends JpaRepository<Request, Long> {

    /**
     * Loads the request with a row lock (SELECT ... FOR UPDATE) so two approvers acting at the same time
     * are serialised and the second one sees the already-updated state.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Request r where r.id = :id")
    Optional<Request> findByIdForUpdate(@Param("id") Long id);
}
