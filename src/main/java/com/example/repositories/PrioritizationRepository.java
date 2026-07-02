package com.example.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.entities.Prioritization;
import com.example.entities.Workflow;
import com.example.entities.User;
import com.example.enums.WorkflowStatus;

@Repository
public interface PrioritizationRepository extends JpaRepository<Prioritization, Long> {
    
    @Query("SELECT p FROM Prioritization p JOIN FETCH p.request r JOIN FETCH r.customer")
    List<Prioritization> findAllWithDetails();

    @Query("SELECT w FROM Workflow w " +
        "JOIN w.request r " +
        "JOIN Prioritization p ON r.id = p.id " +
        "LEFT JOIN FETCH w.request r2 " +
        "LEFT JOIN FETCH r2.customer c " +
        "WHERE w.currentAssignee IS NULL " +
        "AND p.department.id = :deptId " +
        "AND (w.status = com.example.enums.WorkflowStatus.APPROVED_BY_PM " +
        "     OR w.status = com.example.enums.WorkflowStatus.SUBMITTED)")
    List<Workflow> findUnassignedWorkflowsByDepartmentId(@Param("deptId") Long deptId);

    @Modifying
    @Query("UPDATE Workflow w SET w.currentAssignee = :dev, w.status = :status WHERE w.request.id = :reqId")
    void updateWorkflowAssigneeAndStatus(@Param("reqId") Long requestId, 
                                         @Param("dev") User developer, 
                                         @Param("status") WorkflowStatus status);

    @Modifying
    @Query("UPDATE Workflow w SET w.status = :status WHERE w.request.id = :reqId")
    void sendBackToPm(@Param("reqId") Long requestId, @Param("status") WorkflowStatus status);
}