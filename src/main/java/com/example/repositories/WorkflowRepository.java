package com.example.repositories;

import com.example.entities.Workflow;
import com.example.entities.Request;
import com.example.entities.User;
import com.example.enums.WorkflowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, Long> {

    List<Workflow> findByCurrentAssignee(User currentAssignee);

    List<Workflow> findByStatus(WorkflowStatus status);

    Optional<Workflow> findByRequest(Request request);
    @Query("SELECT w FROM Workflow w WHERE w.currentAssignee IS NULL AND w.status = :status ORDER BY w.createdAt DESC")
    List<Workflow> findUnassignedTasksByStatus(@Param("status") WorkflowStatus status);
    Optional<Workflow> findByRequestId(Long requestId);

    @Query("SELECT w FROM Workflow w " +
       "JOIN FETCH w.request r " +
       "JOIN FETCH r.customer c " +
       "JOIN Prioritization p ON r.id = p.id " +
       "WHERE w.currentAssignee.id = :devId " +
       "AND w.status = com.example.enums.WorkflowStatus.DEVELOPMENT")
    List<Workflow> findAssignedTasksByDeveloperId(@Param("devId") Long devId);
}